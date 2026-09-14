package com.example.movieapp.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.IdempotencyKeyGenerator
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.PaymentOrder
import com.example.movieapp.domain.usecase.GetCurrentSubscriptionUseCase
import com.example.movieapp.domain.usecase.GetPlansUseCase
import com.example.movieapp.domain.usecase.SubscribeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val getPlansUseCase: GetPlansUseCase,
    private val subscribeUseCase: SubscribeUseCase,
    private val getCurrentSubscriptionUseCase: GetCurrentSubscriptionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        loadPlans()
    }

    fun loadPlans() {
        pollingJob?.cancel()
        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            val plansResult = getPlansUseCase()
            val currentSubResult = getCurrentSubscriptionUseCase()

            if (plansResult is Result.Success) {
                val currentSub = (currentSubResult as? Result.Success)?.data
                _uiState.value = SubscriptionUiState.PlansLoaded(
                    plans = plansResult.data,
                    selectedPlanId = plansResult.data.firstOrNull()?.id,
                    selectedPaymentMethod = "card",
                    currentSubscription = currentSub
                )
            } else if (plansResult is Result.Error) {
                _uiState.value = SubscriptionUiState.Error(
                    message = plansResult.message,
                    code = plansResult.code,
                    requestId = plansResult.requestId
                )
            }
        }
    }

    fun selectPlan(planId: String) {
        val currentState = _uiState.value
        if (currentState is SubscriptionUiState.PlansLoaded) {
            _uiState.value = currentState.copy(selectedPlanId = planId)
        }
    }

    fun selectPaymentMethod(method: String) {
        val currentState = _uiState.value
        if (currentState is SubscriptionUiState.PlansLoaded) {
            _uiState.value = currentState.copy(selectedPaymentMethod = method)
        }
    }

    fun subscribe() {
        val currentState = _uiState.value as? SubscriptionUiState.PlansLoaded ?: return
        val planId = currentState.selectedPlanId ?: return
        val paymentMethod = currentState.selectedPaymentMethod

        viewModelScope.launch {
            _uiState.value = SubscriptionUiState.Loading
            val idempotencyKey = IdempotencyKeyGenerator.generate()
            when (val result = subscribeUseCase(planId, paymentMethod, idempotencyKey)) {
                is Result.Success -> {
                    val order = result.data
                    _uiState.value = SubscriptionUiState.AwaitingPayment(order, attemptCount = 0)
                    startPolling(order)
                }
                is Result.Error -> {
                    _uiState.value = SubscriptionUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }

    private fun startPolling(paymentOrder: PaymentOrder) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 24
            while (attempts < maxAttempts) {
                delay(5000)
                attempts++
                _uiState.value = SubscriptionUiState.AwaitingPayment(paymentOrder, attemptCount = attempts)
                when (val result = getCurrentSubscriptionUseCase()) {
                    is Result.Success -> {
                        val subscription = result.data
                        if (subscription != null && subscription.status == "active") {
                            _uiState.value = SubscriptionUiState.ActiveSuccess(subscription)
                            return@launch
                        }
                    }
                    is Result.Error -> {
                        // Ignore temporary polling errors
                    }
                }
            }
            _uiState.value = SubscriptionUiState.PaymentPendingTimeout(
                "Thanh toán đang được xử lý, vui lòng kiểm tra lại sau"
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}

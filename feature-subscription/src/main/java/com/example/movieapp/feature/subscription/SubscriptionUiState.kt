package com.example.movieapp.feature.subscription

import com.example.movieapp.domain.model.CurrentSubscription
import com.example.movieapp.domain.model.PaymentOrder
import com.example.movieapp.domain.model.Plan

sealed interface SubscriptionUiState {
    data object Loading : SubscriptionUiState
    data class PlansLoaded(
        val plans: List<Plan>,
        val selectedPlanId: String? = null,
        val selectedPaymentMethod: String = "card",
        val currentSubscription: CurrentSubscription? = null
    ) : SubscriptionUiState
    data class AwaitingPayment(
        val paymentOrder: PaymentOrder,
        val attemptCount: Int = 0
    ) : SubscriptionUiState
    data class ActiveSuccess(val currentSubscription: CurrentSubscription) : SubscriptionUiState
    data class PaymentPendingTimeout(val message: String) : SubscriptionUiState
    data class Error(val message: String, val code: String? = null, val requestId: String? = null) : SubscriptionUiState
}

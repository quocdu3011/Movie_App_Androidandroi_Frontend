package com.example.movieapp.feature.subscription

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.CurrentSubscription
import com.example.movieapp.domain.model.PaymentOrder
import com.example.movieapp.domain.model.Plan
import com.example.movieapp.domain.usecase.GetCurrentSubscriptionUseCase
import com.example.movieapp.domain.usecase.GetPlansUseCase
import com.example.movieapp.domain.usecase.SubscribeUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getPlansUseCase = mockk<GetPlansUseCase>()
    private val subscribeUseCase = mockk<SubscribeUseCase>()
    private val getCurrentSubscriptionUseCase = mockk<GetCurrentSubscriptionUseCase>()

    private lateinit var viewModel: SubscriptionViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadPlans fetches plans and current subscription`() = runTest {
        val plans = listOf(
            Plan(
                id = "plan1", name = "Basic Plan", price = 50000, currency = "VND",
                durationDays = 30, maxConcurrentStreams = 1, maxResolution = "1080p", version = "1"
            )
        )
        coEvery { getPlansUseCase() } returns Result.Success(plans)
        coEvery { getCurrentSubscriptionUseCase() } returns Result.Success(null)

        viewModel = SubscriptionViewModel(getPlansUseCase, subscribeUseCase, getCurrentSubscriptionUseCase)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SubscriptionUiState.PlansLoaded)
        val state = viewModel.uiState.value as SubscriptionUiState.PlansLoaded
        assertEquals("plan1", state.selectedPlanId)
    }

    @Test
    fun `subscribe starts polling and stops when active`() = runTest {
        val plans = listOf(
            Plan(
                id = "plan1", name = "Basic Plan", price = 50000, currency = "VND",
                durationDays = 30, maxConcurrentStreams = 1, maxResolution = "1080p", version = "1"
            )
        )
        val order = PaymentOrder(
            paymentId = "pay1", orderId = "ord1", subscriptionId = "sub1", status = "pending",
            paymentExpiresAt = "2026-12-31T23:59:59Z", amount = 50000, currency = "VND",
            paymentMethod = "card", provider = "mock", idempotentReplay = false, requestId = "req1"
        )
        val activeSub = CurrentSubscription(
            subscriptionId = "sub1", status = "active", startAt = "", endAt = "", plan = plans.first()
        )

        coEvery { getPlansUseCase() } returns Result.Success(plans)
        coEvery { getCurrentSubscriptionUseCase() } returnsMany listOf(
            Result.Success(null),
            Result.Success(null),
            Result.Success(activeSub)
        )
        coEvery { subscribeUseCase("plan1", "card", any()) } returns Result.Success(order)

        viewModel = SubscriptionViewModel(getPlansUseCase, subscribeUseCase, getCurrentSubscriptionUseCase)
        testDispatcher.scheduler.runCurrent()

        viewModel.subscribe()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SubscriptionUiState.AwaitingPayment)

        advanceTimeBy(5000)
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.uiState.value is SubscriptionUiState.AwaitingPayment)

        advanceTimeBy(5000)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SubscriptionUiState.ActiveSuccess)
        val state = viewModel.uiState.value as SubscriptionUiState.ActiveSuccess
        assertEquals("sub1", state.currentSubscription.subscriptionId)
    }

    @Test
    fun `polling times out after 24 attempts if server never returns active`() = runTest {
        val plans = listOf(
            Plan(
                id = "plan1", name = "Basic Plan", price = 50000, currency = "VND",
                durationDays = 30, maxConcurrentStreams = 1, maxResolution = "1080p", version = "1"
            )
        )
        val order = PaymentOrder(
            paymentId = "pay1", orderId = "ord1", subscriptionId = "sub1", status = "pending",
            paymentExpiresAt = "2026-12-31T23:59:59Z", amount = 50000, currency = "VND",
            paymentMethod = "card", provider = "mock", idempotentReplay = false, requestId = "req1"
        )

        coEvery { getPlansUseCase() } returns Result.Success(plans)
        coEvery { getCurrentSubscriptionUseCase() } returns Result.Success(null)
        coEvery { subscribeUseCase("plan1", "card", any()) } returns Result.Success(order)

        viewModel = SubscriptionViewModel(getPlansUseCase, subscribeUseCase, getCurrentSubscriptionUseCase)
        testDispatcher.scheduler.runCurrent()

        viewModel.subscribe()
        testDispatcher.scheduler.runCurrent()

        advanceTimeBy(120_000)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SubscriptionUiState.PaymentPendingTimeout)
    }
}

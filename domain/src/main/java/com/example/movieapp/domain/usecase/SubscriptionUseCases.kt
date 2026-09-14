package com.example.movieapp.domain.usecase

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.CurrentSubscription
import com.example.movieapp.domain.model.PaymentOrder
import com.example.movieapp.domain.model.Plan
import com.example.movieapp.domain.repository.SubscriptionRepository
import javax.inject.Inject

class GetPlansUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): Result<List<Plan>> {
        return subscriptionRepository.getPlans()
    }
}

class SubscribeUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(
        planId: String,
        paymentMethod: String,
        idempotencyKey: String
    ): Result<PaymentOrder> {
        return subscriptionRepository.subscribe(planId, paymentMethod, idempotencyKey)
    }
}

class GetCurrentSubscriptionUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    suspend operator fun invoke(): Result<CurrentSubscription?> {
        return subscriptionRepository.getCurrent()
    }
}

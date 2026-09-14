package com.example.movieapp.domain.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.CurrentSubscription
import com.example.movieapp.domain.model.PaymentOrder
import com.example.movieapp.domain.model.Plan

interface SubscriptionRepository {
    suspend fun getPlans(): Result<List<Plan>>
    suspend fun subscribe(
        planId: String,
        paymentMethod: String,
        idempotencyKey: String
    ): Result<PaymentOrder>
    suspend fun getCurrent(): Result<CurrentSubscription?>
}

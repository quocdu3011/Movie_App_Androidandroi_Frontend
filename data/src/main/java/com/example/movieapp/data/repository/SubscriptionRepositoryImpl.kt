package com.example.movieapp.data.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.data.mapper.toDomain
import com.example.movieapp.data.remote.api.SubscriptionApi
import com.example.movieapp.data.remote.dto.SubscribeRequestDto
import com.example.movieapp.domain.model.CurrentSubscription
import com.example.movieapp.domain.model.PaymentOrder
import com.example.movieapp.domain.model.Plan
import com.example.movieapp.domain.repository.SubscriptionRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionApi: SubscriptionApi,
    private val json: Json
) : SubscriptionRepository {

    override suspend fun getPlans(): Result<List<Plan>> {
        return safeApiCall(
            json = json,
            apiCall = { subscriptionApi.getPlans() },
            transform = { res -> res.items.map { it.toDomain() } }
        )
    }

    override suspend fun subscribe(
        planId: String,
        paymentMethod: String,
        idempotencyKey: String
    ): Result<PaymentOrder> {
        return safeApiCall(
            json = json,
            apiCall = {
                subscriptionApi.subscribe(
                    idempotencyKey = idempotencyKey,
                    request = SubscribeRequestDto(planId = planId, paymentMethod = paymentMethod)
                )
            },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun getCurrent(): Result<CurrentSubscription?> {
        return safeApiCall(
            json = json,
            apiCall = { subscriptionApi.getCurrentSubscription() },
            transform = { dto -> dto?.toDomain() }
        )
    }
}

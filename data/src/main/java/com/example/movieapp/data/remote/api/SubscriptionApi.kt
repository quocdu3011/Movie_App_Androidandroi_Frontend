package com.example.movieapp.data.remote.api

import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.data.remote.dto.CurrentSubscriptionDto
import com.example.movieapp.data.remote.dto.PaymentOrderDto
import com.example.movieapp.data.remote.dto.PlansResponseDto
import com.example.movieapp.data.remote.dto.SubscribeRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SubscriptionApi {

    @GET("subscriptions/plans")
    suspend fun getPlans(): ApiResponseDto<PlansResponseDto>

    @POST("subscriptions/subscribe")
    suspend fun subscribe(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: SubscribeRequestDto
    ): ApiResponseDto<PaymentOrderDto>

    @GET("subscriptions/current")
    suspend fun getCurrentSubscription(): ApiResponseDto<CurrentSubscriptionDto?>
}

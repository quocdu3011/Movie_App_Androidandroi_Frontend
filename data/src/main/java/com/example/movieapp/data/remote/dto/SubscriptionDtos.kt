package com.example.movieapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PlanDto(
    val id: String,
    val name: String,
    val price: Long,
    val currency: String,
    val durationDays: Int,
    val maxConcurrentStreams: Int,
    val maxResolution: String,
    val version: Int = 1
)

@Serializable
data class PlansResponseDto(
    val items: List<PlanDto> = emptyList()
)

@Serializable
data class SubscribeRequestDto(
    val planId: String,
    val paymentMethod: String
)

@Serializable
data class PlanSnapshotDto(
    val name: String,
    val price: Long,
    val currency: String,
    val durationDays: Int,
    val maxConcurrentStreams: Int,
    val maxResolution: String
)

@Serializable
data class PaymentProviderDto(
    val name: String,
    val orderId: String,
    val status: String
)

@Serializable
data class PaymentOrderDto(
    val paymentId: String,
    val orderId: String,
    val subscriptionId: String,
    val status: String,
    val paymentExpiresAt: String,
    val amount: Long,
    val currency: String,
    val paymentMethod: String,
    val planSnapshot: PlanSnapshotDto? = null,
    val provider: PaymentProviderDto? = null,
    val idempotentReplay: Boolean = false,
    val requestId: String = ""
)

@Serializable
data class PaymentSummaryDto(
    val paymentId: String,
    val status: String,
    val method: String,
    val expiresAt: String,
    val createdAt: String
)

@Serializable
data class CurrentSubscriptionDto(
    val subscriptionId: String,
    val status: String,
    val startAt: String? = null,
    val endAt: String? = null,
    val autoRenew: Boolean = false,
    val plan: PlanSnapshotDto? = null,
    val payment: PaymentSummaryDto? = null
)

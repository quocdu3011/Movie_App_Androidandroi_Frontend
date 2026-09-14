package com.example.movieapp.domain.model

data class Plan(
    val id: String,
    val name: String,
    val price: Long,
    val currency: String,
    val durationDays: Int,
    val maxConcurrentStreams: Int,
    val maxResolution: String,
    val version: String
)

data class PaymentOrder(
    val paymentId: String,
    val orderId: String,
    val subscriptionId: String,
    val status: String,
    val paymentExpiresAt: String,
    val amount: Long,
    val currency: String,
    val paymentMethod: String,
    val planSnapshot: Plan? = null,
    val provider: String,
    val idempotentReplay: Boolean,
    val requestId: String
)

data class CurrentSubscription(
    val subscriptionId: String,
    val status: String,
    val startAt: String,
    val endAt: String,
    val autoRenew: Boolean = false,
    val plan: Plan? = null,
    val payment: PaymentOrder? = null
)

data class AuthSession(
    val active: Boolean,
    val userId: String? = null,
    val sessionId: String? = null,
    val role: String? = null,
    val email: String? = null,
    val fullName: String? = null
)

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

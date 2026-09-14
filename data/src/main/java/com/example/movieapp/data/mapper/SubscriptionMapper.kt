package com.example.movieapp.data.mapper

import com.example.movieapp.data.remote.dto.CurrentSubscriptionDto
import com.example.movieapp.data.remote.dto.PaymentOrderDto
import com.example.movieapp.data.remote.dto.PlanDto
import com.example.movieapp.data.remote.dto.PlanSnapshotDto
import com.example.movieapp.domain.model.CurrentSubscription
import com.example.movieapp.domain.model.PaymentOrder
import com.example.movieapp.domain.model.Plan

fun PlanDto.toDomain(): Plan = Plan(
    id = id,
    name = name,
    price = price,
    currency = currency,
    durationDays = durationDays,
    maxConcurrentStreams = maxConcurrentStreams,
    maxResolution = maxResolution,
    version = version.toString()
)

fun PlanSnapshotDto.toDomain(): Plan = Plan(
    id = "",
    name = name,
    price = price,
    currency = currency,
    durationDays = durationDays,
    maxConcurrentStreams = maxConcurrentStreams,
    maxResolution = maxResolution,
    version = "1"
)

fun PaymentOrderDto.toDomain(): PaymentOrder = PaymentOrder(
    paymentId = paymentId,
    orderId = orderId,
    subscriptionId = subscriptionId,
    status = status,
    paymentExpiresAt = paymentExpiresAt,
    amount = amount,
    currency = currency,
    paymentMethod = paymentMethod,
    planSnapshot = planSnapshot?.toDomain(),
    provider = provider?.name ?: "",
    idempotentReplay = idempotentReplay,
    requestId = requestId
)

fun CurrentSubscriptionDto.toDomain(): CurrentSubscription = CurrentSubscription(
    subscriptionId = subscriptionId,
    status = status,
    startAt = startAt ?: "",
    endAt = endAt ?: "",
    autoRenew = autoRenew,
    plan = plan?.toDomain(),
    payment = payment?.let { p ->
        PaymentOrder(
            paymentId = p.paymentId,
            orderId = p.paymentId,
            subscriptionId = subscriptionId,
            status = p.status,
            paymentExpiresAt = p.expiresAt,
            amount = 0L,
            currency = "",
            paymentMethod = p.method,
            planSnapshot = null,
            provider = "",
            idempotentReplay = false,
            requestId = ""
        )
    }
)

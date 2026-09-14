package com.example.movieapp.core.common

import java.util.UUID

object IdempotencyKeyGenerator {
    fun generate(): String = UUID.randomUUID().toString()
}

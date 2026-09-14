package com.example.movieapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val email: String,
    val password: String,
    val fullName: String
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
    val deviceId: String,
    val deviceName: String? = null
)

@Serializable
data class RefreshRequestDto(
    val refreshToken: String
)

@Serializable
data class LogoutRequestDto(
    val refreshToken: String
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val role: String
)

@Serializable
data class AuthSessionDto(
    val active: Boolean,
    val userId: String? = null,
    val sessionId: String? = null,
    val role: String? = null,
    val email: String? = null,
    val fullName: String? = null
)

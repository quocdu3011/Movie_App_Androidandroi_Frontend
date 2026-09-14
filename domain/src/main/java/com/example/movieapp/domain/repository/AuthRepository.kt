package com.example.movieapp.domain.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.AuthSession
import com.example.movieapp.domain.model.TokenPair
import com.example.movieapp.domain.model.User

interface AuthRepository {
    suspend fun register(email: String, password: String, fullName: String): Result<User>
    suspend fun login(email: String, password: String, deviceName: String? = null): Result<TokenPair>
    suspend fun refresh(refreshToken: String): Result<TokenPair>
    suspend fun logout(refreshToken: String): Result<Unit>
    suspend fun getSession(): Result<AuthSession>
}

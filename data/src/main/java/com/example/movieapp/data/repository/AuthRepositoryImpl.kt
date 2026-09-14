package com.example.movieapp.data.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.core.network.DeviceIdProvider
import com.example.movieapp.core.network.TokenStorage
import com.example.movieapp.data.mapper.toDomain
import com.example.movieapp.data.remote.api.AuthApi
import com.example.movieapp.data.remote.dto.LoginRequestDto
import com.example.movieapp.data.remote.dto.LogoutRequestDto
import com.example.movieapp.data.remote.dto.RefreshRequestDto
import com.example.movieapp.data.remote.dto.RegisterRequestDto
import com.example.movieapp.domain.model.AuthSession
import com.example.movieapp.domain.model.TokenPair
import com.example.movieapp.domain.model.User
import com.example.movieapp.domain.repository.AuthRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val deviceIdProvider: DeviceIdProvider,
    private val tokenStorage: TokenStorage,
    private val json: Json
) : AuthRepository {

    override suspend fun register(email: String, password: String, fullName: String): Result<User> {
        return safeApiCall(
            json = json,
            apiCall = { authApi.register(RegisterRequestDto(email, password, fullName)) },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun login(email: String, password: String, deviceName: String?): Result<TokenPair> {
        val deviceId = deviceIdProvider.getOrCreate()
        val result = safeApiCall(
            json = json,
            apiCall = { authApi.login(LoginRequestDto(email, password, deviceId, deviceName)) },
            transform = { dto -> dto.toDomain() }
        )
        if (result is Result.Success) {
            tokenStorage.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    override suspend fun refresh(refreshToken: String): Result<TokenPair> {
        val result = safeApiCall(
            json = json,
            apiCall = { authApi.refresh(RefreshRequestDto(refreshToken)) },
            transform = { dto -> dto.toDomain() }
        )
        if (result is Result.Success) {
            tokenStorage.saveTokens(result.data.accessToken, result.data.refreshToken)
        }
        return result
    }

    override suspend fun logout(refreshToken: String): Result<Unit> {
        val result = safeVoidApiCall(
            json = json,
            apiCall = { authApi.logout(LogoutRequestDto(refreshToken)) }
        )
        tokenStorage.clear()
        return result
    }

    override suspend fun getSession(): Result<AuthSession> {
        return safeApiCall(
            json = json,
            apiCall = { authApi.getSession() },
            transform = { dto -> dto.toDomain() }
        )
    }
}

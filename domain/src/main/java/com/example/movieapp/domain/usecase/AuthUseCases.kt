package com.example.movieapp.domain.usecase

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.AuthSession
import com.example.movieapp.domain.model.TokenPair
import com.example.movieapp.domain.model.User
import com.example.movieapp.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, fullName: String): Result<User> {
        return authRepository.register(email, password, fullName)
    }
}

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String, deviceName: String? = null): Result<TokenPair> {
        return authRepository.login(email, password, deviceName)
    }
}

class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthSession> {
        return authRepository.getSession()
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(refreshToken: String): Result<Unit> {
        return authRepository.logout(refreshToken)
    }
}

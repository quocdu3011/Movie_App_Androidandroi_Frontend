package com.example.movieapp.feature.auth

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class RegisterSuccess(val userEmail: String) : AuthUiState
    object LoginSuccess : AuthUiState
    data class Error(
        val message: String,
        val code: String? = null,
        val requestId: String? = null
    ) : AuthUiState
}

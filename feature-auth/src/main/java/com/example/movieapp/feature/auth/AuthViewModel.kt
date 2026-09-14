package com.example.movieapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.usecase.LoginUseCase
import com.example.movieapp.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, deviceName: String? = null) {
        if (password.length !in 12..128) {
            _uiState.value = AuthUiState.Error(
                message = "Mật khẩu phải từ 12 đến 128 ký tự.",
                code = "INVALID_PASSWORD"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = loginUseCase(email, password, deviceName)) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.LoginSuccess
                }
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }

    fun register(email: String, password: String, fullName: String) {
        if (fullName.isBlank()) {
            _uiState.value = AuthUiState.Error(
                message = "Họ và tên không được để trống.",
                code = "INVALID_FULL_NAME"
            )
            return
        }
        if (password.length !in 12..128) {
            _uiState.value = AuthUiState.Error(
                message = "Mật khẩu phải từ 12 đến 128 ký tự.",
                code = "INVALID_PASSWORD"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = registerUseCase(email, password, fullName)) {
                is Result.Success -> {
                    _uiState.value = AuthUiState.RegisterSuccess(email)
                }
                is Result.Error -> {
                    _uiState.value = AuthUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

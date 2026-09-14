package com.example.movieapp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    fun checkSession() {
        viewModelScope.launch {
            _uiState.value = SplashUiState.Loading
            when (val result = restoreSessionUseCase()) {
                is Result.Success -> {
                    if (result.data.active) {
                        _uiState.value = SplashUiState.Authenticated
                    } else {
                        _uiState.value = SplashUiState.Unauthenticated
                    }
                }
                is Result.Error -> {
                    _uiState.value = SplashUiState.Unauthenticated
                }
            }
        }
    }
}

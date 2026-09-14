package com.example.movieapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.AuthSession
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.LogoutUseCase
import com.example.movieapp.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Success(val session: AuthSession) : SettingsUiState
    data class Error(val message: String, val code: String? = null, val requestId: String? = null) : SettingsUiState
    data object LoggedOut : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val currentProfileStore: CurrentProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    fun loadSession() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            when (val result = restoreSessionUseCase()) {
                is Result.Success -> {
                    _uiState.value = SettingsUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = SettingsUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Loading
            logoutUseCase("")
            currentProfileStore.setProfileId(null)
            _uiState.value = SettingsUiState.LoggedOut
        }
    }
}

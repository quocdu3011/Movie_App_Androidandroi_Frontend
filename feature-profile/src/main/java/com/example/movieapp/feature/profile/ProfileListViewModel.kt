package com.example.movieapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.Profile
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.DeleteProfileUseCase
import com.example.movieapp.domain.usecase.GetProfilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileListUiState {
    data object Loading : ProfileListUiState
    data class Success(val profiles: List<Profile>, val activeProfileId: String?) : ProfileListUiState
    data class Error(val message: String, val code: String? = null, val requestId: String? = null) : ProfileListUiState
}

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val currentProfileStore: CurrentProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileListUiState>(ProfileListUiState.Loading)
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ProfileListUiState.Loading
            when (val result = getProfilesUseCase()) {
                is Result.Success -> {
                    val activeId = currentProfileStore.currentProfileId.value
                    _uiState.value = ProfileListUiState.Success(
                        profiles = result.data,
                        activeProfileId = activeId
                    )
                }
                is Result.Error -> {
                    _uiState.value = ProfileListUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }

    fun selectProfile(profileId: String) {
        currentProfileStore.setProfileId(profileId)
        val currentState = _uiState.value
        if (currentState is ProfileListUiState.Success) {
            _uiState.value = currentState.copy(activeProfileId = profileId)
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            when (val result = deleteProfileUseCase(profileId)) {
                is Result.Success -> {
                    if (currentProfileStore.currentProfileId.value == profileId) {
                        currentProfileStore.setProfileId(null)
                    }
                    loadProfiles()
                }
                is Result.Error -> {
                    _uiState.value = ProfileListUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }
}

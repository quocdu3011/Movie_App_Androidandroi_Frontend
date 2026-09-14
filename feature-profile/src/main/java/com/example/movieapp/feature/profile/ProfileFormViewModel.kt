package com.example.movieapp.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.Profile
import com.example.movieapp.domain.usecase.CreateProfileUseCase
import com.example.movieapp.domain.usecase.GetProfilesUseCase
import com.example.movieapp.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileFormUiState {
    data object Idle : ProfileFormUiState
    data object Loading : ProfileFormUiState
    data class Loaded(
        val isEditMode: Boolean,
        val profileId: String? = null,
        val initialName: String = "",
        val initialAvatarId: Int? = null,
        val initialIsKids: Boolean = false
    ) : ProfileFormUiState
    data object SaveSuccess : ProfileFormUiState
    data class Error(val message: String, val code: String? = null, val requestId: String? = null) : ProfileFormUiState
}

@HiltViewModel
class ProfileFormViewModel @Inject constructor(
    private val createProfileUseCase: CreateProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val getProfilesUseCase: GetProfilesUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileIdArg: String? = savedStateHandle["profileId"]

    private val _uiState = MutableStateFlow<ProfileFormUiState>(ProfileFormUiState.Idle)
    val uiState: StateFlow<ProfileFormUiState> = _uiState.asStateFlow()

    init {
        loadProfileForForm(profileIdArg)
    }

    fun loadProfileForForm(id: String?) {
        if (id == null) {
            _uiState.value = ProfileFormUiState.Loaded(isEditMode = false)
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileFormUiState.Loading
            when (val result = getProfilesUseCase()) {
                is Result.Success -> {
                    val target = result.data.find { it.id == id }
                    if (target != null) {
                        _uiState.value = ProfileFormUiState.Loaded(
                            isEditMode = true,
                            profileId = target.id,
                            initialName = target.name,
                            initialAvatarId = target.avatarId,
                            initialIsKids = target.isKids
                        )
                    } else {
                        _uiState.value = ProfileFormUiState.Error("Không tìm thấy hồ sơ")
                    }
                }
                is Result.Error -> {
                    _uiState.value = ProfileFormUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }

    fun saveProfile(name: String, avatarId: Int?, isKids: Boolean) {
        if (name.isBlank()) {
            _uiState.value = ProfileFormUiState.Error("Tên hồ sơ không được để trống")
            return
        }

        val currentState = _uiState.value
        val isEdit = (currentState as? ProfileFormUiState.Loaded)?.isEditMode == true

        viewModelScope.launch {
            _uiState.value = ProfileFormUiState.Loading
            val result: Result<Profile> = if (isEdit && profileIdArg != null) {
                updateProfileUseCase(id = profileIdArg, name = name, avatarId = avatarId, isKids = isKids)
            } else {
                createProfileUseCase(name = name, avatarId = avatarId, isKids = isKids)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.value = ProfileFormUiState.SaveSuccess
                }
                is Result.Error -> {
                    _uiState.value = ProfileFormUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }
}

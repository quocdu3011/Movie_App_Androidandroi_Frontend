package com.example.movieapp.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.GetPersonalizedHomeUseCase
import com.example.movieapp.domain.usecase.GetPublicHomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val currentProfileStore: CurrentProfileStore,
    private val getPublicHomeUseCase: GetPublicHomeUseCase,
    private val getPersonalizedHomeUseCase: GetPersonalizedHomeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        observeCurrentProfile()
    }

    private fun observeCurrentProfile() {
        viewModelScope.launch {
            currentProfileStore.currentProfileId.collectLatest { profileId ->
                loadHomeData(profileId)
            }
        }
    }

    fun refresh() {
        loadHomeData(currentProfileStore.currentProfileId.value)
    }

    private fun loadHomeData(profileId: String?) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            if (profileId == null) {
                when (val result = getPublicHomeUseCase()) {
                    is Result.Success -> {
                        _uiState.value = HomeUiState.Public(result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = HomeUiState.Error(result.message, result.requestId)
                    }
                }
            } else {
                when (val result = getPersonalizedHomeUseCase(profileId)) {
                    is Result.Success -> {
                        _uiState.value = HomeUiState.Personalized(result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = HomeUiState.Error(result.message, result.requestId)
                    }
                }
            }
        }
    }
}

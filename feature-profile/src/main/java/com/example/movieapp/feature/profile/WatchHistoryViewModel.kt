package com.example.movieapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.GetWatchHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WatchHistoryUiState {
    data object Loading : WatchHistoryUiState
    data class Success(val items: List<HistoryItem>) : WatchHistoryUiState
    data class Error(val message: String, val code: String? = null, val requestId: String? = null) : WatchHistoryUiState
    data object NoProfileSelected : WatchHistoryUiState
}

@HiltViewModel
class WatchHistoryViewModel @Inject constructor(
    private val getWatchHistoryUseCase: GetWatchHistoryUseCase,
    private val currentProfileStore: CurrentProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<WatchHistoryUiState>(WatchHistoryUiState.Loading)
    val uiState: StateFlow<WatchHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val profileId = currentProfileStore.currentProfileId.value
        if (profileId == null) {
            _uiState.value = WatchHistoryUiState.NoProfileSelected
            return
        }

        viewModelScope.launch {
            _uiState.value = WatchHistoryUiState.Loading
            when (val result = getWatchHistoryUseCase(profileId)) {
                is Result.Success -> {
                    _uiState.value = WatchHistoryUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = WatchHistoryUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }
}

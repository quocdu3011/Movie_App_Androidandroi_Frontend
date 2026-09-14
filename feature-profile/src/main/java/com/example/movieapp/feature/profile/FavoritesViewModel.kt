package com.example.movieapp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.GetFavoritesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState
    data class Success(val movies: List<Movie>) : FavoritesUiState
    data class Error(val message: String, val code: String? = null, val requestId: String? = null) : FavoritesUiState
    data object NoProfileSelected : FavoritesUiState
}

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val currentProfileStore: CurrentProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        val profileId = currentProfileStore.currentProfileId.value
        if (profileId == null) {
            _uiState.value = FavoritesUiState.NoProfileSelected
            return
        }

        viewModelScope.launch {
            _uiState.value = FavoritesUiState.Loading
            when (val result = getFavoritesUseCase(profileId)) {
                is Result.Success -> {
                    _uiState.value = FavoritesUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = FavoritesUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }
}

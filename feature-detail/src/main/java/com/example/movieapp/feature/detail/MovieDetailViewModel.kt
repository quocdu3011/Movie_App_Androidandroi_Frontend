package com.example.movieapp.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PlayableItem
import com.example.movieapp.domain.model.SourceItem
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.GetFavoritesUseCase
import com.example.movieapp.domain.usecase.GetMovieDetailUseCase
import com.example.movieapp.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val currentProfileStore: CurrentProfileStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovieDetailUiState())
    val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

    private var currentMovieId: String? = null

    fun loadMovieDetail(movieId: String) {
        currentMovieId = movieId
        val profileId = currentProfileStore.currentProfileId.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Check favorite if profileId is present
            if (profileId != null) {
                when (val favResult = getFavoritesUseCase(profileId)) {
                    is Result.Success -> {
                        val isFav = favResult.data.any { it.id == movieId }
                        _uiState.update { it.copy(isFavorite = isFav) }
                    }
                    else -> {}
                }
            }

            when (val result = getMovieDetailUseCase(movieId, profileId)) {
                is Result.Success -> {
                    val detail = result.data
                    val isSeries = detail.movie.type == "series"
                    val seasons = detail.playableItems
                        .mapNotNull { it.seasonNumber }
                        .distinct()
                        .sorted()

                    val defaultSeason = if (isSeries) seasons.firstOrNull() else null
                    val defaultPlayable = if (isSeries) {
                        detail.playableItems
                            .filter { it.seasonNumber == defaultSeason }
                            .minByOrNull { it.sortOrder }
                    } else {
                        detail.playableItems.firstOrNull()
                    }

                    val available = computeAvailableSources(detail, defaultPlayable?.id)
                    val defaultSource = available.firstOrNull()

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            movieDetail = detail,
                            selectedSeasonNumber = defaultSeason,
                            selectedPlayableItem = defaultPlayable,
                            availableSources = available,
                            selectedSourceItem = defaultSource
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            requestId = result.requestId
                        )
                    }
                }
            }
        }
    }

    fun selectSeason(seasonNumber: Int) {
        val detail = _uiState.value.movieDetail ?: return
        val seasonPlayables = detail.playableItems
            .filter { it.seasonNumber == seasonNumber }
            .sortedBy { it.sortOrder }

        val firstPlayable = seasonPlayables.firstOrNull()
        val available = computeAvailableSources(detail, firstPlayable?.id)

        _uiState.update {
            it.copy(
                selectedSeasonNumber = seasonNumber,
                selectedPlayableItem = firstPlayable,
                availableSources = available,
                selectedSourceItem = available.firstOrNull()
            )
        }
    }

    fun selectPlayableItem(playableItem: PlayableItem) {
        val detail = _uiState.value.movieDetail ?: return
        val available = computeAvailableSources(detail, playableItem.id)

        _uiState.update {
            it.copy(
                selectedPlayableItem = playableItem,
                availableSources = available,
                selectedSourceItem = available.firstOrNull()
            )
        }
    }

    fun selectSourceItem(sourceItem: SourceItem) {
        _uiState.update { it.copy(selectedSourceItem = sourceItem) }
    }

    fun toggleFavorite() {
        val movieId = currentMovieId ?: return
        val profileId = currentProfileStore.currentProfileId.value ?: return
        val currentIsFav = _uiState.value.isFavorite

        viewModelScope.launch {
            when (toggleFavoriteUseCase(profileId, movieId, currentIsFav)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isFavorite = !currentIsFav) }
                }
                else -> {}
            }
        }
    }

    private fun computeAvailableSources(detail: MovieDetail, playableId: String?): List<SourceItem> {
        if (playableId == null) return emptyList()
        return detail.sources.filter { source ->
            source.playableId == playableId &&
                    source.sourceStatus in listOf("available", "unknown") &&
                    !source.sourceItemId.isNullOrBlank()
        }
    }
}

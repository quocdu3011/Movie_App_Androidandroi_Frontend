package com.example.movieapp.feature.detail

import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PlayableItem
import com.example.movieapp.domain.model.SourceItem

data class MovieDetailUiState(
    val isLoading: Boolean = true,
    val movieDetail: MovieDetail? = null,
    val selectedSeasonNumber: Int? = null,
    val selectedPlayableItem: PlayableItem? = null,
    val availableSources: List<SourceItem> = emptyList(),
    val selectedSourceItem: SourceItem? = null,
    val isFavorite: Boolean = false,
    val profileId: String? = null,
    val lastHistoryItem: HistoryItem? = null,
    val continuePlayableItem: PlayableItem? = null,
    val continueSourceItem: SourceItem? = null,
    val errorMessage: String? = null,
    val requestId: String? = null
)

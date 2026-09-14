package com.example.movieapp.feature.search

import com.example.movieapp.domain.model.Movie

data class SearchFilter(
    val genre: String? = null,
    val country: String? = null,
    val year: Int? = null,
    val type: String? = null,
    val contentKind: String? = null,
    val sourceType: String? = null,
    val provider: String? = null,
    val sort: String? = null
) {
    val isActive: Boolean
        get() = genre != null || country != null || year != null || type != null ||
                contentKind != null || sourceType != null || provider != null || sort != null
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class History(val queries: List<String>) : SearchUiState
    data class Success(
        val movies: List<Movie>,
        val page: Int,
        val canLoadMore: Boolean,
        val isLoadingMore: Boolean = false,
        val isBrowseMode: Boolean = false
    ) : SearchUiState
    data class Error(val message: String, val code: String? = null, val requestId: String? = null) : SearchUiState
}

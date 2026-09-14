package com.example.movieapp.feature.home

import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.PersonalizedHome

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Public(val catalogHome: CatalogHome) : HomeUiState
    data class Personalized(val personalizedHome: PersonalizedHome) : HomeUiState
    data class Error(val message: String, val requestId: String? = null) : HomeUiState
}

package com.example.movieapp.domain.model

data class CatalogHome(
    val newReleases: List<Movie> = emptyList(),
    val topRated: List<Movie> = emptyList(),
    val trending: List<Movie> = emptyList()
)

sealed class HomeSection {
    data class ContinueWatching(val items: List<HistoryItem>) : HomeSection()
    data class CatalogNewReleases(val items: List<Movie>) : HomeSection()
    data class FallbackNewReleases(val reason: String, val items: List<Movie>) : HomeSection()
    data class Other(val type: String, val items: List<Movie>) : HomeSection()
}

data class PersonalizedHome(
    val profileId: String,
    val sections: List<HomeSection> = emptyList()
)

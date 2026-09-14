package com.example.movieapp.domain.model

data class GenreRef(
    val id: String,
    val name: String
)

data class CountryRef(
    val isoCode: String,
    val name: String
)

data class Movie(
    val id: String,
    val title: String,
    val originTitle: String? = null,
    val description: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseYear: Int? = null,
    val type: String,
    val contentKind: String,
    val status: String,
    val accessTier: String,
    val isKidsSafe: Boolean,
    val averageRating: Double,
    val publishedAt: String? = null,
    val version: String,
    val createdAt: String,
    val updatedAt: String
)

data class PlayableItem(
    val id: String,
    val kind: String,
    val seasonId: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val label: String,
    val sortOrder: Int,
    val durationSeconds: Int? = null,
    val archivedAt: String? = null
)

data class SourceItem(
    val id: String,
    val sourceType: String,
    val provider: String? = null,
    val sourceStatus: String,
    val sourceItemId: String? = null,
    val playableId: String? = null,
    val serverKey: String? = null,
    val serverLabel: String? = null,
    val playbackMode: String? = null
)

data class MovieDetail(
    val movie: Movie,
    val genres: List<GenreRef> = emptyList(),
    val countries: List<CountryRef> = emptyList(),
    val playableItems: List<PlayableItem> = emptyList(),
    val sources: List<SourceItem> = emptyList()
)

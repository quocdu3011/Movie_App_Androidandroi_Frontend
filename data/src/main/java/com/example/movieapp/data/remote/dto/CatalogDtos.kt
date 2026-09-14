package com.example.movieapp.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class MovieDto(
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
    val averageRating: Double = 0.0,
    val publishedAt: String? = null,
    val version: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class GenreRefDto(
    val slug: String? = null,
    val name: String,
    val id: String? = null
)

@Serializable
data class CountryRefDto(
    val slug: String? = null,
    val isoCode: String? = null,
    val name: String
)

@Serializable
data class PlayableItemDto(
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

@Serializable
data class SourceItemDto(
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

@Serializable
data class MovieDetailDto(
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
    val averageRating: Double = 0.0,
    val publishedAt: String? = null,
    val version: String,
    val createdAt: String,
    val updatedAt: String,
    val genres: List<GenreRefDto> = emptyList(),
    val countries: List<CountryRefDto> = emptyList(),
    val playableItems: List<PlayableItemDto> = emptyList(),
    val sources: List<SourceItemDto> = emptyList()
)

@Serializable
data class CatalogHomeSectionDto(
    val type: String,
    val items: List<MovieDto> = emptyList()
)

@Serializable
data class CatalogHomeDto(
    val newReleases: CatalogHomeSectionDto? = null,
    val topRated: CatalogHomeSectionDto? = null,
    val trending: CatalogHomeSectionDto? = null
)

@Serializable
data class HomeSectionDto(
    val type: String,
    val reason: String? = null,
    val items: List<JsonElement> = emptyList()
)

@Serializable
data class PersonalizedHomeDto(
    val profileId: String,
    val sections: List<HomeSectionDto> = emptyList()
)

package com.example.movieapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val avatarId: Int? = null,
    val isKids: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateProfileDto(
    val name: String,
    val avatarId: Int? = null,
    val isKids: Boolean? = null
)

@Serializable
data class UpdateProfileDto(
    val name: String? = null,
    val avatarId: Int? = null,
    val isKids: Boolean? = null
)

@Serializable
data class FavoritesResponseDto(
    val items: List<MovieDto> = emptyList()
)

@Serializable
data class PutFavoriteResponseDto(
    val profileId: String,
    val movieId: String,
    val created: Boolean
)

@Serializable
data class WatchHistoryResponseDto(
    val items: List<HistoryItemDto> = emptyList()
)

@Serializable
data class HistoryItemDto(
    val movieId: String,
    val playableId: String,
    val sourceItemId: String,
    val positionSeconds: Int,
    val durationSeconds: Int? = null,
    val updatedAt: String,
    val movie: MovieDto? = null,
    val tombstone: Boolean = false
)

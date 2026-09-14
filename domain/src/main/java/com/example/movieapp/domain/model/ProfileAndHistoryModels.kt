package com.example.movieapp.domain.model

data class Profile(
    val id: String,
    val name: String,
    val avatarId: Int? = null,
    val isKids: Boolean,
    val createdAt: String,
    val updatedAt: String
)

data class HistoryItem(
    val movieId: String,
    val playableId: String,
    val sourceItemId: String,
    val positionSeconds: Int,
    val durationSeconds: Int,
    val updatedAt: String,
    val movie: Movie? = null,
    val tombstone: Boolean = false
)

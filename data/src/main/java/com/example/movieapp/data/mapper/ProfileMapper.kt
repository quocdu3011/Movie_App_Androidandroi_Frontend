package com.example.movieapp.data.mapper

import com.example.movieapp.data.remote.dto.HistoryItemDto
import com.example.movieapp.data.remote.dto.ProfileDto
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.Profile

fun ProfileDto.toDomain(): Profile = Profile(
    id = id,
    name = name,
    avatarId = avatarId,
    isKids = isKids,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun HistoryItemDto.toDomain(): HistoryItem = HistoryItem(
    movieId = movieId,
    playableId = playableId,
    sourceItemId = sourceItemId,
    positionSeconds = positionSeconds,
    durationSeconds = durationSeconds ?: 0,
    updatedAt = updatedAt,
    movie = movie?.toDomain(),
    tombstone = tombstone
)

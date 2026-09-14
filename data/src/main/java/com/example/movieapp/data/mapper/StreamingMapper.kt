package com.example.movieapp.data.mapper

import com.example.movieapp.data.remote.dto.MediaAuthDto
import com.example.movieapp.data.remote.dto.PlaybackEventAckDto
import com.example.movieapp.data.remote.dto.PlaybackHeartbeatDto
import com.example.movieapp.data.remote.dto.PlaybackProgressAckDto
import com.example.movieapp.data.remote.dto.PlaybackSessionDto
import com.example.movieapp.data.remote.dto.WatchProgressDto
import com.example.movieapp.domain.model.MediaAuth
import com.example.movieapp.domain.model.PlaybackEventAck
import com.example.movieapp.domain.model.PlaybackHeartbeat
import com.example.movieapp.domain.model.PlaybackProgressAck
import com.example.movieapp.domain.model.PlaybackSession
import com.example.movieapp.domain.model.WatchProgress

fun MediaAuthDto.toDomain(): MediaAuth = MediaAuth(
    expiresAt = expiresAt
)

fun PlaybackSessionDto.toDomain(): PlaybackSession = PlaybackSession(
    sessionId = sessionId,
    playableId = playableId,
    sourceItemId = sourceItemId,
    sourceType = sourceType,
    protocol = protocol,
    playbackUrl = playbackUrl,
    mediaAuth = mediaAuth?.toDomain(),
    urlExpiresAt = urlExpiresAt ?: "",
    leaseExpiresAt = leaseExpiresAt,
    resumePositionSeconds = resumePositionSeconds,
    resumeNeedsConfirmation = resumeNeedsConfirmation,
    subtitles = subtitles,
    offlineSupported = offlineSupported,
    idempotentReplay = idempotentReplay
)

fun PlaybackHeartbeatDto.toDomain(): PlaybackHeartbeat = PlaybackHeartbeat(
    sessionId = sessionId,
    leaseExpiresAt = leaseExpiresAt
)

fun WatchProgressDto.toDomain(): WatchProgress = WatchProgress(
    profileId = profileId,
    playableId = playableId,
    movieId = movieId,
    sourceItemId = sourceItemId,
    sessionOrdinal = sessionOrdinal.toIntOrNull() ?: 0,
    seq = seq.toIntOrNull() ?: 0,
    positionSeconds = positionSeconds,
    durationSeconds = durationSeconds ?: 0,
    updatedAt = updatedAt
)

fun PlaybackProgressAckDto.toDomain(): PlaybackProgressAck = PlaybackProgressAck(
    sessionId = sessionId,
    accepted = accepted,
    applied = applied,
    progress = progress?.toDomain()
)

fun PlaybackEventAckDto.toDomain(): PlaybackEventAck = PlaybackEventAck(
    sessionId = sessionId,
    eventId = eventId,
    duplicate = duplicate,
    state = state,
    qualified = qualified
)

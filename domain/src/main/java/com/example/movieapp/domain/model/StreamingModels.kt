package com.example.movieapp.domain.model

data class MediaAuth(
    val expiresAt: String
)

data class PlaybackSession(
    val sessionId: String,
    val playableId: String,
    val sourceItemId: String,
    val sourceType: String,
    val protocol: String,
    val playbackUrl: String,
    val mediaAuth: MediaAuth? = null,
    val urlExpiresAt: String,
    val leaseExpiresAt: String,
    val resumePositionSeconds: Int,
    val resumeNeedsConfirmation: Boolean,
    val subtitles: List<String> = emptyList(),
    val offlineSupported: Boolean = false,
    val idempotentReplay: Boolean = false
)

data class PlaybackHeartbeat(
    val sessionId: String,
    val leaseExpiresAt: String
)

data class WatchProgress(
    val profileId: String,
    val playableId: String,
    val movieId: String,
    val sourceItemId: String,
    val sessionOrdinal: Int,
    val seq: Int,
    val positionSeconds: Int,
    val durationSeconds: Int,
    val updatedAt: String
)

data class PlaybackProgressAck(
    val sessionId: String,
    val accepted: Boolean,
    val applied: Boolean,
    val progress: WatchProgress? = null
)

data class PlaybackEventAck(
    val sessionId: String,
    val eventId: String,
    val duplicate: Boolean,
    val state: String,
    val qualified: Boolean
)

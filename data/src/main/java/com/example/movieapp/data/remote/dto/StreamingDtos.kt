package com.example.movieapp.data.remote.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreatePlaybackSessionRequestDto(
    val movieId: String,
    val playableId: String,
    val sourceItemId: String,
    val profileId: String
)

@Serializable
data class PlaybackSessionDto(
    val sessionId: String,
    val playableId: String,
    val sourceItemId: String,
    val sourceType: String,
    val protocol: String,
    val playbackUrl: String,
    val mediaAuth: MediaAuthDto? = null,
    val drm: JsonElement? = null,
    val urlExpiresAt: String? = null,
    val leaseExpiresAt: String,
    val resumePositionSeconds: Int = 0,
    val resumeNeedsConfirmation: Boolean = false,
    val subtitles: List<String> = emptyList(),
    val offlineSupported: Boolean = false,
    val idempotentReplay: Boolean = false
)

@Serializable
data class MediaAuthDto(
    val cookieName: String = "movie_media_auth",
    val cookieValue: String = "",
    val path: String = "",
    val expiresAt: String
)

@Serializable
data class PlaybackHeartbeatDto(
    val sessionId: String,
    val leaseExpiresAt: String
)

@Serializable
data class WatchProgressRequestDto(
    val seq: String,
    val positionSeconds: Int,
    val durationSeconds: Int? = null
)

@Serializable
data class WatchProgressDto(
    val profileId: String,
    val playableId: String,
    val movieId: String,
    val sourceItemId: String,
    val sessionOrdinal: String = "",
    val seq: String = "",
    val positionSeconds: Int = 0,
    val durationSeconds: Int? = null,
    val updatedAt: String = ""
)

@Serializable
data class PlaybackProgressAckDto(
    val sessionId: String,
    val accepted: Boolean,
    val applied: Boolean,
    val progress: WatchProgressDto? = null
)

@Serializable
data class PlaybackEventRequestDto(
    val eventId: String,
    val type: String,
    val playedSeconds: Int? = null,
    val reasonCode: String? = null
)

@Serializable
data class PlaybackEventAckDto(
    val sessionId: String,
    val eventId: String,
    val duplicate: Boolean = false,
    val state: String,
    val qualified: Boolean = false
)

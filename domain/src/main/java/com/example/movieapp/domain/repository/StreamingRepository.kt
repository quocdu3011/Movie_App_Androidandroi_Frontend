package com.example.movieapp.domain.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.MediaAuth
import com.example.movieapp.domain.model.PlaybackEventAck
import com.example.movieapp.domain.model.PlaybackHeartbeat
import com.example.movieapp.domain.model.PlaybackProgressAck
import com.example.movieapp.domain.model.PlaybackSession

interface StreamingRepository {
    suspend fun createPlaybackSession(
        profileId: String,
        movieId: String,
        playableId: String,
        sourceItemId: String,
        idempotencyKey: String
    ): Result<PlaybackSession>

    suspend fun sendHeartbeat(sessionId: String): Result<PlaybackHeartbeat>

    suspend fun sendProgress(
        sessionId: String,
        positionSeconds: Int,
        durationSeconds: Int
    ): Result<PlaybackProgressAck>

    suspend fun sendEvent(
        sessionId: String,
        eventType: String,
        eventData: String? = null
    ): Result<PlaybackEventAck>

    suspend fun renewMediaAuth(sessionId: String): Result<MediaAuth>
}

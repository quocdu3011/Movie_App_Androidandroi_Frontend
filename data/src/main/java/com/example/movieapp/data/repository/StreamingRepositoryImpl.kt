package com.example.movieapp.data.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.data.mapper.toDomain
import com.example.movieapp.data.remote.api.StreamingApi
import com.example.movieapp.data.remote.dto.CreatePlaybackSessionRequestDto
import com.example.movieapp.data.remote.dto.PlaybackEventRequestDto
import com.example.movieapp.data.remote.dto.WatchProgressRequestDto
import com.example.movieapp.domain.model.MediaAuth
import com.example.movieapp.domain.model.PlaybackEventAck
import com.example.movieapp.domain.model.PlaybackHeartbeat
import com.example.movieapp.domain.model.PlaybackProgressAck
import com.example.movieapp.domain.model.PlaybackSession
import com.example.movieapp.domain.repository.StreamingRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class StreamingRepositoryImpl @Inject constructor(
    private val streamingApi: StreamingApi,
    private val json: Json
) : StreamingRepository {

    override suspend fun createPlaybackSession(
        profileId: String,
        movieId: String,
        playableId: String,
        sourceItemId: String,
        idempotencyKey: String
    ): Result<PlaybackSession> {
        return safeApiCall(
            json = json,
            apiCall = {
                streamingApi.createPlaybackSession(
                    idempotencyKey = idempotencyKey,
                    request = CreatePlaybackSessionRequestDto(
                        movieId = movieId,
                        playableId = playableId,
                        sourceItemId = sourceItemId,
                        profileId = profileId
                    )
                )
            },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun sendHeartbeat(sessionId: String): Result<PlaybackHeartbeat> {
        return safeApiCall(
            json = json,
            apiCall = { streamingApi.sendHeartbeat(sessionId) },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun sendProgress(
        sessionId: String,
        seq: String,
        positionSeconds: Int,
        durationSeconds: Int?
    ): Result<PlaybackProgressAck> {
        return safeApiCall(
            json = json,
            apiCall = {
                streamingApi.sendProgress(
                    sessionId = sessionId,
                    request = WatchProgressRequestDto(
                        seq = seq,
                        positionSeconds = positionSeconds,
                        durationSeconds = durationSeconds
                    )
                )
            },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun sendEvent(
        sessionId: String,
        eventId: String,
        type: String,
        playedSeconds: Int?,
        reasonCode: String?
    ): Result<PlaybackEventAck> {
        return safeApiCall(
            json = json,
            apiCall = {
                streamingApi.sendEvent(
                    sessionId = sessionId,
                    request = PlaybackEventRequestDto(
                        eventId = eventId,
                        type = type,
                        playedSeconds = playedSeconds,
                        reasonCode = reasonCode
                    )
                )
            },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun renewMediaAuth(sessionId: String): Result<MediaAuth> {
        return safeApiCall(
            json = json,
            apiCall = { streamingApi.renewMediaAuth(sessionId) },
            transform = { dto -> dto.toDomain() }
        )
    }
}

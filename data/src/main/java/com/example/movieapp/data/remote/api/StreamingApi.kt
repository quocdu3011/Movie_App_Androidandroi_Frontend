package com.example.movieapp.data.remote.api

import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.data.remote.dto.CreatePlaybackSessionRequestDto
import com.example.movieapp.data.remote.dto.MediaAuthDto
import com.example.movieapp.data.remote.dto.PlaybackEventAckDto
import com.example.movieapp.data.remote.dto.PlaybackEventRequestDto
import com.example.movieapp.data.remote.dto.PlaybackHeartbeatDto
import com.example.movieapp.data.remote.dto.PlaybackProgressAckDto
import com.example.movieapp.data.remote.dto.PlaybackSessionDto
import com.example.movieapp.data.remote.dto.WatchProgressRequestDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface StreamingApi {

    @POST("streaming/playback-sessions")
    suspend fun createPlaybackSession(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreatePlaybackSessionRequestDto
    ): ApiResponseDto<PlaybackSessionDto>

    @POST("streaming/playback-sessions/{sessionId}/heartbeat")
    suspend fun sendHeartbeat(@Path("sessionId") sessionId: String): ApiResponseDto<PlaybackHeartbeatDto>

    @POST("streaming/playback-sessions/{sessionId}/progress")
    suspend fun sendProgress(
        @Path("sessionId") sessionId: String,
        @Body request: WatchProgressRequestDto
    ): ApiResponseDto<PlaybackProgressAckDto>

    @POST("streaming/playback-sessions/{sessionId}/events")
    suspend fun sendEvent(
        @Path("sessionId") sessionId: String,
        @Body request: PlaybackEventRequestDto
    ): ApiResponseDto<PlaybackEventAckDto>

    @POST("streaming/playback-sessions/{sessionId}/media-auth")
    suspend fun renewMediaAuth(@Path("sessionId") sessionId: String): ApiResponseDto<MediaAuthDto>
}

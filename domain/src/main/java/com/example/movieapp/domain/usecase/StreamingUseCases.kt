package com.example.movieapp.domain.usecase

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.MediaAuth
import com.example.movieapp.domain.model.PlaybackEventAck
import com.example.movieapp.domain.model.PlaybackHeartbeat
import com.example.movieapp.domain.model.PlaybackProgressAck
import com.example.movieapp.domain.model.PlaybackSession
import com.example.movieapp.domain.repository.StreamingRepository
import javax.inject.Inject

class CreatePlaybackSessionUseCase @Inject constructor(
    private val streamingRepository: StreamingRepository
) {
    suspend operator fun invoke(
        profileId: String,
        movieId: String,
        playableId: String,
        sourceItemId: String,
        idempotencyKey: String
    ): Result<PlaybackSession> {
        return streamingRepository.createPlaybackSession(
            profileId = profileId,
            movieId = movieId,
            playableId = playableId,
            sourceItemId = sourceItemId,
            idempotencyKey = idempotencyKey
        )
    }
}

class SendHeartbeatUseCase @Inject constructor(
    private val streamingRepository: StreamingRepository
) {
    suspend operator fun invoke(sessionId: String): Result<PlaybackHeartbeat> {
        return streamingRepository.sendHeartbeat(sessionId)
    }
}

class SendProgressUseCase @Inject constructor(
    private val streamingRepository: StreamingRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        positionSeconds: Int,
        durationSeconds: Int
    ): Result<PlaybackProgressAck> {
        return streamingRepository.sendProgress(
            sessionId = sessionId,
            positionSeconds = positionSeconds,
            durationSeconds = durationSeconds
        )
    }
}

class SendPlaybackEventUseCase @Inject constructor(
    private val streamingRepository: StreamingRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        eventType: String,
        eventData: String? = null
    ): Result<PlaybackEventAck> {
        return streamingRepository.sendEvent(sessionId, eventType, eventData)
    }
}

class RenewMediaAuthUseCase @Inject constructor(
    private val streamingRepository: StreamingRepository
) {
    suspend operator fun invoke(sessionId: String): Result<MediaAuth> {
        return streamingRepository.renewMediaAuth(sessionId)
    }
}

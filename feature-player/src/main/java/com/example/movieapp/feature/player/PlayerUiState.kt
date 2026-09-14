package com.example.movieapp.feature.player

import com.example.movieapp.domain.model.PlaybackSession

sealed interface StopReason {
    val code: String?
    val isFailure: Boolean

    object NormalStop : StopReason { override val code = null; override val isFailure = false }
    object PlaybackEnded : StopReason { override val code = null; override val isFailure = false }
    object LeaseRejected : StopReason { override val code = "LEASE_REJECTED"; override val isFailure = true }
    data class PlayerError(val message: String) : StopReason { override val code = "PLAYER_ERROR"; override val isFailure = true }
}

data class PlayerUiState(
    val isLoading: Boolean = true,
    val session: PlaybackSession? = null,
    val showResumeDialog: Boolean = false,
    val resumePositionSeconds: Int = 0,
    val errorMessage: String? = null,
    val canRetryManually: Boolean = false
)

package com.example.movieapp.core.player

sealed interface PlayerState {
    data object Idle : PlayerState
    data object Buffering : PlayerState
    data object Playing : PlayerState
    data object Paused : PlayerState
    data object Ended : PlayerState
    data class Error(val throwable: Throwable? = null) : PlayerState
}

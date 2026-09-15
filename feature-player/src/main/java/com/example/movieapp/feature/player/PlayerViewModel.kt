package com.example.movieapp.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.IdempotencyKeyGenerator
import com.example.movieapp.core.common.Result
import com.example.movieapp.core.player.PlayerManager
import com.example.movieapp.core.player.PlayerState
import com.example.movieapp.domain.model.MediaAuth
import com.example.movieapp.domain.model.PlaybackSession
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.CreatePlaybackSessionUseCase
import com.example.movieapp.domain.usecase.GetProfilesUseCase
import com.example.movieapp.domain.usecase.RenewMediaAuthUseCase
import com.example.movieapp.domain.usecase.SendHeartbeatUseCase
import com.example.movieapp.domain.usecase.SendPlaybackEventUseCase
import com.example.movieapp.domain.usecase.SendProgressUseCase
import com.example.movieapp.feature.player.di.PlaybackCompletionScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

import com.example.movieapp.domain.usecase.GetMovieDetailUseCase

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val playerManager: PlayerManager,
    private val createSessionUseCase: CreatePlaybackSessionUseCase,
    private val heartbeatUseCase: SendHeartbeatUseCase,
    private val progressUseCase: SendProgressUseCase,
    private val eventUseCase: SendPlaybackEventUseCase,
    private val renewMediaAuthUseCase: RenewMediaAuthUseCase,
    private val currentProfileStore: CurrentProfileStore,
    private val getProfilesUseCase: GetProfilesUseCase,
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    @PlaybackCompletionScope private val playbackCompletionScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    var session: PlaybackSession? = null
        private set

    var seq: Long = 0L
        private set

    var playedMillis: Long = 0L
        private set

    var qualifiedSent: Boolean = false
        private set

    var startedSent: Boolean = false
        private set

    var terminalSent: Boolean = false
        private set

    private var heartbeatJob: Job? = null
    private var progressJob: Job? = null
    private var playerStateJob: Job? = null
    private var mediaAuthRenewJob: Job? = null

    private var lastPlayingTimestamp: Long = 0L

    fun startPlayback(movieId: String, playableId: String, sourceItemId: String, profileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, canRetryManually = false) }

            var resolvedProfileId = profileId
            if (resolvedProfileId.isBlank() || resolvedProfileId == "active" || !isValidUuid(resolvedProfileId)) {
                resolvedProfileId = currentProfileStore.currentProfileId.value ?: ""
            }
            if (resolvedProfileId.isBlank() || !isValidUuid(resolvedProfileId)) {
                when (val profResult = getProfilesUseCase()) {
                    is Result.Success -> {
                        val firstProf = profResult.data.firstOrNull()
                        if (firstProf != null) {
                            resolvedProfileId = firstProf.id
                            currentProfileStore.setProfileId(firstProf.id)
                        }
                    }
                    else -> {}
                }
            }

            fetchMovieMetadata(movieId, playableId, resolvedProfileId)

            val idempotencyKey = IdempotencyKeyGenerator.generate()

            when (val result = createSessionUseCase(resolvedProfileId, movieId, playableId, sourceItemId, idempotencyKey)) {
                is Result.Success -> {
                    session = result.data
                    _uiState.update { it.copy(isLoading = false, session = result.data) }

                    if (result.data.resumeNeedsConfirmation) {
                        _uiState.update {
                            it.copy(
                                showResumeDialog = true,
                                resumePositionSeconds = result.data.resumePositionSeconds
                            )
                        }
                    } else {
                        preparePlayer(result.data, seekTo = result.data.resumePositionSeconds)
                    }
                }
                is Result.Error -> {
                    handlePlaybackError(result)
                }
            }
        }
    }

    private fun isValidUuid(string: String): Boolean {
        return try {
            UUID.fromString(string)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun onResumeConfirmed(resume: Boolean) {
        val currentSession = session ?: return
        _uiState.update { it.copy(showResumeDialog = false) }
        val seekTo = if (resume) currentSession.resumePositionSeconds else 0
        preparePlayer(currentSession, seekTo)
    }

    private fun preparePlayer(session: PlaybackSession, seekTo: Int) {
        playerManager.prepare(session.playbackUrl, startPositionMs = seekTo * 1000L)
        playerManager.play()

        observePlayerState()
        startHeartbeatLoop()
        startProgressLoop()

        val auth = session.mediaAuth
        if (session.sourceType == "owned" && auth != null) {
            startMediaAuthRenewLoop(auth)
        }
    }

    private fun observePlayerState() {
        playerStateJob?.cancel()
        playerStateJob = viewModelScope.launch {
            playerManager.playerState.collectLatest { state ->
                when (state) {
                    is PlayerState.Playing -> {
                        if (!startedSent) {
                            startedSent = true
                            sendEvent("started")
                        }
                        if (lastPlayingTimestamp == 0L) {
                            lastPlayingTimestamp = System.currentTimeMillis()
                        }
                    }
                    is PlayerState.Paused, is PlayerState.Buffering, is PlayerState.Idle -> {
                        updatePlayedMillis()
                    }
                    is PlayerState.Ended -> {
                        updatePlayedMillis()
                        finishSession(StopReason.PlaybackEnded)
                    }
                    is PlayerState.Error -> {
                        updatePlayedMillis()
                        finishSession(StopReason.PlayerError(state.throwable?.message ?: "Player error"))
                    }
                }
            }
        }
    }

    fun updatePlayedMillis() {
        if (lastPlayingTimestamp > 0L) {
            val now = System.currentTimeMillis()
            playedMillis += (now - lastPlayingTimestamp)
            lastPlayingTimestamp = if (playerManager.playerState.value is PlayerState.Playing) now else 0L
        }
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (!terminalSent) {
                delay(30_000)
                val s = session ?: break
                when (val result = heartbeatUseCase(s.sessionId)) {
                    is Result.Success -> {
                        // lease updated
                    }
                    is Result.Error -> {
                        finishSession(StopReason.LeaseRejected)
                        break
                    }
                }
            }
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (!terminalSent) {
                delay(12_000)
                val s = session ?: break
                val pos = playerManager.currentPositionSeconds()
                val dur = playerManager.durationSeconds()

                progressUseCase(s.sessionId, seq = (++seq).toString(), positionSeconds = pos, durationSeconds = dur)

                updatePlayedMillis()
                val playedSeconds = (playedMillis / 1000L).toInt()
                if (!qualifiedSent && playedSeconds >= 30) {
                    qualifiedSent = true
                    sendEvent("qualified", playedSeconds = playedSeconds)
                }
            }
        }
    }

    private fun startMediaAuthRenewLoop(mediaAuth: MediaAuth) {
        mediaAuthRenewJob?.cancel()
        mediaAuthRenewJob = viewModelScope.launch {
            var currentMediaAuth = mediaAuth
            while (!terminalSent) {
                val expiresAtMs = try {
                    Instant.parse(currentMediaAuth.expiresAt).toEpochMilli()
                } catch (_: Exception) {
                    System.currentTimeMillis() + 300_000
                }
                val delayMs = (expiresAtMs - System.currentTimeMillis() - 60_000).coerceAtLeast(10_000)
                delay(delayMs)

                val s = session ?: break
                when (val result = renewMediaAuthUseCase(s.sessionId)) {
                    is Result.Success -> {
                        currentMediaAuth = result.data
                    }
                    is Result.Error -> {
                        finishSession(StopReason.LeaseRejected)
                        break
                    }
                }
            }
        }
    }

    fun finishSession(reason: StopReason) {
        if (terminalSent) return
        terminalSent = true

        heartbeatJob?.cancel()
        progressJob?.cancel()
        mediaAuthRenewJob?.cancel()
        playerStateJob?.cancel()

        updatePlayedMillis()
        val s = session
        val pos = playerManager.currentPositionSeconds()
        val dur = playerManager.durationSeconds()
        val finalSeq = (++seq).toString()
        val playedSeconds = (playedMillis / 1000L).toInt()
        val eventId = UUID.randomUUID().toString()
        playerManager.pause()
        playerManager.release()

        // This scope survives ViewModel.onCleared(), which otherwise cancels the terminal request.
        playbackCompletionScope.launch {
            if (s != null) {
                // Final progress is best effort; a progress failure must not prevent lease release.
                runCatching { progressUseCase(s.sessionId, seq = finalSeq, positionSeconds = pos, durationSeconds = dur) }
                runCatching {
                    eventUseCase(
                        sessionId = s.sessionId,
                        eventId = eventId,
                        type = if (reason.isFailure) "failed" else "stopped",
                        playedSeconds = playedSeconds,
                        reasonCode = reason.code
                    )
                }
            }
        }
    }

    private fun sendEvent(type: String, playedSeconds: Int? = null, reasonCode: String? = null) {
        val s = session ?: return
        viewModelScope.launch {
            eventUseCase(
                sessionId = s.sessionId,
                eventId = UUID.randomUUID().toString(),
                type = type,
                playedSeconds = playedSeconds,
                reasonCode = reasonCode
            )
        }
    }

    private fun handlePlaybackError(error: Result.Error) {
        val message = when (error.code) {
            "CONCURRENT_STREAM_LIMIT" -> "Bạn đang phát tối đa số thiết bị cho phép. Vui lòng dừng một thiết bị khác trước."
            "PLAYBACK_MODE_UNSUPPORTED" -> "Nguồn này hiện chưa hỗ trợ phát trong app."
            "IDEMPOTENCY_KEY_REUSED" -> {
                savedStateHandle.remove<String>("sessionIdempotencyKey")
                "Có lỗi khi khởi tạo phiên phát, vui lòng thử lại."
            }
            else -> error.message
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message,
                canRetryManually = error.code == "IDEMPOTENCY_KEY_REUSED"
            )
        }
    }

    private fun fetchMovieMetadata(movieId: String, playableId: String, profileId: String) {
        viewModelScope.launch {
            when (val result = getMovieDetailUseCase(movieId, profileId)) {
                is Result.Success -> {
                    val detail = result.data
                    val sortedItems = detail.playableItems.sortedBy { it.sortOrder }
                    val idx = sortedItems.indexOfFirst { it.id == playableId }
                    val currentItem = if (idx >= 0) sortedItems[idx] else null
                    val prev = if (idx > 0) sortedItems[idx - 1] else null
                    val next = if (idx >= 0 && idx < sortedItems.size - 1) sortedItems[idx + 1] else null

                    _uiState.update {
                        it.copy(
                            movieTitle = detail.movie.title,
                            episodeTitle = currentItem?.label ?: "",
                            playableItems = sortedItems,
                            currentPlayableId = playableId,
                            previousPlayableItem = prev,
                            nextPlayableItem = next
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun setSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        _uiState.update { it.copy(currentSpeed = speed) }
    }

    fun setQuality(quality: String) {
        _uiState.update { it.copy(currentQuality = quality) }
    }

    fun seekRelative(deltaMs: Long) {
        playerManager.seekBy(deltaMs)
    }

    override fun onCleared() {
        finishSession(StopReason.NormalStop)
        super.onCleared()
    }
}

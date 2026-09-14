package com.example.movieapp.feature.player

import androidx.lifecycle.SavedStateHandle
import com.example.movieapp.core.common.Result
import com.example.movieapp.core.player.PlayerManager
import com.example.movieapp.core.player.PlayerState
import com.example.movieapp.domain.model.MediaAuth
import com.example.movieapp.domain.model.PlaybackEventAck
import com.example.movieapp.domain.model.PlaybackHeartbeat
import com.example.movieapp.domain.model.PlaybackProgressAck
import com.example.movieapp.domain.model.PlaybackSession
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.CreatePlaybackSessionUseCase
import com.example.movieapp.domain.usecase.GetProfilesUseCase
import com.example.movieapp.domain.usecase.RenewMediaAuthUseCase
import com.example.movieapp.domain.usecase.SendHeartbeatUseCase
import com.example.movieapp.domain.usecase.SendPlaybackEventUseCase
import com.example.movieapp.domain.usecase.SendProgressUseCase
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val savedStateHandle = SavedStateHandle()
    private val playerStateFlow = MutableStateFlow<PlayerState>(PlayerState.Idle)
    private val playerManager = mockk<PlayerManager>(relaxed = true) {
        every { playerState } returns playerStateFlow
        every { currentPositionSeconds() } returns 10
        every { durationSeconds() } returns 100
    }

    private val createSessionUseCase = mockk<CreatePlaybackSessionUseCase>()
    private val heartbeatUseCase = mockk<SendHeartbeatUseCase>()
    private val progressUseCase = mockk<SendProgressUseCase>()
    private val eventUseCase = mockk<SendPlaybackEventUseCase>()
    private val renewMediaAuthUseCase = mockk<RenewMediaAuthUseCase>()
    private val currentProfileStore = mockk<CurrentProfileStore>(relaxed = true) {
        every { currentProfileId } returns MutableStateFlow("550e8400-e29b-41d4-a716-446655440000")
    }
    private val getProfilesUseCase = mockk<GetProfilesUseCase>(relaxed = true)

    private lateinit var viewModel: PlayerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { progressUseCase(any(), any(), any(), any()) } returns Result.Success(
            PlaybackProgressAck(sessionId = "s1", accepted = true, applied = true)
        )
        coEvery { eventUseCase(any(), any(), any(), any(), any()) } returns Result.Success(
            PlaybackEventAck(sessionId = "s1", eventId = "e1", duplicate = false, state = "playing", qualified = false)
        )
        coEvery { heartbeatUseCase(any()) } returns Result.Success(
            PlaybackHeartbeat(sessionId = "s1", leaseExpiresAt = "2026-12-31T23:59:59Z")
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startPlayback handles resumeNeedsConfirmation`() = runTest {
        val session = PlaybackSession(
            sessionId = "sess-1", playableId = "p1", sourceItemId = "src1", sourceType = "owned",
            protocol = "hls", playbackUrl = "http://stream.m3u8", leaseExpiresAt = "2026-12-31T23:59:59Z",
            resumePositionSeconds = 120, resumeNeedsConfirmation = true, urlExpiresAt = ""
        )
        coEvery { createSessionUseCase(any(), any(), any(), any(), any()) } returns Result.Success(session)

        viewModel = PlayerViewModel(
            savedStateHandle, playerManager, createSessionUseCase,
            heartbeatUseCase, progressUseCase, eventUseCase, renewMediaAuthUseCase,
            currentProfileStore, getProfilesUseCase
        )
        viewModel.startPlayback("m1", "p1", "src1", "550e8400-e29b-41d4-a716-446655440000")
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.showResumeDialog)
        assertEquals(120, viewModel.uiState.value.resumePositionSeconds)
    }

    @Test
    fun `finishSession is idempotent and sends progress before stopped event`() = runTest {
        val session = PlaybackSession(
            sessionId = "sess-1", playableId = "p1", sourceItemId = "src1", sourceType = "third_party",
            protocol = "hls", playbackUrl = "http://stream.m3u8", leaseExpiresAt = "2026-12-31T23:59:59Z",
            resumePositionSeconds = 0, resumeNeedsConfirmation = false, urlExpiresAt = ""
        )
        coEvery { createSessionUseCase(any(), any(), any(), any(), any()) } returns Result.Success(session)

        viewModel = PlayerViewModel(
            savedStateHandle, playerManager, createSessionUseCase,
            heartbeatUseCase, progressUseCase, eventUseCase, renewMediaAuthUseCase,
            currentProfileStore, getProfilesUseCase
        )
        viewModel.startPlayback("m1", "p1", "src1", "550e8400-e29b-41d4-a716-446655440000")
        testDispatcher.scheduler.runCurrent()

        viewModel.finishSession(StopReason.NormalStop)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.terminalSent)

        // Verify progress is called BEFORE stopped event
        coVerifyOrder {
            progressUseCase("sess-1", seq = "1", positionSeconds = 10, durationSeconds = 100)
            eventUseCase("sess-1", any(), type = "stopped", playedSeconds = any(), reasonCode = null)
        }

        // Call again to verify idempotency
        viewModel.finishSession(StopReason.NormalStop)
        testDispatcher.scheduler.runCurrent()

        // seq should not increase further
        assertEquals(1L, viewModel.seq)
    }

    @Test
    fun `heartbeat rejection triggers finishSession with LeaseRejected`() = runTest {
        val session = PlaybackSession(
            sessionId = "sess-1", playableId = "p1", sourceItemId = "src1", sourceType = "third_party",
            protocol = "hls", playbackUrl = "http://stream.m3u8", leaseExpiresAt = "2026-12-31T23:59:59Z",
            resumePositionSeconds = 0, resumeNeedsConfirmation = false, urlExpiresAt = ""
        )
        coEvery { createSessionUseCase(any(), any(), any(), any(), any()) } returns Result.Success(session)
        coEvery { heartbeatUseCase("sess-1") } returns Result.Error(code = "401", message = "Lease expired")

        viewModel = PlayerViewModel(
            savedStateHandle, playerManager, createSessionUseCase,
            heartbeatUseCase, progressUseCase, eventUseCase, renewMediaAuthUseCase,
            currentProfileStore, getProfilesUseCase
        )
        viewModel.startPlayback("m1", "p1", "src1", "550e8400-e29b-41d4-a716-446655440000")
        testDispatcher.scheduler.runCurrent()

        // Fast-forward 30 seconds for heartbeat loop
        testDispatcher.scheduler.advanceTimeBy(30_000)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.terminalSent)
    }
}

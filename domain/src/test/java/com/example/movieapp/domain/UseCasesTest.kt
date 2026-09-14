package com.example.movieapp.domain

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.MediaAuth
import com.example.movieapp.domain.repository.ProfileRepository
import com.example.movieapp.domain.repository.StreamingRepository
import com.example.movieapp.domain.usecase.GetWatchHistoryUseCase
import com.example.movieapp.domain.usecase.RenewMediaAuthUseCase
import com.example.movieapp.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UseCasesTest {

    private lateinit var profileRepository: ProfileRepository
    private lateinit var streamingRepository: StreamingRepository

    @Before
    fun setUp() {
        profileRepository = mockk(relaxed = true)
        streamingRepository = mockk(relaxed = true)
    }

    @Test
    fun `test GetWatchHistoryUseCase filters out tombstone items`() = runTest {
        val historyList = listOf(
            HistoryItem("m1", "p1", "s1", 100, 200, "2026-09-14", tombstone = false),
            HistoryItem("m2", "p2", "s2", 50, 200, "2026-09-14", tombstone = true),
            HistoryItem("m3", "p3", "s3", 180, 200, "2026-09-14", tombstone = false)
        )

        coEvery { profileRepository.getWatchHistory("prof_1") } returns Result.Success(historyList)

        val useCase = GetWatchHistoryUseCase(profileRepository)
        val result = useCase("prof_1")

        assertTrue(result is Result.Success)
        val activeItems = (result as Result.Success).data
        assertEquals(2, activeItems.size)
        assertEquals("m1", activeItems[0].movieId)
        assertEquals("m3", activeItems[1].movieId)
    }

    @Test
    fun `test ToggleFavoriteUseCase calls delete when currently favorite and put when not`() = runTest {
        coEvery { profileRepository.deleteFavorite("prof_1", "m1") } returns Result.Success(Unit)
        coEvery { profileRepository.putFavorite("prof_1", "m2") } returns Result.Success(Unit)

        val useCase = ToggleFavoriteUseCase(profileRepository)

        // Toggle from favorite -> calls delete
        useCase("prof_1", "m1", isCurrentlyFavorite = true)
        coVerify(exactly = 1) { profileRepository.deleteFavorite("prof_1", "m1") }

        // Toggle from not favorite -> calls put
        useCase("prof_1", "m2", isCurrentlyFavorite = false)
        coVerify(exactly = 1) { profileRepository.putFavorite("prof_1", "m2") }
    }

    @Test
    fun `test RenewMediaAuthUseCase returns MediaAuth`() = runTest {
        val mediaAuth = MediaAuth(expiresAt = "2026-09-14T12:00:00Z")
        coEvery { streamingRepository.renewMediaAuth("session_1") } returns Result.Success(mediaAuth)

        val useCase = RenewMediaAuthUseCase(streamingRepository)
        val result = useCase("session_1")

        assertTrue(result is Result.Success)
        assertEquals("2026-09-14T12:00:00Z", (result as Result.Success).data.expiresAt)
    }
}

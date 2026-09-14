package com.example.movieapp.feature.profile

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.AuthSession
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.Profile
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.DeleteProfileUseCase
import com.example.movieapp.domain.usecase.GetFavoritesUseCase
import com.example.movieapp.domain.usecase.GetProfilesUseCase
import com.example.movieapp.domain.usecase.GetWatchHistoryUseCase
import com.example.movieapp.domain.usecase.LogoutUseCase
import com.example.movieapp.domain.usecase.RestoreSessionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val currentProfileStore = mockk<CurrentProfileStore>(relaxed = true)
    private val profileIdFlow = MutableStateFlow<String?>("p1")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { currentProfileStore.currentProfileId } returns profileIdFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ProfileListViewModel loadProfiles success and delete active profile resets store`() = runTest {
        val getProfilesUseCase = mockk<GetProfilesUseCase>()
        val deleteProfileUseCase = mockk<DeleteProfileUseCase>()

        val profiles = listOf(
            Profile(id = "p1", name = "Adult Profile", isKids = false, createdAt = "", updatedAt = ""),
            Profile(id = "p2", name = "Kids Profile", isKids = true, createdAt = "", updatedAt = "")
        )
        coEvery { getProfilesUseCase() } returns Result.Success(profiles)
        coEvery { deleteProfileUseCase("p1") } returns Result.Success(Unit)

        val viewModel = ProfileListViewModel(getProfilesUseCase, deleteProfileUseCase, currentProfileStore)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is ProfileListUiState.Success)
        val state = viewModel.uiState.value as ProfileListUiState.Success
        assertEquals(2, state.profiles.size)
        assertEquals("p1", state.activeProfileId)

        viewModel.deleteProfile("p1")
        testDispatcher.scheduler.runCurrent()

        coVerify { currentProfileStore.setProfileId(null) }
    }

    @Test
    fun `FavoritesViewModel loadFavorites success`() = runTest {
        val getFavoritesUseCase = mockk<GetFavoritesUseCase>()
        val movies = listOf(
            Movie(
                id = "m1", title = "Fav Movie", originTitle = null, description = null,
                posterUrl = null, backdropUrl = null, releaseYear = 2024, type = "movie",
                contentKind = "movie", status = "published", accessTier = "free", isKidsSafe = true,
                averageRating = 9.0, publishedAt = null, version = "1", createdAt = "", updatedAt = ""
            )
        )
        coEvery { getFavoritesUseCase("p1") } returns Result.Success(movies)

        val viewModel = FavoritesViewModel(getFavoritesUseCase, currentProfileStore)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is FavoritesUiState.Success)
        val state = viewModel.uiState.value as FavoritesUiState.Success
        assertEquals("m1", state.movies.first().id)
    }

    @Test
    fun `WatchHistoryViewModel loadHistory success`() = runTest {
        val getWatchHistoryUseCase = mockk<GetWatchHistoryUseCase>()
        val items = listOf(
            HistoryItem(
                movieId = "m1", playableId = "pl1", sourceItemId = "s1",
                positionSeconds = 100, durationSeconds = 3000, updatedAt = "", tombstone = false
            )
        )
        coEvery { getWatchHistoryUseCase("p1") } returns Result.Success(items)

        val viewModel = WatchHistoryViewModel(getWatchHistoryUseCase, currentProfileStore)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is WatchHistoryUiState.Success)
        val state = viewModel.uiState.value as WatchHistoryUiState.Success
        assertEquals("pl1", state.items.first().playableId)
    }

    @Test
    fun `SettingsViewModel logout clears profile and session`() = runTest {
        val restoreSessionUseCase = mockk<RestoreSessionUseCase>()
        val logoutUseCase = mockk<LogoutUseCase>()

        val session = AuthSession(active = true, userId = "u1", email = "test@example.com", fullName = "Test User")
        coEvery { restoreSessionUseCase() } returns Result.Success(session)
        coEvery { logoutUseCase(any()) } returns Result.Success(Unit)

        val viewModel = SettingsViewModel(restoreSessionUseCase, logoutUseCase, currentProfileStore)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SettingsUiState.Success)

        viewModel.logout()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SettingsUiState.LoggedOut)
        coVerify { currentProfileStore.setProfileId(null) }
    }
}

package com.example.movieapp.feature.home

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.HomeSection
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.PersonalizedHome
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.GetPersonalizedHomeUseCase
import com.example.movieapp.domain.usecase.GetPublicHomeUseCase
import io.mockk.coEvery
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val profileIdFlow = MutableStateFlow<String?>(null)
    private val currentProfileStore = mockk<CurrentProfileStore> {
        coEvery { currentProfileId } returns profileIdFlow
    }
    private val getPublicHomeUseCase = mockk<GetPublicHomeUseCase>()
    private val getPersonalizedHomeUseCase = mockk<GetPersonalizedHomeUseCase>()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when profileId is null loads public home catalog`() = runTest {
        val sampleMovie = Movie(
            id = "m1", title = "Public Movie", type = "movie", contentKind = "film",
            status = "published", accessTier = "free", isKidsSafe = true, averageRating = 8.0,
            version = "1", createdAt = "", updatedAt = ""
        )
        coEvery { getPublicHomeUseCase() } returns Result.Success(
            CatalogHome(newReleases = listOf(sampleMovie))
        )

        viewModel = HomeViewModel(currentProfileStore, getPublicHomeUseCase, getPersonalizedHomeUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Public)
        val publicState = state as HomeUiState.Public
        assertEquals(1, publicState.catalogHome.newReleases.size)
        assertEquals("m1", publicState.catalogHome.newReleases[0].id)
    }

    @Test
    fun `when profileId is present loads personalized home catalog`() = runTest {
        profileIdFlow.value = "profile-123"
        val historyItem = HistoryItem(
            movieId = "m2", playableId = "p1", sourceItemId = "s1",
            positionSeconds = 50, durationSeconds = 100, updatedAt = ""
        )
        coEvery { getPersonalizedHomeUseCase("profile-123") } returns Result.Success(
            PersonalizedHome(
                profileId = "profile-123",
                sections = listOf(HomeSection.ContinueWatching(listOf(historyItem)))
            )
        )

        viewModel = HomeViewModel(currentProfileStore, getPublicHomeUseCase, getPersonalizedHomeUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Personalized)
        val personalizedState = state as HomeUiState.Personalized
        assertEquals("profile-123", personalizedState.personalizedHome.profileId)
        assertEquals(1, personalizedState.personalizedHome.sections.size)
        val section = personalizedState.personalizedHome.sections[0] as HomeSection.ContinueWatching
        assertEquals("m2", section.items[0].movieId)
    }

    @Test
    fun `handles error gracefully`() = runTest {
        coEvery { getPublicHomeUseCase() } returns Result.Error(
            code = "HTTP_500",
            message = "Internal Server Error",
            requestId = "req-500"
        )

        viewModel = HomeViewModel(currentProfileStore, getPublicHomeUseCase, getPersonalizedHomeUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
        val errorState = state as HomeUiState.Error
        assertEquals("Internal Server Error", errorState.message)
        assertEquals("req-500", errorState.requestId)
    }
}

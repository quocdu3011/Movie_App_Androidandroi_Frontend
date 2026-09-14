package com.example.movieapp.feature.search

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.BrowseMoviesUseCase
import com.example.movieapp.domain.usecase.SearchMoviesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val searchMoviesUseCase = mockk<SearchMoviesUseCase>()
    private val browseMoviesUseCase = mockk<BrowseMoviesUseCase>()
    private val currentProfileStore = mockk<CurrentProfileStore>()
    private val searchHistoryStorage = mockk<SearchHistoryStorage>(relaxed = true)

    private val profileIdFlow = MutableStateFlow<String?>("p1")
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { currentProfileStore.currentProfileId } returns profileIdFlow
        every { searchHistoryStorage.getHistory() } returns flowOf(listOf("Action", "Comedy"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onQueryChanged debounces and calls SearchMoviesUseCase`() = runTest {
        val movies = listOf(
            Movie(
                id = "m1", title = "Action Movie", originTitle = "Action Movie",
                description = "", posterUrl = "", backdropUrl = "", releaseYear = 2024,
                type = "movie", contentKind = "movie", status = "published",
                accessTier = "free", isKidsSafe = true, averageRating = 8.5,
                publishedAt = "", version = "1", createdAt = "", updatedAt = ""
            )
        )
        coEvery { searchMoviesUseCase("Action", 1, 24, "p1") } returns Result.Success(movies)

        viewModel = SearchViewModel(
            searchMoviesUseCase, browseMoviesUseCase, currentProfileStore, searchHistoryStorage
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.onQueryChanged("Action")
        advanceTimeBy(100)
        testDispatcher.scheduler.runCurrent()

        advanceTimeBy(250)
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SearchUiState.Success)
        val state = viewModel.uiState.value as SearchUiState.Success
        assertEquals("m1", state.movies.first().id)
        coVerify { searchHistoryStorage.addQuery("Action") }
    }

    @Test
    fun `onFilterChanged calls BrowseMoviesUseCase`() = runTest {
        val movies = listOf(
            Movie(
                id = "m2", title = "Filter Movie", originTitle = "Filter Movie",
                description = "", posterUrl = "", backdropUrl = "", releaseYear = 2024,
                type = "series", contentKind = "series", status = "published",
                accessTier = "free", isKidsSafe = true, averageRating = 8.0,
                publishedAt = "", version = "1", createdAt = "", updatedAt = ""
            )
        )
        coEvery { browseMoviesUseCase(page = 1, pageSize = 24, type = "series", profileId = "p1") } returns Result.Success(movies)

        viewModel = SearchViewModel(
            searchMoviesUseCase, browseMoviesUseCase, currentProfileStore, searchHistoryStorage
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.onFilterChanged(SearchFilter(type = "series"))
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value is SearchUiState.Success)
        val state = viewModel.uiState.value as SearchUiState.Success
        assertTrue(state.isBrowseMode)
        assertEquals("m2", state.movies.first().id)
    }

    @Test
    fun `onClearHistory delegates to storage`() = runTest {
        viewModel = SearchViewModel(
            searchMoviesUseCase, browseMoviesUseCase, currentProfileStore, searchHistoryStorage
        )
        testDispatcher.scheduler.runCurrent()

        viewModel.onClearHistory()
        testDispatcher.scheduler.runCurrent()

        coVerify { searchHistoryStorage.clearHistory() }
    }
}

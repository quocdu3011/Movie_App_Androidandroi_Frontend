package com.example.movieapp.feature.detail

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PlayableItem
import com.example.movieapp.domain.model.SourceItem
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.GetFavoritesUseCase
import com.example.movieapp.domain.usecase.GetMovieDetailUseCase
import com.example.movieapp.domain.usecase.ToggleFavoriteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val profileIdFlow = MutableStateFlow<String?>("profile-123")
    private val currentProfileStore = mockk<CurrentProfileStore> {
        coEvery { currentProfileId } returns profileIdFlow
    }
    private val getMovieDetailUseCase = mockk<GetMovieDetailUseCase>()
    private val toggleFavoriteUseCase = mockk<ToggleFavoriteUseCase>()
    private val getFavoritesUseCase = mockk<GetFavoritesUseCase>()

    private lateinit var viewModel: MovieDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getFavoritesUseCase("profile-123") } returns Result.Success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMovieDetail filters valid sources correctly`() = runTest {
        val movie = Movie(
            id = "m1", title = "Series 1", type = "series", contentKind = "film",
            status = "published", accessTier = "free", isKidsSafe = true, averageRating = 8.0,
            version = "1", createdAt = "", updatedAt = ""
        )
        val playables = listOf(
            PlayableItem(id = "p1", kind = "episode", seasonNumber = 1, episodeNumber = 1, label = "Ep 1", sortOrder = 1)
        )
        val sources = listOf(
            SourceItem(id = "s1", sourceType = "owned", sourceStatus = "available", sourceItemId = "src-item-123", playableId = "p1"),
            SourceItem(id = "s2", sourceType = "owned", sourceStatus = "unavailable", sourceItemId = "src-item-456", playableId = "p1"),
            SourceItem(id = "s3", sourceType = "owned", sourceStatus = "available", sourceItemId = null, playableId = "p1")
        )
        val detail = MovieDetail(movie = movie, playableItems = playables, sources = sources)

        coEvery { getMovieDetailUseCase("m1", "profile-123") } returns Result.Success(detail)

        viewModel = MovieDetailViewModel(
            getMovieDetailUseCase,
            toggleFavoriteUseCase,
            getFavoritesUseCase,
            currentProfileStore
        )
        viewModel.loadMovieDetail("m1")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.availableSources.size)
        assertEquals("s1", state.availableSources[0].id)
        assertEquals("src-item-123", state.selectedSourceItem?.sourceItemId)
    }

    @Test
    fun `toggleFavorite invokes ToggleFavoriteUseCase`() = runTest {
        val movie = Movie(
            id = "m1", title = "Movie 1", type = "movie", contentKind = "film",
            status = "published", accessTier = "free", isKidsSafe = true, averageRating = 8.0,
            version = "1", createdAt = "", updatedAt = ""
        )
        val detail = MovieDetail(movie = movie)
        coEvery { getMovieDetailUseCase("m1", "profile-123") } returns Result.Success(detail)
        coEvery { toggleFavoriteUseCase("profile-123", "m1", false) } returns Result.Success(Unit)

        viewModel = MovieDetailViewModel(
            getMovieDetailUseCase,
            toggleFavoriteUseCase,
            getFavoritesUseCase,
            currentProfileStore
        )
        viewModel.loadMovieDetail("m1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleFavorite()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { toggleFavoriteUseCase("profile-123", "m1", false) }
        assertTrue(viewModel.uiState.value.isFavorite)
    }
}

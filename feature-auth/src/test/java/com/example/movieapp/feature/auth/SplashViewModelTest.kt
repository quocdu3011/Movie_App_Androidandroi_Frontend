package com.example.movieapp.feature.auth

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.AuthSession
import com.example.movieapp.domain.usecase.RestoreSessionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val restoreSessionUseCase = mockk<RestoreSessionUseCase>()

    private lateinit var viewModel: SplashViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SplashViewModel(restoreSessionUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkSession emits Authenticated when active is true`() = runTest {
        coEvery { restoreSessionUseCase() } returns Result.Success(
            AuthSession(active = true, userId = "user-1")
        )

        viewModel.checkSession()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Authenticated)
    }

    @Test
    fun `checkSession emits Unauthenticated when active is false`() = runTest {
        coEvery { restoreSessionUseCase() } returns Result.Success(
            AuthSession(active = false)
        )

        viewModel.checkSession()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Unauthenticated)
    }

    @Test
    fun `checkSession emits Unauthenticated when restoreSession fails`() = runTest {
        coEvery { restoreSessionUseCase() } returns Result.Error(
            code = "HTTP_401",
            message = "Unauthorized"
        )

        viewModel.checkSession()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SplashUiState.Unauthenticated)
    }
}

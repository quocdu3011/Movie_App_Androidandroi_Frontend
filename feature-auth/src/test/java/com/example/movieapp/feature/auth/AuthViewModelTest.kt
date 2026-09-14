package com.example.movieapp.feature.auth

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.TokenPair
import com.example.movieapp.domain.model.User
import com.example.movieapp.domain.usecase.LoginUseCase
import com.example.movieapp.domain.usecase.RegisterUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val loginUseCase = mockk<LoginUseCase>()
    private val registerUseCase = mockk<RegisterUseCase>()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(loginUseCase, registerUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login fails client-side validation when password is shorter than 12 chars`() {
        viewModel.login("test@example.com", "short")

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("INVALID_PASSWORD", (state as AuthUiState.Error).code)
    }

    @Test
    fun `login emits LoginSuccess when loginUseCase succeeds`() = runTest {
        coEvery { loginUseCase("test@example.com", "validpassword123", null) } returns Result.Success(
            TokenPair("access_token", "refresh_token", 3600)
        )

        viewModel.login("test@example.com", "validpassword123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.LoginSuccess)
    }

    @Test
    fun `register fails client-side validation when fullName is empty`() {
        viewModel.register("test@example.com", "validpassword123", "")

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals("INVALID_FULL_NAME", (state as AuthUiState.Error).code)
    }

    @Test
    fun `register emits RegisterSuccess when registerUseCase succeeds`() = runTest {
        coEvery { registerUseCase("test@example.com", "validpassword123", "John Doe") } returns Result.Success(
            User("user-1", "test@example.com", "John Doe", "user")
        )

        viewModel.register("test@example.com", "validpassword123", "John Doe")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.RegisterSuccess)
        assertEquals("test@example.com", (state as AuthUiState.RegisterSuccess).userEmail)
    }
}

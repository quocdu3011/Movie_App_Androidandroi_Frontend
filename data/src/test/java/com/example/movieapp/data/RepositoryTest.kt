package com.example.movieapp.data

import com.example.movieapp.core.common.Result
import com.example.movieapp.core.network.DeviceIdProvider
import com.example.movieapp.core.network.TokenStorage
import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.core.network.model.ErrorDto
import com.example.movieapp.core.network.model.TokenPairDto
import com.example.movieapp.data.remote.api.AuthApi
import com.example.movieapp.data.remote.api.ProfileApi
import com.example.movieapp.data.remote.api.StreamingApi
import com.example.movieapp.data.remote.dto.PlaybackProgressAckDto
import com.example.movieapp.data.remote.dto.PutFavoriteResponseDto
import com.example.movieapp.data.repository.AuthRepositoryImpl
import com.example.movieapp.data.repository.CatalogRepositoryImpl
import com.example.movieapp.data.repository.ProfileRepositoryImpl
import com.example.movieapp.data.repository.StreamingRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `AuthRepositoryImpl login saves tokens on success`() = runTest {
        val authApi = mockk<AuthApi>()
        val deviceIdProvider = mockk<DeviceIdProvider>()
        val tokenStorage = mockk<TokenStorage>(relaxed = true)

        coEvery { deviceIdProvider.getOrCreate() } returns "device-123"
        coEvery { authApi.login(any()) } returns ApiResponseDto(
            success = true,
            data = TokenPairDto("access_123", "refresh_123", 3600),
            requestId = "req-1"
        )

        val repository = AuthRepositoryImpl(authApi, deviceIdProvider, tokenStorage, json)
        val result = repository.login("test@example.com", "password123")

        assertTrue(result is Result.Success)
        val tokenPair = (result as Result.Success).data
        assertEquals("access_123", tokenPair.accessToken)
        assertEquals("refresh_123", tokenPair.refreshToken)

        coVerify { tokenStorage.saveTokens("access_123", "refresh_123") }
    }

    @Test
    fun `AuthRepositoryImpl login returns Error on failure with error code`() = runTest {
        val authApi = mockk<AuthApi>()
        val deviceIdProvider = mockk<DeviceIdProvider>()
        val tokenStorage = mockk<TokenStorage>(relaxed = true)

        coEvery { deviceIdProvider.getOrCreate() } returns "device-123"
        coEvery { authApi.login(any()) } returns ApiResponseDto(
            success = false,
            data = null,
            error = ErrorDto("INVALID_CREDENTIALS", "Email or password incorrect"),
            requestId = "req-err-1"
        )

        val repository = AuthRepositoryImpl(authApi, deviceIdProvider, tokenStorage, json)
        val result = repository.login("test@example.com", "wrongpass")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("INVALID_CREDENTIALS", error.code)
        assertEquals("Email or password incorrect", error.message)
        assertEquals("req-err-1", error.requestId)
    }

    @Test
    fun `ProfileRepositoryImpl putFavorite calls API without body`() = runTest {
        val profileApi = mockk<ProfileApi>()

        coEvery { profileApi.putFavorite("profile-1", "movie-1") } returns ApiResponseDto(
            success = true,
            data = PutFavoriteResponseDto(profileId = "profile-1", movieId = "movie-1", created = true),
            requestId = "req-fav"
        )

        val repository = ProfileRepositoryImpl(profileApi, json)
        val result = repository.putFavorite("profile-1", "movie-1")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { profileApi.putFavorite("profile-1", "movie-1") }
    }

    @Test
    fun `ProfileRepositoryImpl deleteFavorite calls API returning 204`() = runTest {
        val profileApi = mockk<ProfileApi>()

        coEvery { profileApi.deleteFavorite("profile-1", "movie-1") } returns Response.success(Unit)

        val repository = ProfileRepositoryImpl(profileApi, json)
        val result = repository.deleteFavorite("profile-1", "movie-1")

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { profileApi.deleteFavorite("profile-1", "movie-1") }
    }

    @Test
    fun `StreamingRepositoryImpl sendProgress passes seq and durationSeconds`() = runTest {
        val streamingApi = mockk<StreamingApi>()

        coEvery { streamingApi.sendProgress("sess-1", any()) } returns ApiResponseDto(
            success = true,
            data = PlaybackProgressAckDto(sessionId = "sess-1", accepted = true, applied = true),
            requestId = "req-prog"
        )

        val repository = StreamingRepositoryImpl(streamingApi, json)
        val result = repository.sendProgress("sess-1", seq = "1", positionSeconds = 30, durationSeconds = 1200)

        assertTrue(result is Result.Success)
        coVerify {
            streamingApi.sendProgress(
                "sess-1",
                match { it.seq == "1" && it.positionSeconds == 30 && it.durationSeconds == 1200 }
            )
        }
    }
}

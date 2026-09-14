package com.example.movieapp.core.network

import com.example.movieapp.core.network.api.RefreshTokenApi
import com.example.movieapp.core.network.interceptor.TokenAuthenticator
import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.core.network.model.TokenPairDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RetrofitResponse

class TokenAuthenticatorTest {

    private lateinit var tokenStorage: TokenStorage
    private lateinit var refreshTokenApi: RefreshTokenApi
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setUp() {
        tokenStorage = mockk(relaxed = true)
        refreshTokenApi = mockk(relaxed = true)
        authenticator = TokenAuthenticator(tokenStorage, refreshTokenApi)
    }

    @Test
    fun `test priorResponse prevents infinite retry loops`() {
        val priorResponse = Response.Builder()
            .request(Request.Builder().url("http://localhost/api/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        val response = Response.Builder()
            .request(Request.Builder().url("http://localhost/api/test").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .priorResponse(priorResponse)
            .build()

        val result = authenticator.authenticate(null, response)

        assertNull(result)
        coVerify(exactly = 0) { refreshTokenApi.refresh(any()) }
    }

    @Test
    fun `test concurrent 401 requests trigger RefreshTokenApi EXACTLY ONCE (single-flight)`() = runTest {
        val oldAccessToken = "token_old"
        val oldRefreshToken = "refresh_old"
        val newAccessToken = "token_new"
        val newRefreshToken = "refresh_new"

        var storedAccessToken: String? = oldAccessToken
        var storedRefreshToken: String? = oldRefreshToken

        every { tokenStorage.getAccessToken() } answers { storedAccessToken }
        every { tokenStorage.getRefreshToken() } answers { storedRefreshToken }
        every { tokenStorage.saveTokens(any(), any()) } answers {
            storedAccessToken = firstArg()
            storedRefreshToken = secondArg()
        }

        val successApiResponse = ApiResponseDto(
            success = true,
            data = TokenPairDto(
                accessToken = newAccessToken,
                refreshToken = newRefreshToken,
                expiresIn = 3600
            ),
            error = null,
            requestId = "req_refresh_123"
        )

        coEvery { refreshTokenApi.refresh(any()) } answers {
            // Simulate brief network delay
            Thread.sleep(50)
            RetrofitResponse.success(successApiResponse)
        }

        fun create401Response(): Response {
            return Response.Builder()
                .request(
                    Request.Builder()
                        .url("http://localhost/api/resource")
                        .header("Authorization", "Bearer $oldAccessToken")
                        .build()
                )
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .build()
        }

        // Launch 3 concurrent worker requests that get 401 simultaneously
        val deferred1 = async(Dispatchers.IO) { authenticator.authenticate(null, create401Response()) }
        val deferred2 = async(Dispatchers.IO) { authenticator.authenticate(null, create401Response()) }
        val deferred3 = async(Dispatchers.IO) { authenticator.authenticate(null, create401Response()) }

        val results = awaitAll(deferred1, deferred2, deferred3)

        // Single-flight verification: Refresh API MUST be called EXACTLY ONCE
        coVerify(exactly = 1) { refreshTokenApi.refresh(any()) }

        // All 3 requests must get a valid retry request with newAccessToken
        results.forEach { request ->
            assertNotNull(request)
            assertEquals("Bearer $newAccessToken", request?.header("Authorization"))
        }

        // TokenStorage must be updated with new tokens
        verify(exactly = 1) { tokenStorage.saveTokens(newAccessToken, newRefreshToken) }
    }

    @Test
    fun `test refresh failure clears tokenStorage and returns null`() = runTest {
        every { tokenStorage.getAccessToken() } returns "old_token"
        every { tokenStorage.getRefreshToken() } returns "old_refresh"

        coEvery { refreshTokenApi.refresh(any()) } returns RetrofitResponse.error(
            401,
            okhttp3.ResponseBody.create(null, "")
        )

        val response = Response.Builder()
            .request(
                Request.Builder()
                    .url("http://localhost/api/resource")
                    .header("Authorization", "Bearer old_token")
                    .build()
            )
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()

        val result = authenticator.authenticate(null, response)

        assertNull(result)
        verify(exactly = 1) { tokenStorage.clear() }
    }
}

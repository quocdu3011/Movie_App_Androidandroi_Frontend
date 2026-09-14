package com.example.movieapp.core.network.interceptor

import com.example.movieapp.core.network.TokenStorage
import com.example.movieapp.core.network.api.RefreshTokenApi
import com.example.movieapp.core.network.model.RefreshTokenRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TokenAuthenticator implements okhttp3.Authenticator for handling 401 Unauthorized responses.
 *
 * Single-flight mechanics:
 * The Mutex serializes 401 retry handling across multiple concurrent worker threads.
 * Crucially, Mutex alone only serializes execution; the token equality check (`failedToken != currentToken`)
 * inside the lock creates true single-flight behavior.
 * When 3 parallel requests receive 401 with token T1:
 * - Request 1 acquires the Mutex lock first, observes currentToken == T1, calls RefreshTokenApi,
 *   obtains token T2, and saves (T2_access, T2_refresh) into TokenStorage.
 * - Request 2 and Request 3 enter the Mutex lock sequentially after Request 1 finishes.
 *   They observe currentToken is now T2 (different from T1), so they immediately retry with T2
 *   WITHOUT calling RefreshTokenApi again.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val refreshTokenApi: RefreshTokenApi
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite retry loops: maximum 1 retry attempt
        if (response.priorResponse != null) {
            return null
        }

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        return runBlocking {
            mutex.withLock {
                val currentToken = tokenStorage.getAccessToken()

                // Single-flight check: if token changed while waiting for lock, another thread already refreshed it.
                if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                    return@withLock response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                // Token has not changed; execute refresh
                val refreshToken = tokenStorage.getRefreshToken() ?: run {
                    tokenStorage.clear()
                    return@withLock null
                }

                try {
                    val refreshResponse = refreshTokenApi.refresh(RefreshTokenRequestDto(refreshToken))
                    if (refreshResponse.isSuccessful) {
                        val apiResponse = refreshResponse.body()
                        val tokenPair = apiResponse?.data
                        if (apiResponse?.success == true && tokenPair != null) {
                            // Backend rotates refresh token on every refresh call: overwrite BOTH tokens
                            tokenStorage.saveTokens(tokenPair.accessToken, tokenPair.refreshToken)

                            return@withLock response.request.newBuilder()
                                .header("Authorization", "Bearer ${tokenPair.accessToken}")
                                .build()
                        }
                    }
                } catch (_: Exception) {
                    // Network error during token refresh
                }

                // Refresh failed (e.g. 401 refresh token expired / invalid): clear session and abort
                tokenStorage.clear()
                null
            }
        }
    }
}

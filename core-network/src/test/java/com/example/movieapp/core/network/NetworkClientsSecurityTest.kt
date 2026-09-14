package com.example.movieapp.core.network

import com.example.movieapp.core.network.api.RefreshTokenApi
import com.example.movieapp.core.network.di.NetworkModule
import com.example.movieapp.core.network.interceptor.AuthInterceptor
import com.example.movieapp.core.network.interceptor.TokenAuthenticator
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkClientsSecurityTest {

    private lateinit var tokenStorage: TokenStorage
    private lateinit var refreshTokenApi: RefreshTokenApi
    private lateinit var authInterceptor: AuthInterceptor
    private lateinit var tokenAuthenticator: TokenAuthenticator
    private lateinit var cookieJarProvider: SharedCookieJarProvider

    private lateinit var backendOkHttpClient: OkHttpClient
    private lateinit var refreshOkHttpClient: OkHttpClient
    private lateinit var mediaOkHttpClient: OkHttpClient

    @Before
    fun setUp() {
        tokenStorage = mockk(relaxed = true)
        refreshTokenApi = mockk(relaxed = true)

        authInterceptor = AuthInterceptor(tokenStorage)
        tokenAuthenticator = TokenAuthenticator(tokenStorage, refreshTokenApi)
        cookieJarProvider = SharedCookieJarProvider()

        backendOkHttpClient = NetworkModule.provideBackendOkHttpClient(
            authInterceptor = authInterceptor,
            tokenAuthenticator = tokenAuthenticator,
            cookieJarProvider = cookieJarProvider
        )

        refreshOkHttpClient = NetworkModule.provideRefreshOkHttpClient()

        mediaOkHttpClient = NetworkModule.provideMediaOkHttpClient(
            cookieJarProvider = cookieJarProvider
        )
    }

    @Test
    fun `test MediaHttpClient NEVER includes Authorization header`() {
        every { tokenStorage.getAccessToken() } returns "secret_jwt_token_123"

        // MediaHttpClient has no AuthInterceptor
        val request = Request.Builder()
            .url("https://external-cdn.example.com/hls/stream.m3u8")
            .build()

        assertTrue(mediaOkHttpClient.interceptors.none { it is AuthInterceptor })
        assertNull(mediaOkHttpClient.authenticator as? TokenAuthenticator)
        assertNull(request.header("Authorization"))
    }

    @Test
    fun `test MediaHttpClient does NOT trigger RefreshTokenApi on 401`() {
        assertNotEquals(tokenAuthenticator, mediaOkHttpClient.authenticator)
        assertEquals(okhttp3.Authenticator.NONE, mediaOkHttpClient.authenticator)

        coVerify(exactly = 0) { refreshTokenApi.refresh(any()) }
    }

    @Test
    fun `test MediaHttpClient and BackendApiClient share the EXACT SAME CookieJar instance`() {
        assertEquals(backendOkHttpClient.cookieJar, mediaOkHttpClient.cookieJar)
        assertEquals(cookieJarProvider.cookieJar, mediaOkHttpClient.cookieJar)

        // Save a cookie to shared CookieJar
        val url = "https://movie.example.com/api".toHttpUrl()
        val cookie = Cookie.Builder()
            .domain("movie.example.com")
            .path("/api")
            .name("session_cookie")
            .value("cookie_val_123")
            .build()

        cookieJarProvider.cookieJar.saveFromResponse(url, listOf(cookie))

        val backendCookies = backendOkHttpClient.cookieJar.loadForRequest(url)
        val mediaCookies = mediaOkHttpClient.cookieJar.loadForRequest(url)

        assertEquals(1, backendCookies.size)
        assertEquals(1, mediaCookies.size)
        assertEquals("session_cookie", mediaCookies[0].name)

        // Cookie is NOT sent to unrelated domains
        val externalUrl = "https://unrelated-domain.com/test".toHttpUrl()
        val externalCookies = mediaOkHttpClient.cookieJar.loadForRequest(externalUrl)
        assertTrue(externalCookies.isEmpty())
    }

    @Test
    fun `test RefreshClient has no AuthInterceptor or TokenAuthenticator`() {
        assertTrue(refreshOkHttpClient.interceptors.none { it is AuthInterceptor })
        assertEquals(okhttp3.Authenticator.NONE, refreshOkHttpClient.authenticator)
    }
}

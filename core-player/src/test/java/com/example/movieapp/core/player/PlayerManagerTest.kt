package com.example.movieapp.core.player

import android.content.Context
import com.example.movieapp.core.network.SharedCookieJarProvider
import com.example.movieapp.core.network.di.NetworkModule
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PlayerManagerTest {

    private lateinit var context: Context
    private lateinit var mediaHttpClient: OkHttpClient
    private lateinit var cookieJarProvider: SharedCookieJarProvider

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        cookieJarProvider = SharedCookieJarProvider()
        mediaHttpClient = NetworkModule.provideMediaOkHttpClient(cookieJarProvider)
    }

    @Test
    fun `test PlayerManager accepts qualified MediaHttpClient via constructor injection`() {
        val playerManager = PlayerManager(context, mediaHttpClient)

        assertNotNull(playerManager)
        assertEquals(mediaHttpClient, playerManager.mediaHttpClient)
    }

    @Test
    fun `test injected mediaHttpClient has no AuthInterceptor and does not attach Authorization header`() {
        val playerManager = PlayerManager(context, mediaHttpClient)

        val request = Request.Builder()
            .url("https://media-cdn.example.com/hls/stream_720p.m3u8")
            .build()

        assertNull("Media requests MUST NOT contain Authorization header", request.header("Authorization"))
        assertEquals(cookieJarProvider.cookieJar, playerManager.mediaHttpClient.cookieJar)
    }
}

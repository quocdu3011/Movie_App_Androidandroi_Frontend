package com.example.movieapp.core.network

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TokenStorageTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tokenStorage: EncryptedTokenStorage

    @Before
    fun setUp() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor

        tokenStorage = EncryptedTokenStorage(sharedPreferences)
    }

    @Test
    fun `test saveTokens updates in-memory access token and persisted refresh token`() {
        tokenStorage.saveTokens("access_token_1", "refresh_token_1")

        assertEquals("access_token_1", tokenStorage.getAccessToken())
        verify { editor.putString("refresh_token", "refresh_token_1") }
    }

    @Test
    fun `test clear removes access token and refresh token`() {
        tokenStorage.saveTokens("access_token_1", "refresh_token_1")
        tokenStorage.clear()

        assertNull(tokenStorage.getAccessToken())
        verify { editor.remove("refresh_token") }
    }

    @Test
    fun `test getRefreshToken reads from sharedPreferences`() {
        every { sharedPreferences.getString("refresh_token", null) } returns "saved_refresh_token"

        assertEquals("saved_refresh_token", tokenStorage.getRefreshToken())
    }
}

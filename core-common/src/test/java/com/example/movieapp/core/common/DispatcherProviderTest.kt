package com.example.movieapp.core.common

import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test

class DispatcherProviderTest {

    @Test
    fun `test DefaultDispatcherProvider returns standard dispatchers`() {
        val provider: DispatcherProvider = DefaultDispatcherProvider()

        assertEquals(Dispatchers.Main, provider.main)
        assertEquals(Dispatchers.IO, provider.io)
        assertEquals(Dispatchers.Default, provider.default)
        assertEquals(Dispatchers.Unconfined, provider.unconfined)
    }
}

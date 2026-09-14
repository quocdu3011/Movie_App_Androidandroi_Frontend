package com.example.movieapp.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `test Result Success data and helper functions`() {
        val result: Result<String> = Result.Success("movie_123")

        assertTrue(result is Result.Success)
        assertEquals("movie_123", (result as Result.Success).data)
        assertEquals("movie_123", result.getOrNull())
        assertEquals("movie_123", result.getOrDefault("default"))

        var onSuccessCalled = false
        result.onSuccess {
            onSuccessCalled = true
            assertEquals("movie_123", it)
        }
        assertTrue(onSuccessCalled)

        val mapped = result.map { it.uppercase() }
        assertEquals("MOVIE_123", mapped.getOrNull())
    }

    @Test
    fun `test Result Error details and helper functions`() {
        val exception = RuntimeException("Network down")
        val result: Result<String> = Result.Error(
            code = "NETWORK_ERROR",
            message = "Unable to connect",
            requestId = "req_123456",
            throwable = exception
        )

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("NETWORK_ERROR", error.code)
        assertEquals("Unable to connect", error.message)
        assertEquals("req_123456", error.requestId)
        assertEquals(exception, error.throwable)

        assertNull(result.getOrNull())
        assertEquals("default", result.getOrDefault("default"))

        var onErrorCalled = false
        result.onError {
            onErrorCalled = true
            assertEquals("NETWORK_ERROR", it.code)
            assertEquals("req_123456", it.requestId)
        }
        assertTrue(onErrorCalled)

        val mapped = result.map { str: String -> str.length }
        assertTrue(mapped is Result.Error)
        assertEquals("NETWORK_ERROR", (mapped as Result.Error).code)
    }
}

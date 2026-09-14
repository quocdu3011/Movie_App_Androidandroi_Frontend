package com.example.movieapp.core.common

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdempotencyKeyGeneratorTest {

    @Test
    fun `test generate returns valid key within constraints`() {
        val key1 = IdempotencyKeyGenerator.generate()
        val key2 = IdempotencyKeyGenerator.generate()

        assertNotEquals(key1, key2)
        assertTrue("Length should be between 8 and 120", key1.length in 8..120)
        assertTrue("Key should match allowed charset [A-Za-z0-9._:-]", key1.matches(Regex("^[A-Za-z0-9._:-]+$")))
    }
}

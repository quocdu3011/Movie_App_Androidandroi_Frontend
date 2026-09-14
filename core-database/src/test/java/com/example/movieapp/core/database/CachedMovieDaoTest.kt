package com.example.movieapp.core.database

import com.example.movieapp.core.database.dao.CachedMovieDao
import com.example.movieapp.core.database.entity.CachedMovieEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CachedMovieDaoTest {

    private lateinit var cachedMovieDao: CachedMovieDao

    @Before
    fun setUp() {
        cachedMovieDao = mockk(relaxed = true)
    }

    @Test
    fun `test upsert and getAll movies flow`() = runTest {
        val moviesList = listOf(
            CachedMovieEntity(id = "m1", title = "Movie 1", posterUrl = "http://poster1.jpg", cachedAt = 1000L),
            CachedMovieEntity(id = "m2", title = "Movie 2", posterUrl = "http://poster2.jpg", cachedAt = 2000L)
        )

        every { cachedMovieDao.getAll() } returns flowOf(moviesList)

        val result = cachedMovieDao.getAll().first()

        assertEquals(2, result.size)
        assertEquals("m1", result[0].id)
        assertEquals("Movie 1", result[0].title)
        verify(exactly = 1) { cachedMovieDao.getAll() }
    }

    @Test
    fun `test deleteOlderThan calls dao with threshold timestamp`() = runTest {
        val threshold = 5000L

        cachedMovieDao.deleteOlderThan(threshold)

        coVerify(exactly = 1) { cachedMovieDao.deleteOlderThan(threshold) }
    }
}

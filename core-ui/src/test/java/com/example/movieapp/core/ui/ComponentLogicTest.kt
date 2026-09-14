package com.example.movieapp.core.ui

import com.example.movieapp.core.ui.component.MovieItemUiModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ComponentLogicTest {

    @Test
    fun `test MovieItemUiModel holds correct values`() {
        val model = MovieItemUiModel(
            id = "movie_1",
            title = "Test Movie",
            posterUrl = "http://example.com/poster.jpg",
            progressPercent = 0.75f
        )

        assertEquals("movie_1", model.id)
        assertEquals("Test Movie", model.title)
        assertEquals("http://example.com/poster.jpg", model.posterUrl)
        assertEquals(0.75f, model.progressPercent)
    }
}

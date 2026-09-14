package com.example.movieapp.data

import com.example.movieapp.data.mapper.toDomain
import com.example.movieapp.data.remote.dto.CatalogHomeDto
import com.example.movieapp.data.remote.dto.CatalogHomeSectionDto
import com.example.movieapp.data.remote.dto.CountryRefDto
import com.example.movieapp.data.remote.dto.GenreRefDto
import com.example.movieapp.data.remote.dto.HomeSectionDto
import com.example.movieapp.data.remote.dto.MediaAuthDto
import com.example.movieapp.data.remote.dto.MovieDetailDto
import com.example.movieapp.data.remote.dto.MovieDto
import com.example.movieapp.data.remote.dto.PaymentOrderDto
import com.example.movieapp.data.remote.dto.PersonalizedHomeDto
import com.example.movieapp.data.remote.dto.PlayableItemDto
import com.example.movieapp.data.remote.dto.PlaybackSessionDto
import com.example.movieapp.data.remote.dto.ProfileDto
import com.example.movieapp.data.remote.dto.SourceItemDto
import com.example.movieapp.data.remote.dto.UserDto
import com.example.movieapp.domain.model.HomeSection
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `MovieDto toDomain maps all fields correctly`() {
        val dto = MovieDto(
            id = "movie-1",
            title = "Inception",
            originTitle = "Inception Original",
            description = "A dream within a dream",
            posterUrl = "http://poster.png",
            backdropUrl = "http://backdrop.png",
            releaseYear = 2010,
            type = "movie",
            contentKind = "film",
            status = "published",
            accessTier = "subscription",
            isKidsSafe = false,
            averageRating = 8.8,
            publishedAt = "2010-07-16T00:00:00Z",
            version = "1",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        val domain = dto.toDomain()

        assertEquals("movie-1", domain.id)
        assertEquals("Inception", domain.title)
        assertEquals("Inception Original", domain.originTitle)
        assertEquals(2010, domain.releaseYear)
        assertEquals("film", domain.contentKind)
        assertEquals("subscription", domain.accessTier)
        assertEquals(false, domain.isKidsSafe)
        assertEquals(8.8, domain.averageRating, 0.01)
    }

    @Test
    fun `MovieDetailDto toDomain maps nested lists correctly`() {
        val dto = MovieDetailDto(
            id = "movie-2",
            title = "Movie 2",
            type = "movie",
            contentKind = "film",
            status = "published",
            accessTier = "free",
            isKidsSafe = true,
            averageRating = 7.5,
            version = "1",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            genres = listOf(GenreRefDto(slug = "action", name = "Action")),
            countries = listOf(CountryRefDto(isoCode = "US", name = "USA")),
            playableItems = listOf(
                PlayableItemDto(id = "p-1", kind = "movie", label = "Main", sortOrder = 1)
            ),
            sources = listOf(
                SourceItemDto(id = "s-1", sourceType = "owned", sourceStatus = "available")
            )
        )
        val domain = dto.toDomain()

        assertEquals("movie-2", domain.movie.id)
        assertEquals(1, domain.genres.size)
        assertEquals("Action", domain.genres[0].name)
        assertEquals("p-1", domain.playableItems[0].id)
        assertEquals("s-1", domain.sources[0].id)
    }

    @Test
    fun `PersonalizedHomeDto toDomain maps sections dynamically`() {
        val movieDto = MovieDto(
            id = "m-1",
            title = "New Movie",
            type = "movie",
            contentKind = "film",
            status = "published",
            accessTier = "free",
            isKidsSafe = true,
            version = "1",
            createdAt = "",
            updatedAt = ""
        )
        val movieJson = json.encodeToJsonElement(movieDto)

        val dto = PersonalizedHomeDto(
            profileId = "prof-1",
            sections = listOf(
                HomeSectionDto(
                    type = "catalog_new_releases",
                    items = listOf(movieJson)
                ),
                HomeSectionDto(
                    type = "fallback_new_releases",
                    reason = "recommendation_unavailable",
                    items = listOf(movieJson)
                )
            )
        )

        val domain = dto.toDomain(json)

        assertEquals("prof-1", domain.profileId)
        assertEquals(2, domain.sections.size)

        val sec1 = domain.sections[0] as HomeSection.CatalogNewReleases
        assertEquals(1, sec1.items.size)
        assertEquals("m-1", sec1.items[0].id)

        val sec2 = domain.sections[1] as HomeSection.FallbackNewReleases
        assertEquals("recommendation_unavailable", sec2.reason)
        assertEquals(1, sec2.items.size)
    }

    @Test
    fun `PlaybackSessionDto toDomain maps session correctly`() {
        val dto = PlaybackSessionDto(
            sessionId = "sess-123",
            playableId = "p-1",
            sourceItemId = "s-1",
            sourceType = "owned",
            protocol = "hls",
            playbackUrl = "http://stream.m3u8",
            mediaAuth = MediaAuthDto(expiresAt = "2026-12-31T23:59:59Z"),
            leaseExpiresAt = "2026-12-31T23:59:59Z",
            resumePositionSeconds = 120,
            resumeNeedsConfirmation = true
        )
        val domain = dto.toDomain()

        assertEquals("sess-123", domain.sessionId)
        assertEquals(120, domain.resumePositionSeconds)
        assertTrue(domain.resumeNeedsConfirmation)
        assertNotNull(domain.mediaAuth)
        assertEquals("2026-12-31T23:59:59Z", domain.mediaAuth?.expiresAt)
    }
}

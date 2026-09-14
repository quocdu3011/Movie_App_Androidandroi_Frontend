package com.example.movieapp.data.mapper

import com.example.movieapp.data.remote.dto.CatalogHomeDto
import com.example.movieapp.data.remote.dto.CountryRefDto
import com.example.movieapp.data.remote.dto.GenreRefDto
import com.example.movieapp.data.remote.dto.HistoryItemDto
import com.example.movieapp.data.remote.dto.MovieDetailDto
import com.example.movieapp.data.remote.dto.MovieDto
import com.example.movieapp.data.remote.dto.PersonalizedHomeDto
import com.example.movieapp.data.remote.dto.PlayableItemDto
import com.example.movieapp.data.remote.dto.SourceItemDto
import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.CountryRef
import com.example.movieapp.domain.model.GenreRef
import com.example.movieapp.domain.model.HomeSection
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PersonalizedHome
import com.example.movieapp.domain.model.PlayableItem
import com.example.movieapp.domain.model.SourceItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

fun MovieDto.toDomain(): Movie = Movie(
    id = id,
    title = title,
    originTitle = originTitle,
    description = description,
    posterUrl = posterUrl,
    backdropUrl = backdropUrl,
    releaseYear = releaseYear,
    type = type,
    contentKind = contentKind,
    status = status,
    accessTier = accessTier,
    isKidsSafe = isKidsSafe,
    averageRating = averageRating,
    publishedAt = publishedAt,
    version = version,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GenreRefDto.toDomain(): GenreRef = GenreRef(
    id = id ?: slug ?: "",
    name = name
)

fun CountryRefDto.toDomain(): CountryRef = CountryRef(
    isoCode = isoCode ?: slug ?: "",
    name = name
)

fun PlayableItemDto.toDomain(): PlayableItem = PlayableItem(
    id = id,
    kind = kind,
    seasonId = seasonId,
    seasonNumber = seasonNumber,
    episodeNumber = episodeNumber,
    label = label,
    sortOrder = sortOrder,
    durationSeconds = durationSeconds,
    archivedAt = archivedAt
)

fun SourceItemDto.toDomain(): SourceItem = SourceItem(
    id = id,
    sourceType = sourceType,
    provider = provider,
    sourceStatus = sourceStatus ?: "available",
    sourceItemId = sourceItemId,
    playableId = playableId,
    serverKey = serverKey,
    serverLabel = serverLabel,
    playbackMode = playbackMode
)

fun MovieDetailDto.toDomain(): MovieDetail {
    val movie = Movie(
        id = id,
        title = title,
        originTitle = originTitle,
        description = description,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        releaseYear = releaseYear,
        type = type,
        contentKind = contentKind,
        status = status,
        accessTier = accessTier,
        isKidsSafe = isKidsSafe,
        averageRating = averageRating,
        publishedAt = publishedAt,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    return MovieDetail(
        movie = movie,
        genres = genres.map { it.toDomain() },
        countries = countries.map { it.toDomain() },
        playableItems = playableItems.map { it.toDomain() },
        sources = sources.map { it.toDomain() }
    )
}

fun CatalogHomeDto.toDomain(): CatalogHome = CatalogHome(
    newReleases = newReleases?.items?.map { it.toDomain() } ?: emptyList(),
    topRated = topRated?.items?.map { it.toDomain() } ?: emptyList(),
    trending = trending?.items?.map { it.toDomain() } ?: emptyList()
)

fun PersonalizedHomeDto.toDomain(json: Json): PersonalizedHome {
    val mappedSections = sections.map { sectionDto ->
        when (sectionDto.type) {
            "continue_watching" -> {
                val items = sectionDto.items.mapNotNull { element ->
                    try { json.decodeFromJsonElement<HistoryItemDto>(element).toDomain() } catch (_: Exception) { null }
                }
                HomeSection.ContinueWatching(items)
            }
            "catalog_new_releases" -> {
                val items = sectionDto.items.mapNotNull { element ->
                    try { json.decodeFromJsonElement<MovieDto>(element).toDomain() } catch (_: Exception) { null }
                }
                HomeSection.CatalogNewReleases(items)
            }
            "fallback_new_releases" -> {
                val items = sectionDto.items.mapNotNull { element ->
                    try { json.decodeFromJsonElement<MovieDto>(element).toDomain() } catch (_: Exception) { null }
                }
                HomeSection.FallbackNewReleases(sectionDto.reason ?: "recommendation_unavailable", items)
            }
            else -> {
                val items = sectionDto.items.mapNotNull { element ->
                    try { json.decodeFromJsonElement<MovieDto>(element).toDomain() } catch (_: Exception) { null }
                }
                HomeSection.Other(sectionDto.type, items)
            }
        }
    }
    return PersonalizedHome(
        profileId = profileId,
        sections = mappedSections
    )
}

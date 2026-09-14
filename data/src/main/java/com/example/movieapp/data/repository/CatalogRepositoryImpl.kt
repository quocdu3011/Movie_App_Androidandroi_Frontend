package com.example.movieapp.data.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.data.mapper.toDomain
import com.example.movieapp.data.remote.api.CatalogApi
import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PersonalizedHome
import com.example.movieapp.domain.repository.CatalogRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CatalogRepositoryImpl @Inject constructor(
    private val catalogApi: CatalogApi,
    private val json: Json
) : CatalogRepository {

    override suspend fun getPublicHome(pageSize: Int): Result<CatalogHome> {
        return safeApiCall(
            json = json,
            apiCall = { catalogApi.getCatalogHome(pageSize) },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun getPersonalizedHome(profileId: String): Result<PersonalizedHome> {
        return safeApiCall(
            json = json,
            apiCall = { catalogApi.getPersonalizedHome(profileId) },
            transform = { dto -> dto.toDomain(json) }
        )
    }

    override suspend fun getMovies(
        page: Int?,
        pageSize: Int?,
        q: String?,
        genre: String?,
        country: String?,
        year: Int?,
        type: String?,
        contentKind: String?,
        sourceType: String?,
        provider: String?,
        sort: String?,
        profileId: String?
    ): Result<List<Movie>> {
        return safeApiCall(
            json = json,
            apiCall = {
                catalogApi.getMovies(
                    page = page,
                    pageSize = pageSize,
                    q = q,
                    genre = genre,
                    country = country,
                    year = year,
                    type = type,
                    contentKind = contentKind,
                    sourceType = sourceType,
                    provider = provider,
                    sort = sort,
                    profileId = profileId
                )
            },
            transform = { paged -> paged.items.map { it.toDomain() } }
        )
    }

    override suspend fun search(
        q: String,
        page: Int,
        pageSize: Int,
        profileId: String?
    ): Result<List<Movie>> {
        return safeApiCall(
            json = json,
            apiCall = { catalogApi.search(q = q, page = page, pageSize = pageSize, profileId = profileId) },
            transform = { paged -> paged.items.map { it.toDomain() } }
        )
    }

    override suspend fun getMovieDetail(movieId: String, profileId: String?): Result<MovieDetail> {
        return safeApiCall(
            json = json,
            apiCall = { catalogApi.getMovieDetail(movieId = movieId, profileId = profileId) },
            transform = { dto -> dto.toDomain() }
        )
    }
}

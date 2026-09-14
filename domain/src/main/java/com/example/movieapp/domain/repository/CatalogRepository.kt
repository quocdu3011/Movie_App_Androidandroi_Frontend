package com.example.movieapp.domain.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.CatalogHome
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.MovieDetail
import com.example.movieapp.domain.model.PersonalizedHome

interface CatalogRepository {
    suspend fun getPublicHome(pageSize: Int = 10): Result<CatalogHome>
    suspend fun getPersonalizedHome(profileId: String): Result<PersonalizedHome>
    suspend fun getMovies(
        type: String? = null,
        contentKind: String? = null,
        genreId: String? = null,
        countryCode: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<List<Movie>>
    suspend fun search(
        q: String,
        page: Int = 1,
        pageSize: Int = 20,
        profileId: String? = null
    ): Result<List<Movie>>
    suspend fun getMovieDetail(movieId: String, profileId: String? = null): Result<MovieDetail>
}

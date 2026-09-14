package com.example.movieapp.data.remote.api

import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.core.network.model.PagedResponseDto
import com.example.movieapp.data.remote.dto.CatalogHomeDto
import com.example.movieapp.data.remote.dto.MovieDetailDto
import com.example.movieapp.data.remote.dto.MovieDto
import com.example.movieapp.data.remote.dto.PersonalizedHomeDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {

    @GET("catalog/home")
    suspend fun getCatalogHome(
        @Query("pageSize") pageSize: Int? = null
    ): ApiResponseDto<CatalogHomeDto>

    @GET("catalog/movies")
    suspend fun getMovies(
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("q") q: String? = null,
        @Query("genre") genre: String? = null,
        @Query("country") country: String? = null,
        @Query("year") year: Int? = null,
        @Query("type") type: String? = null,
        @Query("contentKind") contentKind: String? = null,
        @Query("sourceType") sourceType: String? = null,
        @Query("provider") provider: String? = null,
        @Query("sort") sort: String? = null,
        @Query("profileId") profileId: String? = null
    ): ApiResponseDto<PagedResponseDto<MovieDto>>

    @GET("catalog/search")
    suspend fun search(
        @Query("q") q: String,
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
        @Query("profileId") profileId: String? = null
    ): ApiResponseDto<PagedResponseDto<MovieDto>>

    @GET("catalog/movies/{movieId}")
    suspend fun getMovieDetail(
        @Path("movieId") movieId: String,
        @Query("profileId") profileId: String? = null
    ): ApiResponseDto<MovieDetailDto>

    @GET("home")
    suspend fun getPersonalizedHome(
        @Query("profileId") profileId: String
    ): ApiResponseDto<PersonalizedHomeDto>
}

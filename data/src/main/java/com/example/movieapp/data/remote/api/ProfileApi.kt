package com.example.movieapp.data.remote.api

import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.data.remote.dto.CreateProfileDto
import com.example.movieapp.data.remote.dto.FavoritesResponseDto
import com.example.movieapp.data.remote.dto.ProfileDto
import com.example.movieapp.data.remote.dto.PutFavoriteResponseDto
import com.example.movieapp.data.remote.dto.UpdateProfileDto
import com.example.movieapp.data.remote.dto.WatchHistoryResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProfileApi {

    @GET("profiles")
    suspend fun getProfiles(): ApiResponseDto<List<ProfileDto>>

    @POST("profiles")
    suspend fun createProfile(@Body request: CreateProfileDto): ApiResponseDto<ProfileDto>

    @PATCH("profiles/{profileId}")
    suspend fun updateProfile(
        @Path("profileId") profileId: String,
        @Body request: UpdateProfileDto
    ): ApiResponseDto<ProfileDto>

    @DELETE("profiles/{profileId}")
    suspend fun deleteProfile(@Path("profileId") profileId: String): Response<Unit>

    @GET("profiles/{profileId}/favorites")
    suspend fun getFavorites(@Path("profileId") profileId: String): ApiResponseDto<FavoritesResponseDto>

    @PUT("profiles/{profileId}/favorites/{movieId}")
    suspend fun putFavorite(
        @Path("profileId") profileId: String,
        @Path("movieId") movieId: String
    ): ApiResponseDto<PutFavoriteResponseDto>

    @DELETE("profiles/{profileId}/favorites/{movieId}")
    suspend fun deleteFavorite(
        @Path("profileId") profileId: String,
        @Path("movieId") movieId: String
    ): Response<Unit>

    @GET("profiles/{profileId}/watch-history")
    suspend fun getWatchHistory(@Path("profileId") profileId: String): ApiResponseDto<WatchHistoryResponseDto>
}

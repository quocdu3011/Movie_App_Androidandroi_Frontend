package com.example.movieapp.core.network.api

import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.core.network.model.RefreshTokenRequestDto
import com.example.movieapp.core.network.model.TokenPairDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface RefreshTokenApi {
    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequestDto): Response<ApiResponseDto<TokenPairDto>>
}

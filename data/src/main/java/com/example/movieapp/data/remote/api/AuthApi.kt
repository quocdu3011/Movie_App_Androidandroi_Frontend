package com.example.movieapp.data.remote.api

import com.example.movieapp.core.network.model.ApiResponseDto
import com.example.movieapp.core.network.model.TokenPairDto
import com.example.movieapp.data.remote.dto.AuthSessionDto
import com.example.movieapp.data.remote.dto.LoginRequestDto
import com.example.movieapp.data.remote.dto.LogoutRequestDto
import com.example.movieapp.data.remote.dto.RefreshRequestDto
import com.example.movieapp.data.remote.dto.RegisterRequestDto
import com.example.movieapp.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequestDto): ApiResponseDto<UserDto>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequestDto): ApiResponseDto<TokenPairDto>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequestDto): ApiResponseDto<TokenPairDto>

    @POST("auth/logout")
    suspend fun logout(@Body request: LogoutRequestDto): Response<Unit>

    @GET("auth/session")
    suspend fun getSession(): ApiResponseDto<AuthSessionDto>
}

package com.example.movieapp.data.mapper

import com.example.movieapp.core.network.model.TokenPairDto
import com.example.movieapp.data.remote.dto.AuthSessionDto
import com.example.movieapp.data.remote.dto.UserDto
import com.example.movieapp.domain.model.AuthSession
import com.example.movieapp.domain.model.TokenPair
import com.example.movieapp.domain.model.User

fun UserDto.toDomain(): User = User(
    id = id,
    email = email,
    fullName = fullName,
    role = role
)

fun TokenPairDto.toDomain(): TokenPair = TokenPair(
    accessToken = accessToken,
    refreshToken = refreshToken,
    expiresIn = expiresIn
)

fun AuthSessionDto.toDomain(): AuthSession = AuthSession(
    active = active,
    userId = userId,
    sessionId = sessionId,
    role = role,
    email = email,
    fullName = fullName
)

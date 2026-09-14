package com.example.movieapp.data.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.data.mapper.toDomain
import com.example.movieapp.data.remote.api.ProfileApi
import com.example.movieapp.data.remote.dto.CreateProfileDto
import com.example.movieapp.data.remote.dto.UpdateProfileDto
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.Profile
import com.example.movieapp.domain.repository.ProfileRepository
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    private val json: Json
) : ProfileRepository {

    override suspend fun getProfiles(): Result<List<Profile>> {
        return safeApiCall(
            json = json,
            apiCall = { profileApi.getProfiles() },
            transform = { dtoList -> dtoList.map { it.toDomain() } }
        )
    }

    override suspend fun createProfile(name: String, avatarId: Int?, isKids: Boolean): Result<Profile> {
        return safeApiCall(
            json = json,
            apiCall = { profileApi.createProfile(CreateProfileDto(name, avatarId, isKids)) },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun updateProfile(id: String, name: String?, avatarId: Int?, isKids: Boolean?): Result<Profile> {
        return safeApiCall(
            json = json,
            apiCall = { profileApi.updateProfile(id, UpdateProfileDto(name, avatarId, isKids)) },
            transform = { dto -> dto.toDomain() }
        )
    }

    override suspend fun deleteProfile(id: String): Result<Unit> {
        return safeVoidApiCall(
            json = json,
            apiCall = { profileApi.deleteProfile(id) }
        )
    }

    override suspend fun getFavorites(profileId: String): Result<List<Movie>> {
        return safeApiCall(
            json = json,
            apiCall = { profileApi.getFavorites(profileId) },
            transform = { res -> res.items.map { it.toDomain() } }
        )
    }

    override suspend fun putFavorite(profileId: String, movieId: String): Result<Unit> {
        return safeApiCall(
            json = json,
            apiCall = { profileApi.putFavorite(profileId, movieId) },
            transform = { Unit }
        )
    }

    override suspend fun deleteFavorite(profileId: String, movieId: String): Result<Unit> {
        return safeVoidApiCall(
            json = json,
            apiCall = { profileApi.deleteFavorite(profileId, movieId) }
        )
    }

    override suspend fun getWatchHistory(profileId: String): Result<List<HistoryItem>> {
        return safeApiCall(
            json = json,
            apiCall = { profileApi.getWatchHistory(profileId) },
            transform = { res -> res.items.map { it.toDomain() } }
        )
    }
}

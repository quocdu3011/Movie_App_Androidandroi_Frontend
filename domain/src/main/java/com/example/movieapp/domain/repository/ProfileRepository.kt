package com.example.movieapp.domain.repository

import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.Profile

interface ProfileRepository {
    suspend fun getProfiles(): Result<List<Profile>>
    suspend fun createProfile(name: String, avatarId: Int? = null, isKids: Boolean = false): Result<Profile>
    suspend fun updateProfile(id: String, name: String? = null, avatarId: Int? = null, isKids: Boolean? = null): Result<Profile>
    suspend fun deleteProfile(id: String): Result<Unit>
    suspend fun getFavorites(profileId: String): Result<List<Movie>>
    suspend fun putFavorite(profileId: String, movieId: String): Result<Unit>
    suspend fun deleteFavorite(profileId: String, movieId: String): Result<Unit>
    suspend fun getWatchHistory(profileId: String): Result<List<HistoryItem>>
}

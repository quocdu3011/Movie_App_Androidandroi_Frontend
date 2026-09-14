package com.example.movieapp.domain.usecase

import com.example.movieapp.core.common.Result
import com.example.movieapp.core.common.map
import com.example.movieapp.domain.model.HistoryItem
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.model.Profile
import com.example.movieapp.domain.repository.ProfileRepository
import javax.inject.Inject

class GetProfilesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(): Result<List<Profile>> {
        return profileRepository.getProfiles()
    }
}

class CreateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(name: String, avatarId: Int? = null, isKids: Boolean = false): Result<Profile> {
        return profileRepository.createProfile(name, avatarId, isKids)
    }
}

class UpdateProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(id: String, name: String? = null, avatarId: Int? = null, isKids: Boolean? = null): Result<Profile> {
        return profileRepository.updateProfile(id, name, avatarId, isKids)
    }
}

class DeleteProfileUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return profileRepository.deleteProfile(id)
    }
}

class GetFavoritesUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String): Result<List<Movie>> {
        return profileRepository.getFavorites(profileId)
    }
}

class ToggleFavoriteUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String, movieId: String, isCurrentlyFavorite: Boolean): Result<Unit> {
        return if (isCurrentlyFavorite) {
            profileRepository.deleteFavorite(profileId, movieId)
        } else {
            profileRepository.putFavorite(profileId, movieId)
        }
    }
}

class GetWatchHistoryUseCase @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(profileId: String): Result<List<HistoryItem>> {
        val result = profileRepository.getWatchHistory(profileId)
        return result.map { items ->
            // Filter out items where tombstone == true as required by contract
            items.filter { !it.tombstone }
        }
    }
}

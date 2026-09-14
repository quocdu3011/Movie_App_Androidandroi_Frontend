package com.example.movieapp.domain.store

import kotlinx.coroutines.flow.StateFlow

interface CurrentProfileStore {
    val currentProfileId: StateFlow<String?>
    fun setProfileId(profileId: String?)
}

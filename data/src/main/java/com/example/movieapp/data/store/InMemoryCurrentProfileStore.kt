package com.example.movieapp.data.store

import com.example.movieapp.domain.store.CurrentProfileStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryCurrentProfileStore @Inject constructor() : CurrentProfileStore {
    private val _currentProfileId = MutableStateFlow<String?>(null)
    override val currentProfileId: StateFlow<String?> = _currentProfileId.asStateFlow()

    override fun setProfileId(profileId: String?) {
        _currentProfileId.value = profileId
    }
}

package com.example.movieapp.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceIdDataStore: DataStore<Preferences> by preferencesDataStore(name = "device_id_prefs")

@Singleton
class DeviceIdProvider(
    private val dataStore: DataStore<Preferences>
) {
    @Inject
    constructor(context: Context) : this(context.deviceIdDataStore)

    private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

    suspend fun getOrCreate(): String {
        val existingId = dataStore.data.map { preferences ->
            preferences[DEVICE_ID_KEY]
        }.first()

        if (!existingId.isNullOrBlank()) {
            return existingId
        }

        val newDeviceId = UUID.randomUUID().toString()
        dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = newDeviceId
        }
        return newDeviceId
    }
}

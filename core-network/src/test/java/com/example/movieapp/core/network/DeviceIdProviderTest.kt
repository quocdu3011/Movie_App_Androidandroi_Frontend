package com.example.movieapp.core.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class DeviceIdProviderTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var deviceIdProvider: DeviceIdProvider
    private var preferencesStore: MutablePreferences = mutablePreferencesOf()

    @Before
    fun setUp() {
        dataStore = mockk(relaxed = true)
        coEvery { dataStore.data } answers { flowOf(preferencesStore) }
        coEvery { dataStore.edit(any()) } answers {
            val transform = firstArg<suspend (MutablePreferences) -> Unit>()
            transform(preferencesStore)
            preferencesStore
        }
        deviceIdProvider = DeviceIdProvider(dataStore)
    }

    @Test
    fun `test getOrCreate creates and saves new device id if not present`() = runTest {
        val deviceId = deviceIdProvider.getOrCreate()

        assertNotNull(deviceId)
        coVerify(exactly = 1) { dataStore.edit(any()) }

        // Second call should return identical saved device ID without edit
        val cachedDeviceId = deviceIdProvider.getOrCreate()
        assertEquals(deviceId, cachedDeviceId)
    }
}

package com.example.movieapp.feature.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchHistoryDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_history_pref")

interface SearchHistoryStorage {
    fun getHistory(): Flow<List<String>>
    suspend fun addQuery(query: String)
    suspend fun removeQuery(query: String)
    suspend fun clearHistory()
}

@Singleton
class DataStoreSearchHistoryStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : SearchHistoryStorage {

    private val historyKey = stringPreferencesKey("search_history_raw")
    private val delimiter = "|||"

    override fun getHistory(): Flow<List<String>> {
        return context.searchHistoryDataStore.data.map { prefs ->
            val raw = prefs[historyKey] ?: ""
            if (raw.isBlank()) emptyList()
            else raw.split(delimiter).filter { it.isNotBlank() }
        }
    }

    override suspend fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.searchHistoryDataStore.edit { prefs ->
            val raw = prefs[historyKey] ?: ""
            val currentList = if (raw.isBlank()) mutableListOf()
            else raw.split(delimiter).filter { it.isNotBlank() }.toMutableList()

            currentList.remove(trimmed)
            currentList.add(0, trimmed)
            val updatedList = currentList.take(10)
            prefs[historyKey] = updatedList.joinToString(delimiter)
        }
    }

    override suspend fun removeQuery(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val raw = prefs[historyKey] ?: ""
            if (raw.isNotBlank()) {
                val currentList = raw.split(delimiter).filter { it.isNotBlank() }.toMutableList()
                currentList.remove(query)
                prefs[historyKey] = currentList.joinToString(delimiter)
            }
        }
    }

    override suspend fun clearHistory() {
        context.searchHistoryDataStore.edit { prefs ->
            prefs.remove(historyKey)
        }
    }
}

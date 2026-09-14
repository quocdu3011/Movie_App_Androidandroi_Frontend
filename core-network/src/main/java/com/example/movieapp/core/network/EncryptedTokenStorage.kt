package com.example.movieapp.core.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedTokenStorage(
    private val sharedPreferences: SharedPreferences
) : TokenStorage {

    @Inject
    constructor(@ApplicationContext context: Context) : this(createEncryptedPreferences(context))

    @Volatile
    private var inMemoryAccessToken: String? = null

    override fun getAccessToken(): String? = inMemoryAccessToken

    override fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    override fun saveTokens(accessToken: String, refreshToken: String) {
        this.inMemoryAccessToken = accessToken
        sharedPreferences.edit()
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    override fun clear() {
        this.inMemoryAccessToken = null
        sharedPreferences.edit()
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "encrypted_token_prefs"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        private fun createEncryptedPreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}

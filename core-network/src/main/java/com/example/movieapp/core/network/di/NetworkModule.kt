package com.example.movieapp.core.network.di

import android.content.Context
import com.example.movieapp.core.network.NetworkConfig
import com.example.movieapp.core.network.DeviceIdProvider
import com.example.movieapp.core.network.EncryptedTokenStorage
import com.example.movieapp.core.network.SharedCookieJarProvider
import com.example.movieapp.core.network.TokenStorage
import com.example.movieapp.core.network.api.RefreshTokenApi
import com.example.movieapp.core.network.interceptor.AuthInterceptor
import com.example.movieapp.core.network.interceptor.TokenAuthenticator
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context
    ): TokenStorage = EncryptedTokenStorage(context)

    @Provides
    @Singleton
    fun provideSharedCookieJarProvider(): SharedCookieJarProvider = SharedCookieJarProvider()

    @Provides
    @Singleton
    @RefreshClient
    fun provideRefreshOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRefreshTokenApi(
        @RefreshClient okHttpClient: OkHttpClient,
        json: Json
    ): RefreshTokenApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(NetworkConfig.activeBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(RefreshTokenApi::class.java)
    }

    @Provides
    @Singleton
    @BackendClient
    fun provideBackendOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        cookieJarProvider: SharedCookieJarProvider
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .cookieJar(cookieJarProvider.cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @MediaClient
    fun provideMediaOkHttpClient(
        cookieJarProvider: SharedCookieJarProvider
    ): OkHttpClient {
        // MediaHttpClient MUST NOT have AuthInterceptor or TokenAuthenticator
        // BUT MUST share the exact same CookieJar as BackendApiClient
        return OkHttpClient.Builder()
            .cookieJar(cookieJarProvider.cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

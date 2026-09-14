package com.example.movieapp.data.di

import com.example.movieapp.core.network.di.BackendClient
import com.example.movieapp.data.remote.api.AuthApi
import com.example.movieapp.data.remote.api.CatalogApi
import com.example.movieapp.data.remote.api.ProfileApi
import com.example.movieapp.data.remote.api.StreamingApi
import com.example.movieapp.data.remote.api.SubscriptionApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataNetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(
        @BackendClient okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)

    @Provides
    @Singleton
    fun provideSubscriptionApi(retrofit: Retrofit): SubscriptionApi = retrofit.create(SubscriptionApi::class.java)

    @Provides
    @Singleton
    fun provideStreamingApi(retrofit: Retrofit): StreamingApi = retrofit.create(StreamingApi::class.java)
}

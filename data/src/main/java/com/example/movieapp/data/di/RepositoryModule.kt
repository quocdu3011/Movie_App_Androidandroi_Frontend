package com.example.movieapp.data.di

import com.example.movieapp.data.repository.AuthRepositoryImpl
import com.example.movieapp.data.repository.CatalogRepositoryImpl
import com.example.movieapp.data.repository.ProfileRepositoryImpl
import com.example.movieapp.data.repository.StreamingRepositoryImpl
import com.example.movieapp.data.repository.SubscriptionRepositoryImpl
import com.example.movieapp.domain.repository.AuthRepository
import com.example.movieapp.domain.repository.CatalogRepository
import com.example.movieapp.domain.repository.ProfileRepository
import com.example.movieapp.domain.repository.StreamingRepository
import com.example.movieapp.domain.repository.SubscriptionRepository
import com.example.movieapp.data.store.InMemoryCurrentProfileStore
import com.example.movieapp.domain.store.CurrentProfileStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindCatalogRepository(impl: CatalogRepositoryImpl): CatalogRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindStreamingRepository(impl: StreamingRepositoryImpl): StreamingRepository

    @Binds
    @Singleton
    abstract fun bindCurrentProfileStore(impl: InMemoryCurrentProfileStore): CurrentProfileStore
}

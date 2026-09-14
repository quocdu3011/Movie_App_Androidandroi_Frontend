package com.example.movieapp.feature.search.di

import com.example.movieapp.feature.search.DataStoreSearchHistoryStorage
import com.example.movieapp.feature.search.SearchHistoryStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {

    @Binds
    @Singleton
    abstract fun bindSearchHistoryStorage(
        impl: DataStoreSearchHistoryStorage
    ): SearchHistoryStorage
}

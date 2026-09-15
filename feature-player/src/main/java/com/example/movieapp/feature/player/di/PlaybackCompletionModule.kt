package com.example.movieapp.feature.player.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PlaybackCompletionScope

@Module
@InstallIn(SingletonComponent::class)
object PlaybackCompletionModule {
    @Provides
    @Singleton
    @PlaybackCompletionScope
    fun providePlaybackCompletionScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

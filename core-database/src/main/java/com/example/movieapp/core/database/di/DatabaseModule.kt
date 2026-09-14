package com.example.movieapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.example.movieapp.core.database.AppDatabase
import com.example.movieapp.core.database.dao.CachedMovieDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "movie_app_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideCachedMovieDao(
        database: AppDatabase
    ): CachedMovieDao {
        return database.cachedMovieDao()
    }
}

package com.example.movieapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.movieapp.core.database.dao.CachedMovieDao
import com.example.movieapp.core.database.entity.CachedMovieEntity

@Database(
    entities = [CachedMovieEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedMovieDao(): CachedMovieDao
}

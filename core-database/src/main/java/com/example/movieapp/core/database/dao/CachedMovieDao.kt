package com.example.movieapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.movieapp.core.database.entity.CachedMovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(movies: List<CachedMovieEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(movie: CachedMovieEntity)

    @Query("SELECT * FROM cached_movies ORDER BY cachedAt DESC")
    fun getAll(): Flow<List<CachedMovieEntity>>

    @Query("DELETE FROM cached_movies WHERE cachedAt < :thresholdTimestamp")
    suspend fun deleteOlderThan(thresholdTimestamp: Long)

    @Query("DELETE FROM cached_movies")
    suspend fun clearAll()
}

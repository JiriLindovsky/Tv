package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TvAppDao {
    @Query("SELECT * FROM tv_apps ORDER BY isFavorite DESC, clicks DESC, timestamp DESC")
    fun getAllApps(): Flow<List<TvApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: TvApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<TvApp>)

    @Query("UPDATE tv_apps SET clicks = clicks + 1 WHERE id = :id")
    suspend fun incrementClicks(id: Int)

    @Query("UPDATE tv_apps SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM tv_apps WHERE id = :id")
    suspend fun deleteAppById(id: Int)

    @Query("SELECT COUNT(*) FROM tv_apps")
    suspend fun getCount(): Int
}

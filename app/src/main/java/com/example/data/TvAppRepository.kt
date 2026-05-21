package com.example.data

import kotlinx.coroutines.flow.Flow

class TvAppRepository(private val tvAppDao: TvAppDao) {
    val allApps: Flow<List<TvApp>> = tvAppDao.getAllApps()

    suspend fun insert(app: TvApp) = tvAppDao.insertApp(app)

    suspend fun insertAll(apps: List<TvApp>) = tvAppDao.insertApps(apps)

    suspend fun incrementClicks(id: Int) = tvAppDao.incrementClicks(id)

    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) = tvAppDao.toggleFavorite(id, isFavorite)

    suspend fun deleteById(id: Int) = tvAppDao.deleteAppById(id)

    suspend fun getCount(): Int = tvAppDao.getCount()
}

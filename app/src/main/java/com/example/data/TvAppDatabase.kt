package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TvApp::class], version = 1, exportSchema = false)
abstract class TvAppDatabase : RoomDatabase() {
    abstract fun tvAppDao(): TvAppDao

    companion object {
        @Volatile
        private var INSTANCE: TvAppDatabase? = null

        fun getDatabase(context: Context): TvAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TvAppDatabase::class.java,
                    "tv_app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

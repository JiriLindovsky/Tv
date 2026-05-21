package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_apps")
data class TvApp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val packageName: String,
    val category: String, // "Video", "Audio", "Games", "Utility"
    val description: String,
    val rating: Float = 4.5f,
    val isFavorite: Boolean = false,
    val clicks: Int = 0,
    val isCustom: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

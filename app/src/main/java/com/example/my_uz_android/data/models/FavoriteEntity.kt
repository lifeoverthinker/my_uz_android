package com.example.my_uz_android.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Zapisane do ulubionych (sala, prowadzący, grupa)
@Serializable
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val type: String,
    @ColumnInfo(name = "resource_id")
    val resourceId: String
)
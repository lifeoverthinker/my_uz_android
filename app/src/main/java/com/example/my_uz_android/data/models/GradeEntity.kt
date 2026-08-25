package com.example.my_uz_android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Ocena z przedmiotu
@Serializable
@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectName: String,
    val classType: String = "",
    val grade: Double,
    val weight: Int = 1,
    val description: String? = null,
    val comment: String? = null,
    val date: Long = System.currentTimeMillis(),
    val semester: Int = 1,
    val isPoints: Boolean = false
)
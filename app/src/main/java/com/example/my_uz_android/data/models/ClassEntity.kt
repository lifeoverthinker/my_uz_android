package com.example.my_uz_android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Pojedyncze zajęcia w planie
@Serializable
@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val supabaseId: String? = null,
    val subjectName: String,
    val classType: String,
    val startTime: String,
    val endTime: String,
    val dayOfWeek: Int,
    val date: String,
    val groupCode: String,
    val subgroup: String?,
    val teacherName: String? = null,
    val teacherEmail: String? = null,
    val teacherInstitute: String? = null,
    val room: String? = null,
    val colorHex: String? = "#3D84FF"
)
package com.example.my_uz_android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Dodatkowy kierunek lub grupa
@Serializable
@Entity(tableName = "user_courses")
data class UserCourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val groupCode: String,
    val fieldOfStudy: String? = null,
    val selectedSubgroup: String? = null,
    val colorHex: String? = null,
    val faculty: String? = null,
    val studyMode: String? = null,
    val semester: Int? = null
)
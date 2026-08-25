package com.example.my_uz_android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "absences")
data class AbsenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subjectName: String,
    val date: Long,
    val classType: String?,
    val isExcused: Boolean = false,
    val description: String? = null
)
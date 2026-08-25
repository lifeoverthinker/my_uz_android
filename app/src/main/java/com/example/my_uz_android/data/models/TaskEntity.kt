package com.example.my_uz_android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// Zadanie na liście to-do studenta
@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val subjectName: String? = null,
    val classType: String? = null,
    val priority: Int = 1,
    val isAllDay: Boolean = false,
    val dueDate: Long = System.currentTimeMillis(),
    val dueTime: String? = null,
    val endDate: Long = System.currentTimeMillis(),
    val color: Int = 0xFF68548E.toInt(),
    val isCompleted: Boolean = false,
    val subjectId: Int? = null,
    val hasReminder: Boolean = false,
    val reminderTime: Long? = null
) {
    val classSubject: String? get() = subjectName
}
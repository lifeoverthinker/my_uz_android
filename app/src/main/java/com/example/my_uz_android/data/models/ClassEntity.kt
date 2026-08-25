package com.example.my_uz_android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Encja bazodanowa reprezentująca pojedyncze zajęcia w planie studenta.
 *
 * Klasa stanowi główne źródło prawdy (Single Source of Truth) o harmonogramie w lokalnej
 * bazie danych SQLite oraz umożliwia synchronizację z zewnętrznym backendem dzięki serializacji.
 * Niejawnie pełni funkcję nadrzędną dla encji ocen i zadań, wykorzystując do mapowania
 * nazwę przedmiotu ([subjectName]) oraz typ zajęć ([classType]).
 * * Uwaga architektoniczna: Należy dbać o spójność wielkości liter oraz białych znaków
 * w polu [subjectName], aby zachować integralność logicznych relacji z ocenami i zadaniami.
 */
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


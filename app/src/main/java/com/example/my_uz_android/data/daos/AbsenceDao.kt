package com.example.my_uz_android.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.my_uz_android.data.models.AbsenceEntity
import kotlinx.coroutines.flow.Flow

// Operacje na nieobecnościach
@Dao
interface AbsenceDao {
    @Query("SELECT * FROM absences ORDER BY date DESC")
    fun getAllAbsences(): Flow<List<AbsenceEntity>>

    @Query("SELECT * FROM absences WHERE id = :id")
    fun getAbsenceById(id: Int): Flow<AbsenceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: AbsenceEntity)

    @Delete
    suspend fun deleteAbsence(absence: AbsenceEntity)

    @Query("DELETE FROM absences")
    suspend fun deleteAll()
}
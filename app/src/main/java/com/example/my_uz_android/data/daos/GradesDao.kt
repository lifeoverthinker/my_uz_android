package com.example.my_uz_android.data.daos

import androidx.room.*
import com.example.my_uz_android.data.models.GradeEntity
import kotlinx.coroutines.flow.Flow

// Operacje na ocenach studenta
@Dao
interface GradesDao {

    @Query("SELECT * FROM grades ORDER BY date DESC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE subjectName = :subjectName ORDER BY date DESC")
    fun getGradesForSubject(subjectName: String): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE id = :id")
    fun getGradeById(id: Int): Flow<GradeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity)

    @Update
    suspend fun updateGrade(grade: GradeEntity)

    @Delete
    suspend fun deleteGrade(grade: GradeEntity)

    @Query("DELETE FROM grades")
    suspend fun deleteAll()
}
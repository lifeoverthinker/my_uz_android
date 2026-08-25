package com.example.my_uz_android.data.repositories

import com.example.my_uz_android.data.daos.GradesDao
import com.example.my_uz_android.data.models.GradeEntity
import kotlinx.coroutines.flow.Flow

// Zarządzanie ocenami i średnią
class GradesRepository(private val gradesDao: GradesDao) {

    fun getAllGradesStream(): Flow<List<GradeEntity>> = gradesDao.getAllGrades()

    fun getGradesForSubjectStream(subject: String): Flow<List<GradeEntity>> =
        gradesDao.getGradesForSubject(subject)

    fun getGradeByIdStream(id: Int): Flow<GradeEntity?> = gradesDao.getGradeById(id)

    suspend fun insertGrade(grade: GradeEntity) = gradesDao.insertGrade(grade)

    suspend fun insertGrades(grades: List<GradeEntity>) {
        grades.forEach { gradesDao.insertGrade(it) }
    }

    suspend fun updateGrade(grade: GradeEntity) = gradesDao.updateGrade(grade)

    suspend fun deleteGrade(grade: GradeEntity) = gradesDao.deleteGrade(grade)

    suspend fun deleteAllGrades() = gradesDao.deleteAll()
}
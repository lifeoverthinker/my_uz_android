package com.example.my_uz_android.data.repositories

import com.example.my_uz_android.data.daos.AbsenceDao
import com.example.my_uz_android.data.models.AbsenceEntity
import kotlinx.coroutines.flow.Flow

class AbsenceRepository(private val absenceDao: AbsenceDao) {
    fun getAllAbsencesStream(): Flow<List<AbsenceEntity>> = absenceDao.getAllAbsences()

    fun getAbsence(id: Int): Flow<AbsenceEntity?> = absenceDao.getAbsenceById(id)

    suspend fun insertAbsence(absence: AbsenceEntity) = absenceDao.insertAbsence(absence)

    suspend fun insertAbsences(absences: List<AbsenceEntity>) {
        absences.forEach { absenceDao.insertAbsence(it) }
    }

    suspend fun deleteAbsence(absence: AbsenceEntity) = absenceDao.deleteAbsence(absence)

    suspend fun deleteAllAbsences() = absenceDao.deleteAll()
}
package com.example.my_uz_android.data.repositories

import com.example.my_uz_android.data.daos.SettingsDao
import com.example.my_uz_android.data.models.SettingsEntity
import kotlinx.coroutines.flow.Flow

// Zarządzanie ustawieniami i danymi sesji studenta
class SettingsRepository(private val settingsDao: SettingsDao) {

    fun getSettingsStream(): Flow<SettingsEntity?> = settingsDao.getSettingsStream()

    suspend fun getSettingsOnce(): SettingsEntity? = settingsDao.getSettingsOnce()

    suspend fun insertSettings(settings: SettingsEntity) = settingsDao.insertOrUpdate(settings)

    suspend fun clearSettings() = settingsDao.clearAll()
}
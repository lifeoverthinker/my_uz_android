package com.example.my_uz_android.data.daos

import androidx.room.*
import com.example.my_uz_android.data.models.SettingsEntity
import kotlinx.coroutines.flow.Flow

// Ustawienia aplikacji (zawsze 1 wiersz w bazie)
@Dao
interface SettingsDao {

    @Query("SELECT * FROM settings LIMIT 1")
    fun getSettingsStream(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: SettingsEntity)

    @Query("DELETE FROM settings")
    suspend fun clearAll()
}
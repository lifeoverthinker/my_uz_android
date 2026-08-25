package com.example.my_uz_android.data.daos

import androidx.room.*
import com.example.my_uz_android.data.models.EventEntity
import kotlinx.coroutines.flow.Flow

// Operacje na wydarzeniach i kalendarzu
@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Int): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventByIdSuspend(id: Int): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
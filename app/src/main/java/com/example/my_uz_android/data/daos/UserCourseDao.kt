package com.example.my_uz_android.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.my_uz_android.data.models.UserCourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCourseDao {
    // Pobiera wszystkie kierunki, Flow automatycznie odświeży listę w UI
    @Query("SELECT * FROM user_courses ORDER BY id ASC")
    fun getAllUserCoursesStream(): Flow<List<UserCourseEntity>>

    // Dodaje nowy kierunek (np. po wpisaniu kodu grupy)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCourse(userCourse: UserCourseEntity)

    // Aktualizuje np. wybrany kolor lub podgrupę
    @Update
    suspend fun updateUserCourse(userCourse: UserCourseEntity)

    // Usuwa kierunek z listy
    @Delete
    suspend fun deleteUserCourse(userCourse: UserCourseEntity)

    // Sprawdza, czy już mamy ten kierunek w bazie
    @Query("SELECT * FROM user_courses WHERE groupCode = :groupCode LIMIT 1")
    suspend fun getUserCourseByCode(groupCode: String): UserCourseEntity?
    @Query("DELETE FROM user_courses")
    suspend fun deleteAll()
}
package com.example.my_uz_android.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.my_uz_android.data.models.UserCourseEntity
import kotlinx.coroutines.flow.Flow

// Obsługa dodatkowych kierunków i grup studenta
@Dao
interface UserCourseDao {
    @Query("SELECT * FROM user_courses ORDER BY id ASC")
    fun getAllUserCoursesStream(): Flow<List<UserCourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCourse(userCourse: UserCourseEntity)

    @Update
    suspend fun updateUserCourse(userCourse: UserCourseEntity)

    @Delete
    suspend fun deleteUserCourse(userCourse: UserCourseEntity)

    @Query("SELECT * FROM user_courses WHERE groupCode = :groupCode LIMIT 1")
    suspend fun getUserCourseByCode(groupCode: String): UserCourseEntity?

    @Query("DELETE FROM user_courses")
    suspend fun deleteAll()
}
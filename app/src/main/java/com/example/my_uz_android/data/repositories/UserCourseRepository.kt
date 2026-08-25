package com.example.my_uz_android.data.repositories

import com.example.my_uz_android.data.daos.UserCourseDao
import com.example.my_uz_android.data.models.UserCourseEntity
import kotlinx.coroutines.flow.Flow

class UserCourseRepository(private val userCourseDao: UserCourseDao) {

    fun getAllUserCoursesStream(): Flow<List<UserCourseEntity>> =
        userCourseDao.getAllUserCoursesStream()

    suspend fun insertUserCourse(userCourse: UserCourseEntity) =
        userCourseDao.insertUserCourse(userCourse)

    suspend fun updateUserCourse(userCourse: UserCourseEntity) =
        userCourseDao.updateUserCourse(userCourse)

    suspend fun deleteUserCourse(userCourse: UserCourseEntity) =
        userCourseDao.deleteUserCourse(userCourse)

    suspend fun getUserCourseByCode(groupCode: String): UserCourseEntity? =
        userCourseDao.getUserCourseByCode(groupCode)
    suspend fun deleteAll() = userCourseDao.deleteAll()
}
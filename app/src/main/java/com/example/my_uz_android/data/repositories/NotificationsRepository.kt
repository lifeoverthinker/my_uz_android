package com.example.my_uz_android.data.repositories

import com.example.my_uz_android.data.daos.NotificationDao
import com.example.my_uz_android.data.models.NotificationEntity

// Repozytorium powiadomień i licznika nieprzeczytanych
class NotificationsRepository(private val notificationDao: NotificationDao) {

    fun getAllNotifications() = notificationDao.getAllNotifications()

    fun getUnreadCount() = notificationDao.getUnreadCount()

    suspend fun insertNotification(notification: NotificationEntity) =
        notificationDao.insertNotification(notification)

    suspend fun markAllAsRead() = notificationDao.markAllAsRead()

    suspend fun deleteNotification(notification: NotificationEntity) =
        notificationDao.deleteNotification(notification)

    suspend fun clearAll() = notificationDao.clearAll()
}
package com.example.my_uz_android.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.NotificationEntity
import com.example.my_uz_android.data.repositories.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ViewModel do obsługi listy powiadomień
class NotificationsViewModel(private val notificationsRepository: NotificationsRepository) : ViewModel() {

    val notifications: Flow<List<NotificationEntity>> = notificationsRepository.getAllNotifications()

    // Liczba nieprzeczytanych powiadomień (do czerwonej kropki/badge)
    val unreadCount: Flow<Int> = notificationsRepository.getUnreadCount()

    // Oznacza wszystkie jako przeczytane po wejściu na ekran
    fun markAllAsRead() {
        viewModelScope.launch {
            notificationsRepository.markAllAsRead()
        }
    }

    // Usuwa pojedyncze powiadomienie (np. po swipe w lewo)
    fun deleteNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            notificationsRepository.deleteNotification(notification)
        }
    }

    // Czyści całą listę powiadomień
    fun clearAll() {
        viewModelScope.launch {
            notificationsRepository.clearAll()
        }
    }
}
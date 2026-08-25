package com.example.my_uz_android.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode(val displayName: String) {
    LIGHT("Jasny"),
    DARK("Ciemny"),
    SYSTEM("Systemowy")
}

// Główne ustawienia profilu i aplikacji (pojedynczy rekord o stałym id = 1)
@Serializable
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isAnonymous: Boolean = false,
    val userName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val gender: String? = null,
    val selectedGroupCode: String? = null,
    val selectedGroupName: String? = null,
    val activeDirectionCode: String? = null,
    val selectedSubgroup: String? = null,
    val faculty: String? = null,
    val department: String? = null,
    val fieldOfStudy: String? = null,
    val studyMode: String? = null,
    val currentSemester: Int = 1,
    val additionalGroupCodes: String = "",
    val activeIndexDirectionCode: String? = null,
    val isFirstRun: Boolean = true,
    val isDarkMode: Boolean = false,
    val themeMode: String = ThemeMode.SYSTEM.name,
    val offlineModeEnabled: Boolean = false,
    val classColorsJson: String = "{}",

    val notificationsEnabled: Boolean = true,
    val notificationsClasses: Boolean = true,
    val notificationClassTimeBefore: Int = 15,

    val autoSyncEnabled: Boolean = true,
    val syncIntervalHours: Int = 24,

    val appLanguage: String = "pl"
)
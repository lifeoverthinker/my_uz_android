package com.example.my_uz_android.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.my_uz_android.data.db.AppDatabase
import com.example.my_uz_android.data.models.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AppAlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ALARM_TEST", "1. ODBIORNIK OBUDZONY! Akcja: ${intent.action}")

        val pendingResult = goAsync() // Pozwala BroadcastReceiverowi na wykonanie zadań asynchronicznych (coroutines)

        val action = intent.action ?: run { pendingResult.finish(); return }

        // Po restarcie telefonu przywracamy alarmy na nowo
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                try {
                    val database = AppDatabase.getDatabase(context)
                    val classes = database.classDao().getAllClasses().firstOrNull().orEmpty()
                    NotificationHelper.scheduleClassAlarms(context, classes)
                } catch (e: Exception) {
                    Log.e("ALARM_TEST", "Błąd przywracania alarmów po restarcie", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val id = intent.getIntExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "MyUZ"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""

        val isTask = action == ACTION_TASK_ALARM
        val type = if (isTask) "task_reminder" else "class_reminder"

        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)

                // 1. POBRANIE NAJNOWSZYCH USTAWIEŃ Z BAZY DANYCH
                val settings = database.settingsDao().getSettingsStream().firstOrNull()

                val isMasterEnabled = settings?.notificationsEnabled ?: true
                val isClassesEnabled = settings?.notificationsClasses ?: true

                // 2. WERYFIKACJA PRZEŁĄCZNIKÓW
                if (!isMasterEnabled) {
                    Log.d("ALARM_TEST", "Powiadomienia całkowicie wyłączone. Przerywam.")
                    return@launch
                }

                if (!isTask && !isClassesEnabled) {
                    Log.d("ALARM_TEST", "Powiadomienia o zajęciach (15 minut przed) wyłączone. Przerywam.")
                    return@launch
                }

                // 3. WYŚWIETLENIE POWIADOMIENIA (Jeśli przeszło weryfikację)
                NotificationHelper.showNotification(
                    context = context,
                    title = title,
                    message = message,
                    isTask = isTask,
                    notificationId = id
                )
                Log.d("ALARM_TEST", "Wywołano showNotification pomyślnie!")

                // 4. ZAPIS DO HISTORII POWIADOMIEŃ APLIKACJI
                val notification = NotificationEntity(
                    id = 0,
                    title = title,
                    message = message,
                    type = type,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
                database.notificationDao().insertNotification(notification)
                Log.d("ALARM_TEST", "Zapisano do bazy danych pomyślnie!")

            } catch (e: Exception) {
                Log.e("ALARM_TEST", "BŁĄD przy weryfikacji/wyświetlaniu powiadomienia!", e)
            } finally {
                // Informuje system, że zakończyliśmy działanie w tle
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TASK_ALARM = "com.example.my_uz_android.ACTION_TASK_ALARM"
        const val ACTION_CLASS_ALARM = "com.example.my_uz_android.ACTION_CLASS_ALARM"

        const val EXTRA_ID = "EXTRA_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
    }
}
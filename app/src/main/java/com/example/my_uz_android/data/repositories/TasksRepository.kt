package com.example.my_uz_android.data.repositories

import com.example.my_uz_android.data.daos.TasksDao
import com.example.my_uz_android.data.models.SharedTaskRequest
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.util.NetworkResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Repozytorium zadań studenta + obsługa chmury Supabase do dzielenia się listą zadań
class TasksRepository(
    private val tasksDao: TasksDao,
    private val supabase: SupabaseClient
) {
    fun getAllTasks(): Flow<List<TaskEntity>> = tasksDao.getAllTasks()

    fun getTaskById(id: Int): Flow<TaskEntity?> = tasksDao.getTaskById(id)

    suspend fun insertTask(task: TaskEntity) = tasksDao.insert(task)

    suspend fun insertTasks(tasks: List<TaskEntity>) {
        tasksDao.insertAll(tasks)
    }

    suspend fun updateTask(task: TaskEntity) = tasksDao.update(task)

    suspend fun deleteTask(task: TaskEntity) = tasksDao.delete(task)

    suspend fun deleteAllTasks() = tasksDao.deleteAll()

    // Udostępnianie listy zadań kodem 6-znakowym
    suspend fun shareTasks(tasks: List<TaskEntity>): NetworkResult<String> {
        return try {
            val payloadJson = Json.encodeToString(tasks)
            val code = (1..6)
                .map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }
                .joinToString("")

            val request = SharedTaskRequest(shareId = code, payload = payloadJson)
            supabase.postgrest["shared_tasks"].insert(request)
            NetworkResult.Success(code)
        } catch (e: Exception) {
            NetworkResult.Error("Błąd udostępniania: ${e.localizedMessage}")
        }
    }

    // Pobieranie i import zadań ze wspólnego kodu
    suspend fun importTasks(code: String): NetworkResult<List<TaskEntity>> {
        return try {
            val result = supabase.postgrest["shared_tasks"]
                .select { filter { eq("share_id", code.trim().uppercase()) } }
                .decodeSingleOrNull<SharedTaskRequest>()

            if (result != null) {
                val tasks: List<TaskEntity> = Json.decodeFromString(result.payload)
                val tasksToInsert = tasks.map { it.copy(id = 0) }
                tasksDao.insertAll(tasksToInsert)
                NetworkResult.Success(tasksToInsert)
            } else {
                NetworkResult.Error("Nie znaleziono zadań dla podanego kodu: $code")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Błąd importu zadań: ${e.localizedMessage}")
        }
    }
}
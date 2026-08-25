package com.example.my_uz_android.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.my_uz_android.data.models.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interfejs dostępu do danych (Data Access Object) dla encji zadań.
 *
 * Stanowi abstrakcję nad lokalną bazą danych SQLite. Definiuje zestaw operacji CRUD
 * niezbędnych do zarządzania zadaniami studenta. Zastosowanie typu [Flow] pozwala
 * na utworzenie reaktywnego strumienia danych, który automatycznie emituje nowe wartości
 * przy każdej zmianie w tabeli, gwarantując zawsze aktualny stan interfejsu użytkownika.
 */
@Dao
interface TasksDao {

    /**
     * Pobiera reaktywny strumień wszystkich zadań.
     */
    @Query("SELECT * FROM tasks ORDER BY dueDate DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    /**
     * Pobiera reaktywny strumień szczegółów konkretnego zadania na podstawie jego identyfikatora.
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Int): Flow<TaskEntity?>

    /**
     * Zwraca dane pojedynczego zadania w sposób jednorazowy (bez ciągłej subskrypcji).
     * Wykorzystywane do asynchronicznych operacji w tle, np. walidacji danych
     * lub logiki synchronizacyjnej w Workerach, gdzie subskrypcja na [Flow] jest zbędna.
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskByIdSuspend(id: Int): TaskEntity?

    /**
     * Pobiera strumień zadań oczekujących na wykonanie, posortowanych rosnąco według najbliższego terminu.
     * Służy do zasilania widoków priorytetowych, pomagając studentowi w planowaniu najbliższych obowiązków.
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY dueDate ASC")
    fun getIncompleteTasks(): Flow<List<TaskEntity>>

    /**
     * Zapisuje nowe zadanie w bazie.
     * W przypadku konfliktu (np. zadanie o tym samym kluczu głównym już istnieje),
     * starszy wpis zostaje nadpisany ([OnConflictStrategy.REPLACE]), co ułatwia mechanizm upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    /**
     * Trwale usuwa wszystkie rekordy z tabeli zadań.
     * Metoda wykorzystywana zazwyczaj przy wylogowywaniu użytkownika lub czyszczeniu pamięci podręcznej.
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}




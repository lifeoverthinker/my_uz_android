package com.example.my_uz_android.ui.screens.calendar.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.data.repositories.TasksRepository
import com.example.my_uz_android.util.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Komponent warstwy prezentacji (ViewModel) zarządzający stanem i logiką ekranu zadań.
 *
 * Stanowi pomost pomiędzy interfejsem użytkownika a warstwą danych ([TasksRepository]).
 * Hermetyzuje asynchroniczne operacje na zadaniach (CRUD, eksport, import) i udostępnia
 * reaktywne strumienie stanu ([StateFlow]), na które widok może bezpiecznie reagować.
 */
class TasksViewModel(
    private val tasksRepository: TasksRepository
) : ViewModel() {

    /** Strumień synchronizujący na żywo listę wszystkich zadań pobranych z bazy danych. */
    val tasksStream: Flow<List<TaskEntity>> = tasksRepository.getAllTasks()

    private val _sharedCode = MutableStateFlow<String?>(null)
    val sharedCode: StateFlow<String?> = _sharedCode.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _shareError = MutableStateFlow<String?>(null)
    val shareError: StateFlow<String?> = _shareError.asStateFlow()

    private val _importStatus = MutableStateFlow<String?>(null)
    val importStatus: StateFlow<String?> = _importStatus.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    /**
     * Odwraca bieżący status ukończenia zadania i deleguje aktualizację do repozytorium.
     * Zapewnia natychmiastową odpowiedź interfejsu na interakcje użytkownika z listą.
     */
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            tasksRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    /**
     * Inicjuje proces trwałego usunięcia zadanego elementu z lokalnej bazy danych.
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            tasksRepository.deleteTask(task)
        }
    }

    /**
     * Przesyła wybrane zadania do zewnętrznego serwera w celu wygenerowania kodu współdzielenia.
     * * Wykorzystuje [NetworkResult] do bezpiecznego zarządzania stanem operacji sieciowej,
     * informując interfejs użytkownika o ładowaniu, sukcesie (zwrot kodu) lub błędzie.
     */
    fun shareMyTasks(selectedTaskIds: Set<Int>? = null) {
        viewModelScope.launch {
            _isSharing.value = true
            _shareError.value = null
            try {
                val allTasks = tasksStream.first()

                val tasksToShare = if (!selectedTaskIds.isNullOrEmpty()) {
                    allTasks.filter { selectedTaskIds.contains(it.id) }
                } else {
                    allTasks
                }

                if (tasksToShare.isNotEmpty()) {
                    when (val result = tasksRepository.shareTasks(tasksToShare)) {
                        is NetworkResult.Success -> _sharedCode.value = result.data
                        is NetworkResult.Error -> _shareError.value = result.message
                    }
                } else {
                    _shareError.value = "Brak zadań do udostępnienia."
                }
            } catch (e: Exception) {
                _shareError.value = "Błąd: ${e.message}"
            } finally {
                _isSharing.value = false
            }
        }
    }

    /**
     * Przetwarza wejściowy kod i komunikuje się z backendem w celu pobrania udostępnionych zadań.
     * Pobrane zadania są automatycznie zapisywane do lokalnej bazy poprzez repozytorium.
     */
    fun importTasks(code: String) {
        if (code.isBlank()) return

        viewModelScope.launch {
            _isImporting.value = true
            _importStatus.value = null
            try {
                when (val result = tasksRepository.importTasks(code.trim().uppercase())) {
                    is NetworkResult.Success -> {
                        val count = result.data?.size ?: 0
                        _importStatus.value = "Pomyślnie zaimportowano $count zadań!"
                    }
                    is NetworkResult.Error -> {
                        _importStatus.value = result.message
                    }
                }
            } catch (e: Exception) {
                _importStatus.value = "Błąd krytyczny: ${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun clearSharedCode() {
        _sharedCode.value = null
    }

    fun clearShareError() {
        _shareError.value = null
    }

    fun clearImportStatus() {
        _importStatus.value = null
    }

    /**
     * Resetuje wszystkie komunikaty o błędach.
     * Funkcja zachowana dla kompatybilności wstecznej starszego kodu UI.
     */
    fun clearError() {
        clearShareError()
        clearImportStatus()
    }
}
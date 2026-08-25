package com.example.my_uz_android.ui.screens.calendar.tasks

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.TasksRepository
import com.example.my_uz_android.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Stan ekranu dodawania/edycji zadania.
 *
 * Przechowuje:
 * - dane formularza,
 * - ustawienia przypomnienia,
 * - listę dostępnych przedmiotów i typów zajęć.
 */
data class TaskAddEditUiState(
    val taskId: Int = 0,
    val title: String = "",
    val classSubject: String? = null,
    val classType: String? = null,
    val description: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(8, 0),
    val endDate: LocalDate = LocalDate.now(),
    val endTime: LocalTime = LocalTime.of(10, 0),
    val isAllDay: Boolean = false,
    val priority: Int = 1,
    val isSaved: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderDate: LocalDate = LocalDate.now(),
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val availableSubjects: List<Pair<String, List<String>>> = emptyList(),
    val isCompleted: Boolean = false
)

/**
 * ViewModel odpowiedzialny za logikę formularza zadania.
 *
 * Funkcje:
 * - ładowanie danych do edycji,
 * - walidacja i zapis zadania,
 * - konfiguracja alarmów przypomnień.
 */
class TaskAddEditViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
    private val tasksRepository: TasksRepository,
    private val classRepository: ClassRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TaskAddEditUiState())
    val uiState: StateFlow<TaskAddEditUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private var loadedTaskId: Int? = null

    init {
        loadAvailableSubjects()

        val taskId = savedStateHandle.get<Int>("taskId")
        if (taskId != null && taskId != -1 && taskId != 0) {
            loadTask(taskId)
        } else {
            val initialDate = savedStateHandle.get<Long>("dueDate")
                ?: savedStateHandle.get<Long>("date")

            _uiState.update {
                it.copy(
                    title = savedStateHandle.get<String>("title") ?: "",
                    description = savedStateHandle.get<String>("desc")
                        ?: savedStateHandle.get<String>("description")
                        ?: "",
                    classSubject = savedStateHandle.get<String>("subject"),
                    classType = savedStateHandle.get<String>("type")
                        ?: savedStateHandle.get<String>("classType"),
                    isAllDay = savedStateHandle.get<Boolean>("isAllDay") ?: false,
                    startDate = initialDate
                        ?.takeIf { it > 0L }
                        ?.let { dateMillis ->
                            Instant.ofEpochMilli(dateMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        } ?: LocalDate.now()
                )
            }
        }
    }

    /**
     * Ładuje zadanie do edycji (jednorazowo).
     */
    fun loadTask(taskId: Int) {
        if (loadedTaskId == taskId) return
        loadedTaskId = taskId

        viewModelScope.launch {
            val task = tasksRepository.getTaskById(taskId).firstOrNull() ?: return@launch

            val startZone = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault())
            val endZone = Instant.ofEpochMilli(task.endDate).atZone(ZoneId.systemDefault())

            val reminderInfo = task.reminderTime?.let {
                val reminderZone = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault())
                reminderZone.toLocalDate() to reminderZone.toLocalTime()
            }

            _uiState.update {
                it.copy(
                    taskId = task.id,
                    title = task.title,
                    description = task.description ?: "",
                    classSubject = task.subjectName,
                    classType = task.classType,
                    startDate = startZone.toLocalDate(),
                    startTime = startZone.toLocalTime(),
                    endDate = endZone.toLocalDate(),
                    endTime = endZone.toLocalTime(),
                    isAllDay = task.isAllDay,
                    priority = task.priority,
                    hasReminder = task.hasReminder,
                    reminderDate = reminderInfo?.first ?: startZone.toLocalDate(),
                    reminderTime = reminderInfo?.second ?: LocalTime.of(8, 0),
                    isCompleted = task.isCompleted
                )
            }
        }
    }

    /**
     * Ładuje słownik przedmiotów i typów zajęć z lokalnego planu.
     */
    private fun loadAvailableSubjects() {
        viewModelScope.launch {
            classRepository.getAllClassesStream().collect { classes ->
                val subjectsMap = classes
                    .groupBy { it.subjectName }
                    .mapValues { (_, list) -> list.map { it.classType }.distinct().sorted() }
                    .toList()
                    .sortedBy { it.first }

                _uiState.update { it.copy(availableSubjects = subjectsMap) }
            }
        }
    }

    fun updateTitle(v: String) = _uiState.update { it.copy(title = v) }
    fun updateSubjectName(v: String?) = _uiState.update { it.copy(classSubject = v) }
    fun updateClassType(v: String?) = _uiState.update { it.copy(classType = v) }
    fun updateIsAllDay(v: Boolean) = _uiState.update { it.copy(isAllDay = v) }
    fun updateStartDate(v: LocalDate) = _uiState.update { it.copy(startDate = v) }
    fun updateEndDate(v: LocalDate) = _uiState.update { it.copy(endDate = v) }
    fun updateStartTime(v: LocalTime) = _uiState.update { it.copy(startTime = v) }
    fun updateEndTime(v: LocalTime) = _uiState.update { it.copy(endTime = v) }
    fun updateDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun updatePriority(v: Int) = _uiState.update { it.copy(priority = v) }
    fun updateHasReminder(v: Boolean) = _uiState.update { it.copy(hasReminder = v) }
    fun updateReminderDate(v: LocalDate) = _uiState.update { it.copy(reminderDate = v) }
    fun updateReminderTime(v: LocalTime) = _uiState.update { it.copy(reminderTime = v) }

    /**
     * Zapisuje nowe lub edytowane zadanie.
     *
     * Ważne:
     * - zachowuje `isCompleted` przy edycji,
     * - nie zgaduje ID po insercie przez max() (eliminuje race condition),
     * - alarm przypomnienia ustawia tylko gdy reminder > teraz.
     */
    fun saveTask() {
        viewModelScope.launch {
            val state = _uiState.value
            val zone = ZoneId.systemDefault()

            val dueMillis = state.startDate
                .atTime(if (state.isAllDay) LocalTime.MIN else state.startTime)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

            val endMillis = state.endDate
                .atTime(if (state.isAllDay) LocalTime.MAX else state.endTime)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

            val reminderMillis = if (state.hasReminder) {
                state.reminderDate
                    .atTime(state.reminderTime)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
            } else {
                null
            }

            val currentTaskId = state.taskId

            // Dla nowego zadania id=0, dla edycji zachowujemy id.
            val taskToSave = TaskEntity(
                id = currentTaskId,
                title = state.title.trim(),
                description = state.description.ifBlank { null },
                subjectName = state.classSubject?.trim().orEmpty(),
                classType = state.classType?.trim().orEmpty(),
                priority = state.priority,
                isAllDay = state.isAllDay,
                dueDate = dueMillis,
                endDate = endMillis,
                isCompleted = state.isCompleted,
                hasReminder = state.hasReminder,
                reminderTime = reminderMillis
            )

            if (currentTaskId == 0) {
                // INSERT zwraca realne ID w bazie – używamy go do alarmu.
                val insertedId = tasksRepository.insertTask(taskToSave).toInt()
                scheduleReminderIfNeeded(insertedId, taskToSave.title, reminderMillis, state.hasReminder)
            } else {
                tasksRepository.updateTask(taskToSave)
                scheduleReminderIfNeeded(currentTaskId, taskToSave.title, reminderMillis, state.hasReminder)
            }

            _isSaved.value = true
        }
    }

    /**
     * Usuwa zadanie i anuluje jego alarm.
     */
    fun deleteTask() {
        viewModelScope.launch {
            val id = _uiState.value.taskId
            if (id == 0) return@launch

            val task = tasksRepository.getTaskById(id).firstOrNull() ?: return@launch
            tasksRepository.deleteTask(task)
            NotificationHelper.cancelAlarm(getApplication(), id, isTask = true)
            _isSaved.value = true
        }
    }

    /**
     * Zarządza alarmem przypomnienia:
     * - najpierw anuluje poprzedni,
     * - potem ewentualnie ustawia nowy.
     */
    private fun scheduleReminderIfNeeded(
        taskId: Int,
        taskTitle: String,
        reminderMillis: Long?,
        hasReminder: Boolean
    ) {
        NotificationHelper.cancelAlarm(getApplication(), taskId, isTask = true)

        if (hasReminder && reminderMillis != null && reminderMillis > System.currentTimeMillis()) {
            NotificationHelper.scheduleExactAlarm(
                context = getApplication(),
                timeInMillis = reminderMillis,
                id = taskId,
                title = "Przypomnienie o zadaniu",
                message = taskTitle,
                isTask = true
            )
        }
    }
}
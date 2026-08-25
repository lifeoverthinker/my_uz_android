package com.example.my_uz_android.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.data.models.EventEntity
import com.example.my_uz_android.data.models.SettingsEntity
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.data.models.UserCourseEntity
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.EventRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import com.example.my_uz_android.data.repositories.TasksRepository
import com.example.my_uz_android.data.repositories.UniversityRepository
import com.example.my_uz_android.data.repositories.UserCourseRepository
import com.example.my_uz_android.util.SubgroupMatcher
import com.example.my_uz_android.util.classesStillRemainingToday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Model danych reprezentujący kompletny stan interfejsu (UI State) dla ekranu głównego.
 *
 * Klasa ta agreguje wszystkie informacje niezbędne do wyrenderowania pulpitu studenta,
 * w tym spersonalizowane powitania, aktualny plan zajęć, listę nadchodzących zadań
 * oraz wskaźniki postępu semestru. Hermetyzacja tych danych w jednym, niemutowalnym
 * obiekcie gwarantuje spójność wyświetlanych informacji i ułatwia testowanie.
 */
data class HomeUiState(
    val userName: String = "Student",
    val studyFields: List<String> = emptyList(),
    val faculties: List<String> = emptyList(),
    val semester: Int? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasAnyClasses: Boolean = false,
    val todaysClasses: List<ClassEntity> = emptyList(),
    val tomorrowClasses: List<ClassEntity> = emptyList(),
    val upcomingTasks: List<TaskEntity> = emptyList(),
    val todaysEvents: List<EventEntity> = emptyList(),
    val semesterProgress: Float = 0f,
    val daysLeftInSemester: Int = 0,
    val error: String? = null,
    val classColorMap: Map<String, Int> = emptyMap(),
    val currentDateReference: LocalDate = LocalDate.now(ZoneId.of("Europe/Warsaw"))
)

/**
 * Komponent warstwy prezentacji (ViewModel) zarządzający logiką ekranu głównego.
 *
 * Integruje strumienie danych z wielu repozytoriów, przetwarza je w oparciu o bieżący czas
 * oraz preferencje użytkownika i eksponuje gotowy stan do warstwy widoku.
 * Implementuje również mechanizm cyklicznego odświeżania czasu systemowego, zapobiegając
 * dezaktualizacji widoków po zmianie dób.
 */
class HomeViewModel(
    application: Application,
    private val classRepository: ClassRepository,
    private val settingsRepository: SettingsRepository,
    private val tasksRepository: TasksRepository,
    private val universityRepository: UniversityRepository,
    private val userCourseRepository: UserCourseRepository,
    private val eventRepository: EventRepository
) : AndroidViewModel(application) {

    private val gson = Gson()
    private val _isLoadingNetwork = MutableStateFlow(false)
    private val isRefreshInProgress = AtomicBoolean(false)

    private val _currentTimeReference = MutableStateFlow(LocalDateTime.now(ZoneId.of("Europe/Warsaw")))
    private var timeTickerJob: Job? = null

    init {
        startTimeTicker()
    }

    /**
     * Strumień serwujący połączony i przetworzony stan UI.
     * Wykorzystuje operator [combine] do nasłuchiwania zmian w bazie danych, ustawieniach
     * oraz lokalnym czasie, gwarantując asynchroniczne i bezpieczne przygotowanie danych do wyświetlenia.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.getSettingsStream(),
        classRepository.getAllClassesStream(),
        tasksRepository.getAllTasks(),
        userCourseRepository.getAllUserCoursesStream(),
        eventRepository.getAllEventsStream(),
        _isLoadingNetwork,
        _currentTimeReference
    ) { args: Array<Any?> ->
        try {
            val settings = args[0] as SettingsEntity?
            @Suppress("UNCHECKED_CAST") val myClasses = args[1] as List<ClassEntity>
            @Suppress("UNCHECKED_CAST") val tasks = args[2] as List<TaskEntity>
            @Suppress("UNCHECKED_CAST") val courses = args[3] as List<UserCourseEntity>
            @Suppress("UNCHECKED_CAST") val events = args[4] as List<EventEntity>
            val isLoadingNet = args[5] as Boolean
            val nowReference = args[6] as LocalDateTime

            val today = nowReference.toLocalDate()
            val tomorrow = today.plusDays(1)

            val colorMap = try {
                val type = object : TypeToken<Map<String, Int>>() {}.type
                gson.fromJson<Map<String, Int>>(settings?.classColorsJson ?: "{}", type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            val allStudyFields = mutableListOf<String>()
            settings?.selectedGroupCode?.let { mainGroup ->
                val fieldStr = if (!settings.fieldOfStudy.isNullOrBlank()) {
                    "${settings.fieldOfStudy} ($mainGroup)"
                } else {
                    mainGroup
                }
                allStudyFields.add(fieldStr)
            }

            courses.forEach { course ->
                val fieldStr = if (!course.fieldOfStudy.isNullOrBlank()) {
                    "${course.fieldOfStudy} (${course.groupCode})"
                } else {
                    course.groupCode
                }
                if (!allStudyFields.contains(fieldStr)) {
                    allStudyFields.add(fieldStr)
                }
            }

            val facultiesSet = mutableSetOf<String>()
            settings?.faculty?.let { if (it.isNotBlank()) facultiesSet.add(it) }
            courses.forEach { course ->
                course.faculty?.let { if (it.isNotBlank()) facultiesSet.add(it) }
            }
            val facultiesList = facultiesSet.toList().sorted()

            val allUserCodesLower = mutableSetOf<String>()
            settings?.selectedGroupCode?.let { allUserCodesLower.add(it.trim().lowercase()) }
            courses.forEach { allUserCodesLower.add(it.groupCode.trim().lowercase()) }

            val userEnrollments = SubgroupMatcher.buildUserEnrollments(settings, courses, allUserCodesLower)

            val visibleClasses = myClasses.filter { classItem ->
                SubgroupMatcher.isClassVisible(
                    classItem.groupCode,
                    classItem.classType,
                    classItem.subgroup,
                    userEnrollments
                )
            }

            val finalVisibleClasses = if (visibleClasses.isEmpty() && myClasses.isNotEmpty() && userEnrollments.isEmpty()) {
                myClasses
            } else {
                visibleClasses
            }

            val classesForToday = classesStillRemainingToday(
                classes = finalVisibleClasses,
                today = today,
                nowTime = nowReference.toLocalTime()
            )

            val classesForTomorrow = finalVisibleClasses.filter { it.date == tomorrow.toString() }
            val upcomingTasks = tasks.filter { !it.isCompleted }.sortedBy { it.dueDate }

            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", java.util.Locale("pl", "PL"))
            val todayStr = today.format(formatter).replaceFirstChar { it.uppercase() }
            val todaysEvents = events.filter { it.date == todayStr }

            val totalDays = ChronoUnit.DAYS.between(LocalDate.of(2025, 2, 24), LocalDate.of(2025, 6, 20)).toFloat()
            val daysPassed = ChronoUnit.DAYS.between(LocalDate.of(2025, 2, 24), today).coerceAtLeast(0L).toFloat()
            val progress = (daysPassed / totalDays).coerceIn(0f, 1f)
            val left = ChronoUnit.DAYS.between(today, LocalDate.of(2025, 6, 20)).coerceAtLeast(0).toInt()

            val showLoading = settings == null

            HomeUiState(
                userName = settings?.userName ?: "Student",
                studyFields = allStudyFields,
                faculties = facultiesList,
                semester = settings?.currentSemester,
                isLoading = showLoading,
                isRefreshing = isLoadingNet,
                hasAnyClasses = finalVisibleClasses.isNotEmpty(),
                todaysClasses = classesForToday,
                tomorrowClasses = classesForTomorrow,
                upcomingTasks = upcomingTasks,
                todaysEvents = todaysEvents,
                semesterProgress = progress,
                daysLeftInSemester = left,
                classColorMap = colorMap,
                currentDateReference = today
            )
        } catch (e: Exception) {
            Log.e("HomeVM", "Błąd generowania stanu UI", e)
            HomeUiState(error = e.localizedMessage, isLoading = false)
        }
    }
        .catch { e -> Log.e("HomeVM", "Flow error", e) }
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HomeUiState(isLoading = true)
        )

    /**
     * Wymusza synchronizację lokalnego harmonogramu z zewnętrznym API na podstawie zdefiniowanych
     * przez studenta kierunków i grup.
     * Wykorzystuje blokadę [AtomicBoolean], aby zapobiec nakładaniu się na siebie wielokrotnych żądań sieciowych.
     */
    fun refreshSchedule() {
        viewModelScope.launch {
            if (!isRefreshInProgress.compareAndSet(false, true)) return@launch
            try {
                val settings = settingsRepository.getSettingsStream().firstOrNull() ?: return@launch
                val groupCodes = mutableListOf<Pair<String, String?>>()

                settings.selectedGroupCode?.let { groupCodes.add(it to settings.selectedSubgroup) }

                val extraCourses = userCourseRepository.getAllUserCoursesStream().firstOrNull().orEmpty()
                extraCourses.forEach { groupCodes.add(it.groupCode to it.selectedSubgroup) }

                if (groupCodes.isEmpty()) return@launch

                _isLoadingNetwork.value = true

                groupCodes
                    .distinctBy { (code, subgroup) -> code.trim().lowercase() to subgroup?.trim()?.lowercase() }
                    .forEach { (code, subgroup) ->
                        universityRepository.refreshSchedule(code, subgroup, classRepository)
                    }
            } catch (e: Exception) {
                Log.e("HomeVM", "Błąd odświeżania: ${e.message}")
            } finally {
                _isLoadingNetwork.value = false
                isRefreshInProgress.set(false)
            }
        }
    }

    /**
     * Uruchamia mechanizm cyklicznej aktualizacji referencji czasu systemowego w tle.
     * Gwarantuje prawidłowe przejście danych w widoku na nowy dzień o północy, automatycznie
     * inicjując proces ponownego przeliczenia strumienia [uiState] bez ingerencji użytkownika.
     */
    private fun startTimeTicker() {
        timeTickerJob?.cancel()
        timeTickerJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                val newTime = LocalDateTime.now(ZoneId.of("Europe/Warsaw"))
                _currentTimeReference.value = newTime
            }
        }
    }
}




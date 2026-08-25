package com.example.my_uz_android.ui.screens.index

/**
 * ViewModel zarządzający listą nieobecności oraz sprawdzaniem limitów przedmiotów.
 * Filtruje zajęcia z uwzględnieniem podgrup, kierunków oraz faktu,
 * czy dana nieobecność została usprawiedliwiona (nie licząc jej do limitu).
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.AbsenceEntity
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.data.models.SettingsEntity
import com.example.my_uz_android.data.models.UserCourseEntity
import com.example.my_uz_android.data.repositories.AbsenceRepository
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import com.example.my_uz_android.data.repositories.UserCourseRepository
import com.example.my_uz_android.util.SubgroupMatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Model danych reprezentujący zbiór nieobecności dla pojedynczego przedmiotu.
 */
data class SubjectAbsences(
    val subjectName: String,
    val courseName: String,
    val types: List<AbsenceTypeGroup>
)

/**
 * Model danych grupujący nieobecności z podziałem na rodzaj zajęć (Wykład, Lab)
 * oraz przechowujący ich dopuszczalny limit.
 */
data class AbsenceTypeGroup(
    val classType: String,
    val absences: List<AbsenceEntity>,
    val limit: Int
)

/**
 * Dostępne stany ładowania ekranu nieobecności.
 */
sealed interface AbsencesUiState {
    data object Loading : AbsencesUiState
    data class Success(val data: List<SubjectAbsences>) : AbsencesUiState
    data class Error(val message: String) : AbsencesUiState
}

private data class AbsenceCourseFilter(
    val groupCode: String,
    val subgroupRaw: String?
)

class AbsencesViewModel(
    private val absenceRepository: AbsenceRepository,
    private val classRepository: ClassRepository,
    private val settingsRepository: SettingsRepository,
    private val userCourseRepository: UserCourseRepository
) : ViewModel() {

    private val _limits = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _selectedGroups = MutableStateFlow<Set<String>>(emptySet())
    private var isGroupsInitialized = false

    private val _uiState = MutableStateFlow<AbsencesUiState>(AbsencesUiState.Loading)
    val uiState: StateFlow<AbsencesUiState> = _uiState.asStateFlow()

    @Suppress("UNCHECKED_CAST")
    private val absencesDataFlow: Flow<List<SubjectAbsences>> = combine(
        absenceRepository.getAllAbsencesStream(),
        classRepository.getAllClassesStream(),
        _limits,
        userCourseRepository.getAllUserCoursesStream(),
        settingsRepository.getSettingsStream(),
        _selectedGroups
    ) { args: Array<Any?> ->
        val absences = args[0] as List<AbsenceEntity>
        val allClasses = args[1] as List<ClassEntity>
        val limits = args[2] as Map<String, Int>
        val courses = args[3] as List<UserCourseEntity>
        val settings = args[4] as SettingsEntity?
        val selectedGroupsRaw = args[5] as Set<String>

        buildSubjectAbsences(
            absences = absences,
            allClasses = allClasses,
            limits = limits,
            courses = courses,
            settings = settings,
            selectedGroupsRaw = selectedGroupsRaw
        )
    }

    init {
        viewModelScope.launch {
            absencesDataFlow
                .catch { e ->
                    _uiState.value = AbsencesUiState.Error(e.message ?: "Wystąpił błąd")
                }
                .collect { data ->
                    _uiState.value = AbsencesUiState.Success(data)
                }
        }
    }

    /**
     * Włącza lub wyłącza widoczność wybranego kierunku na ekranie nieobecności.
     * Nie pozwala wyłączyć ostatniego aktywnego kierunku.
     */
    fun toggleGroupVisibility(groupCode: String) {
        val normalized = normalizeGroupCodeAbsencesVm(groupCode)
        val current = _selectedGroups.value.toMutableSet()

        if (current.contains(normalized) && current.size <= 1) return

        if (current.contains(normalized)) current.remove(normalized) else current.add(normalized)
        _selectedGroups.value = current
    }

    val absencesState: StateFlow<List<SubjectAbsences>> = _uiState
        .map { state -> if (state is AbsencesUiState.Success) state.data else emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Usuwa wskazaną nieobecność z bazy danych.
     */
    fun deleteAbsence(absence: AbsenceEntity) = viewModelScope.launch {
        absenceRepository.deleteAbsence(absence)
    }

    /**
     * Aktualizuje lokalny, wybrany przez użytkownika limit nieobecności dla danych zajęć.
     */
    fun updateLimit(subjectName: String, classType: String, newLimit: Int) {
        val key = normalizeLimitKeyAbsencesVm(subjectName, classType)
        _limits.update { it + (key to newLimit) }
    }

    /**
     * Buduje skomplikowaną strukturę listy nieobecności dla UI, uwzględniając podział na przedmioty,
     * weryfikację użytkownika (czy ma dostęp do zajęć), sprawdzanie podgrup itp.
     */
    private fun buildSubjectAbsences(
        absences: List<AbsenceEntity>,
        allClasses: List<ClassEntity>,
        limits: Map<String, Int>,
        courses: List<UserCourseEntity>,
        settings: SettingsEntity?,
        selectedGroupsRaw: Set<String>
    ): List<SubjectAbsences> {
        val allCoursesForUi = buildAllCoursesForUiAbsencesVm(settings, courses)
        val allUserCodes = allCoursesForUi.map { normalizeGroupCodeAbsencesVm(it.groupCode) }.toSet()

        val activeCodes = resolveActiveCodesAbsencesVm(allUserCodes, selectedGroupsRaw)
        val filtersByGroup = buildActiveFilters(settings, courses)
            .filter { normalizeGroupCodeAbsencesVm(it.groupCode) in activeCodes }
            .groupBy { normalizeGroupCodeAbsencesVm(it.groupCode) }

        val activeClasses = allClasses.filter { clazz ->
            val groupCode = normalizeGroupCodeAbsencesVm(clazz.groupCode)
            if (groupCode !in activeCodes) return@filter false

            val filtersForGroup = filtersByGroup[groupCode] ?: return@filter false
            val selectedSubgroupsRaw = filtersForGroup.map { it.subgroupRaw }

            val hasAnySelectedSubgroup = selectedSubgroupsRaw.any { !it.isNullOrBlank() }
            if (!hasAnySelectedSubgroup) return@filter true

            SubgroupMatcher.matches(
                classSubgroupRaw = clazz.subgroup,
                selectedSubgroupsRaw = selectedSubgroupsRaw
            )
        }

        val courseMap = allCoursesForUi.associateBy { normalizeGroupCodeAbsencesVm(it.groupCode) }

        val subjectToCourseMap = activeClasses
            .groupBy { normalizeSubjectAbsencesVm(it.subjectName) }
            .mapValues { (_, classesForSubject) ->
                val groupCode = normalizeGroupCodeAbsencesVm(classesForSubject.firstOrNull()?.groupCode)
                val course = courseMap[groupCode]
                course?.fieldOfStudy ?: course?.groupCode ?: groupCode
            }
            .filterValues { it.isNotBlank() }

        val validAbsences = absences

        return validAbsences
            .groupBy { normalizeSubjectAbsencesVm(it.subjectName) }
            .toSortedMap()
            .mapNotNull { (_, absencesForSubject) ->
                if (absencesForSubject.isEmpty()) return@mapNotNull null

                val displaySubjectName = absencesForSubject.first().subjectName
                val allTypes = absencesForSubject
                    .map { normalizeTypeAbsencesVm(it.classType) }
                    .distinct()
                    .sorted()

                val typeGroups = allTypes.map { type ->
                    val limitKey = normalizeLimitKeyAbsencesVm(displaySubjectName, type)
                    val currentLimit = limits[limitKey] ?: 2

                    // Rozdzielamy je logicznie do sortowania, ale zachowujemy wszystkie by je narysować w UI!
                    val typeAbsences = absencesForSubject
                        .filter { normalizeTypeAbsencesVm(it.classType) == type }
                        .sortedBy { it.date }

                    // Właściwość, z której aplikacja liczy "Ile zostało" nieobecności.
                    // Filtr: if (!it.isExcused) oznacza, że wliczamy TYLKO te, które są NIeusprawiedliwione
                    AbsenceTypeGroup(
                        classType = type,
                        absences = typeAbsences,
                        limit = currentLimit
                    )
                }

                val courseName = subjectToCourseMap[normalizeSubjectAbsencesVm(displaySubjectName)] ?: "Inne / Dawne"

                SubjectAbsences(
                    subjectName = displaySubjectName,
                    courseName = courseName,
                    types = typeGroups
                )
            }
    }

    private fun resolveActiveCodesAbsencesVm(
        allUserCodes: Set<String>,
        selectedGroupsRaw: Set<String>
    ): Set<String> {
        val selectedNormalized = selectedGroupsRaw
            .map { normalizeGroupCodeAbsencesVm(it) }
            .filter { it in allUserCodes }
            .toSet()

        return if (selectedNormalized.isNotEmpty()) {
            selectedNormalized
        } else {
            allUserCodes
        }
    }

    private fun buildAllCoursesForUiAbsencesVm(
        settings: SettingsEntity?,
        courses: List<UserCourseEntity>
    ): List<UserCourseEntity> {
        val allCoursesForUi = mutableListOf<UserCourseEntity>()

        settings?.selectedGroupCode?.let { mainCode ->
            allCoursesForUi.add(
                UserCourseEntity(
                    id = -1,
                    groupCode = mainCode,
                    fieldOfStudy = settings.fieldOfStudy ?: mainCode,
                    semester = settings.currentSemester,
                    selectedSubgroup = settings.selectedSubgroup
                )
            )
        }

        courses.forEach { course ->
            val code = normalizeGroupCodeAbsencesVm(course.groupCode)
            if (allCoursesForUi.none { normalizeGroupCodeAbsencesVm(it.groupCode) == code }) {
                allCoursesForUi.add(course)
            }
        }

        return allCoursesForUi
    }

    private fun buildActiveFilters(
        settings: SettingsEntity?,
        courses: List<UserCourseEntity>
    ): List<AbsenceCourseFilter> {
        val out = mutableListOf<AbsenceCourseFilter>()
        settings?.selectedGroupCode?.let { out.add(AbsenceCourseFilter(it, settings.selectedSubgroup)) }
        courses.forEach { out.add(AbsenceCourseFilter(it.groupCode, it.selectedSubgroup)) }
        return out
    }
}

private fun normalizeGroupCodeAbsencesVm(value: String?): String = value?.trim()?.lowercase().orEmpty()
private fun normalizeSubjectAbsencesVm(value: String?): String = value?.trim()?.lowercase().orEmpty()
private fun normalizeTypeAbsencesVm(value: String?): String {
    val raw = value?.trim().orEmpty()
    return if (raw.isBlank()) "Inne" else raw
}

private fun normalizeLimitKeyAbsencesVm(subjectName: String?, classType: String?): String {
    return "${normalizeSubjectAbsencesVm(subjectName)}|${normalizeTypeAbsencesVm(classType).lowercase()}"
}
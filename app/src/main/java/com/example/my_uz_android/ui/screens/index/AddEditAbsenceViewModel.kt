package com.example.my_uz_android.ui.screens.index

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.AbsenceEntity
import com.example.my_uz_android.data.models.SettingsEntity
import com.example.my_uz_android.data.models.UserCourseEntity
import com.example.my_uz_android.data.repositories.AbsenceRepository
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import com.example.my_uz_android.data.repositories.UserCourseRepository
import com.example.my_uz_android.util.SubgroupMatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AddEditAbsenceUiState(
    val id: Int = 0,
    val subjectName: String? = null,
    val classType: String? = null,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val isExcused: Boolean = false,
    val availableSubjects: List<Pair<String, List<String>>> = emptyList()
)

private data class AddEditAbsenceCourseFilter(
    val groupCode: String,
    val subgroupRaw: String?
)

class AddEditAbsenceViewModel(
    savedStateHandle: SavedStateHandle,
    private val absenceRepository: AbsenceRepository,
    private val classRepository: ClassRepository,
    private val settingsRepository: SettingsRepository,
    private val userCourseRepository: UserCourseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditAbsenceUiState())
    val uiState: StateFlow<AddEditAbsenceUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private var loadedAbsenceId: Int? = null

    init {
        loadAvailableSubjectsFilteredByPlan()

        val absenceId = savedStateHandle.get<Int>("absenceId")
        if (absenceId != null && absenceId != -1 && absenceId != 0) {
            loadAbsence(absenceId)
        } else {
            val preSubject = savedStateHandle.get<String>("subject")
            val preType = savedStateHandle.get<String>("classType")
            initNewAbsence(preSubject, preType)
        }
    }

    fun initNewAbsence(subject: String?, type: String?) {
        _uiState.update {
            it.copy(
                id = 0,
                subjectName = subject,
                classType = type,
                description = "",
                date = System.currentTimeMillis(),
                isExcused = false
            )
        }
    }

    fun loadAbsence(absenceId: Int) {
        if (loadedAbsenceId == absenceId) return
        loadedAbsenceId = absenceId

        viewModelScope.launch {
            val allAbsences = absenceRepository.getAllAbsencesStream().first()
            val absence = allAbsences.find { it.id == absenceId }

            if (absence != null) {
                _uiState.update {
                    it.copy(
                        id = absence.id,
                        subjectName = absence.subjectName,
                        classType = absence.classType,
                        description = absence.description ?: "",
                        date = absence.date,
                        isExcused = absence.isExcused
                    )
                }
            }
        }
    }

    private fun loadAvailableSubjectsFilteredByPlan() {
        viewModelScope.launch {
            combine(
                classRepository.getAllClassesStream(),
                settingsRepository.getSettingsStream(),
                userCourseRepository.getAllUserCoursesStream()
            ) { classes, settings, courses ->
                buildAvailableSubjects(classes, settings, courses)
            }.collect { subjectsMap ->
                _uiState.update { it.copy(availableSubjects = subjectsMap) }
            }
        }
    }

    private fun buildAvailableSubjects(
        classes: List<com.example.my_uz_android.data.models.ClassEntity>,
        settings: SettingsEntity?,
        courses: List<UserCourseEntity>
    ): List<Pair<String, List<String>>> {

        fun normalizeGroupCode(v: String?): String = v?.trim()?.uppercase().orEmpty()
        fun normalizeType(v: String?): String = v?.trim().orEmpty().ifBlank { "Inne" }

        val activeCodes = buildSet {
            settings?.selectedGroupCode?.takeIf { it.isNotBlank() }?.let { add(normalizeGroupCode(it)) }
            courses.forEach { add(normalizeGroupCode(it.groupCode)) }
        }

        if (activeCodes.isEmpty()) return emptyList()

        val filters = mutableListOf<AddEditAbsenceCourseFilter>()
        settings?.selectedGroupCode?.let {
            filters.add(AddEditAbsenceCourseFilter(it, settings.selectedSubgroup))
        }
        courses.forEach {
            filters.add(AddEditAbsenceCourseFilter(it.groupCode, it.selectedSubgroup))
        }
        val filtersByGroup = filters.groupBy { normalizeGroupCode(it.groupCode) }

        val visibleClasses = classes.filter { clazz ->
            val groupCode = normalizeGroupCode(clazz.groupCode)
            if (groupCode !in activeCodes) return@filter false

            val filtersForGroup = filtersByGroup[groupCode] ?: return@filter false
            val selectedSubgroupsRaw = filtersForGroup.map { it.subgroupRaw }

            /**
             * Jeśli dla grupy nie ma żadnej wybranej podgrupy,
             * pokazujemy wszystkie zajęcia tej grupy.
             */
            val hasAnySelectedSubgroup = selectedSubgroupsRaw.any { !it.isNullOrBlank() }
            if (!hasAnySelectedSubgroup) return@filter true

            SubgroupMatcher.matches(
                classSubgroupRaw = clazz.subgroup,
                selectedSubgroupsRaw = selectedSubgroupsRaw
            )
        }

        return visibleClasses
            .groupBy { it.subjectName }
            .mapValues { (_, classList) ->
                classList
                    .map { normalizeType(it.classType) }
                    .distinct()
                    .sorted()
            }
            .toList()
            .sortedBy { it.first.lowercase() }
    }

    fun updateSubjectName(name: String?) {
        _uiState.update { it.copy(subjectName = name) }
    }

    fun updateClassType(type: String?) {
        _uiState.update { it.copy(classType = type) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateDate(date: Long) {
        _uiState.update { it.copy(date = date) }
    }

    fun updateIsExcused(isExcused: Boolean) {
        _uiState.update { it.copy(isExcused = isExcused) }
    }

    fun saveAbsence() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.subjectName == null) return@launch

            val entity = AbsenceEntity(
                id = state.id,
                subjectName = state.subjectName,
                classType = state.classType ?: "",
                date = state.date,
                description = state.description.ifBlank { null },
                isExcused = state.isExcused
            )

            absenceRepository.insertAbsence(entity)
            _isSaved.value = true
        }
    }

    // Usuwamy obecną nieobecność z bazy
    fun deleteAbsence() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.id != 0) {
                val entity = AbsenceEntity(
                    id = state.id,
                    subjectName = state.subjectName ?: "",
                    date = state.date,
                    classType = state.classType ?: "",
                    description = state.description.ifBlank { null },
                    isExcused = state.isExcused
                )
                absenceRepository.deleteAbsence(entity)
                _isSaved.value = true
            }
        }
    }
}
package com.example.my_uz_android.ui.screens.index

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.GradeEntity
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.GradesRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class GradeType {
    STANDARD,
    CUSTOM,
    ACTIVITY,
    POINTS
}

data class AddEditGradeUiState(
    val gradeId: Int = 0,
    val subjectName: String? = null,
    val classType: String? = null,
    val gradeType: GradeType = GradeType.STANDARD,
    val gradeValue: Double? = 5.0, // NAPRAWIONY BŁĄD: startujemy z domyślnym 5.0 a nie null
    val customGradeValue: String = "",
    val weight: String = "1",
    val description: String = "",
    val comment: String = "",
    val date: Long = System.currentTimeMillis(),
    val availableSubjects: List<Pair<String, List<String>>> = emptyList()
)

class AddEditGradeViewModel(
    savedStateHandle: SavedStateHandle,
    private val gradesRepository: GradesRepository,
    private val classRepository: ClassRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditGradeUiState())
    val uiState: StateFlow<AddEditGradeUiState> = _uiState.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private var currentSemesterFromSettings: Int = 1
    private var loadedGradeId: Int? = null

    init {
        loadAvailableSubjects()
        loadCurrentSemester()

        val gradeId = savedStateHandle.get<Int>("gradeId") ?: 0

        if (gradeId > 0) {
            loadGrade(gradeId)
        } else {
            val subject = savedStateHandle.get<String>("subject")
            val type = savedStateHandle.get<String>("classType")
            val desc = savedStateHandle.get<String>("description")
            val weightStr = savedStateHandle.get<String>("weight")
            val gradeValStr = savedStateHandle.get<String>("gradeValue")
            val comment = savedStateHandle.get<String>("comment")

            initNewGrade(subject, type, desc, weightStr, gradeValStr, comment)
        }
    }

    fun initNewGrade(
        subject: String?,
        type: String?,
        desc: String? = null,
        weight: String? = null,
        gradeVal: String? = null,
        comment: String? = null
    ) {
        val gradeDouble = gradeVal?.toDoubleOrNull()
        _uiState.update {
            it.copy(
                gradeId = 0,
                subjectName = subject,
                classType = type,
                description = desc ?: "",
                weight = weight ?: "1",
                gradeValue = if (gradeDouble != -1.0) (gradeDouble ?: 5.0) else null, // Zabezpieczenie na domyślne 5.0
                gradeType = if (gradeDouble == -1.0) GradeType.ACTIVITY else GradeType.STANDARD,
                comment = comment ?: "",
                date = System.currentTimeMillis()
            )
        }
    }

    fun loadGrade(gradeId: Int) {
        if (loadedGradeId == gradeId) return
        loadedGradeId = gradeId

        viewModelScope.launch {
            val grade = gradesRepository.getGradeByIdStream(gradeId).first()
            if (grade != null) {
                val type = when {
                    grade.isPoints -> GradeType.POINTS
                    grade.grade == -1.0 -> GradeType.ACTIVITY
                    else -> GradeType.STANDARD
                }

                _uiState.update {
                    it.copy(
                        gradeId = grade.id,
                        subjectName = grade.subjectName,
                        classType = grade.classType,
                        gradeType = type,
                        gradeValue = if (type == GradeType.STANDARD) grade.grade else null,
                        customGradeValue = if (type == GradeType.POINTS) {
                            if (grade.grade % 1.0 == 0.0) grade.grade.toInt().toString() else grade.grade.toString()
                        } else "",
                        weight = grade.weight.toString(),
                        description = grade.description ?: "",
                        comment = grade.comment ?: "",
                        date = grade.date
                    )
                }
            }
        }
    }

    private fun loadCurrentSemester() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsStream().first()
            currentSemesterFromSettings = settings?.currentSemester ?: 1
        }
    }

    private fun loadAvailableSubjects() {
        viewModelScope.launch {
            classRepository.getAllClassesStream().collect { classes ->
                val subjectsMap = classes.groupBy { it.subjectName }
                    .mapValues { (_, classList) ->
                        classList.map { it.classType }.distinct().sorted()
                    }
                    .toList()
                    .sortedBy { it.first }
                _uiState.update { it.copy(availableSubjects = subjectsMap) }
            }
        }
    }

    fun updateSubjectName(name: String?) { _uiState.update { it.copy(subjectName = name) } }
    fun updateClassType(type: String?) { _uiState.update { it.copy(classType = type) } }

    fun updateGradeType(type: GradeType) {
        _uiState.update {
            it.copy(
                gradeType = type,
                weight = if (type == GradeType.STANDARD) "1" else "0"
            )
        }
    }

    fun updateGradeValue(value: Double?) { _uiState.update { it.copy(gradeValue = value) } }
    fun updateCustomGradeValue(value: String) { _uiState.update { it.copy(customGradeValue = value) } }
    fun updateWeight(weight: String) { _uiState.update { it.copy(weight = weight) } }
    fun updateDescription(description: String) { _uiState.update { it.copy(description = description) } }
    fun updateComment(comment: String) { _uiState.update { it.copy(comment = comment) } }
    fun updateDate(date: Long) { _uiState.update { it.copy(date = date) } }

    // Zapisujemy nową lub edytowaną ocenę do bazy
    fun saveGrade() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.subjectName == null) return@launch

            val currentSettings = settingsRepository.getSettingsStream().firstOrNull()
            val semester = currentSettings?.currentSemester ?: currentSemesterFromSettings

            var finalGrade = 0.0
            var isPoints = false
            var finalWeight = state.weight.toIntOrNull() ?: 1

            when (state.gradeType) {
                GradeType.STANDARD -> {
                    finalGrade = state.gradeValue ?: 5.0
                    isPoints = false
                }
                GradeType.ACTIVITY -> {
                    finalGrade = -1.0
                    isPoints = false
                    finalWeight = 0
                }
                GradeType.CUSTOM -> {
                    finalGrade = state.customGradeValue.toDoubleOrNull() ?: -1.0
                    isPoints = false
                }
                GradeType.POINTS -> {
                    finalGrade = state.customGradeValue.replace(",", ".").toDoubleOrNull() ?: 0.0
                    isPoints = true
                    finalWeight = 0
                }
            }

            val idToSave = state.gradeId

            val entity = GradeEntity(
                id = idToSave,
                subjectName = state.subjectName,
                classType = state.classType ?: "",
                grade = finalGrade,
                weight = finalWeight,
                description = state.description.ifBlank { if (isPoints) "Punkty" else "Ocena" },
                comment = state.comment.ifBlank { null },
                date = state.date,
                semester = semester,
                isPoints = isPoints
            )

            if (idToSave != 0) {
                gradesRepository.updateGrade(entity)
            } else {
                gradesRepository.insertGrade(entity)
            }
            _isSaved.value = true
        }
    }
}
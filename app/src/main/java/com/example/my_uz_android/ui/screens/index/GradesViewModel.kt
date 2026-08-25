package com.example.my_uz_android.ui.screens.index

/**
 * ViewModel zawiadujący logiką Indeksu ocen.
 * Sortuje oceny po przedmiotach, oblicza średnie ważone (przy użyciu GradeCalculator)
 * oraz obsługuje przełączanie (filtrowanie) kierunków i specjalności studenta.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.GradeEntity
import com.example.my_uz_android.data.models.UserCourseEntity
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.GradesRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import com.example.my_uz_android.data.repositories.UserCourseRepository
import com.example.my_uz_android.util.GradeCalculator
import com.example.my_uz_android.util.SubgroupMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Przechowuje statystyki i listę ocen zebranych dla konkretnego rodzaju zajęć (np. tylko Lab).
 */
data class ClassTypeState(
    val name: String,
    val average: Double?,
    val grades: List<GradeEntity>
)

/**
 * Kompletny kontener na przedmiot (zawiera różne ClassTypeState dla Wykładów, Ćwiczeń itp.).
 */
data class SubjectState(
    val uniqueKey: String,
    val code: String,
    val name: String,
    val courseName: String,
    val average: Double?,
    val types: List<ClassTypeState>
)

/**
 * Stan UI przechowujący dane głównego ekranu Indeksu.
 */
data class GradesUiState(
    val subjects: List<SubjectState> = emptyList(),
    val average: Double? = null,
    val courseAverages: List<Pair<String, Double?>> = emptyList(),
    val isLoading: Boolean = false,
    val allGrades: List<GradeEntity> = emptyList(),
    val userCourses: List<UserCourseEntity> = emptyList(),
    val selectedGroupCodes: Set<String> = emptySet(),
    val isPlanSelected: Boolean = false
)

class GradesViewModel(
    private val gradesRepository: GradesRepository,
    private val classRepository: ClassRepository,
    private val settingsRepository: SettingsRepository,
    private val userCourseRepository: UserCourseRepository
) : ViewModel() {

    private val _selectedGroups = MutableStateFlow<Set<String>>(emptySet())
    private var isGroupsInitialized = false

    // Główny proces budujący gigantyczną listę przedmiotów z obliczonymi na żywo średnimi.
    val uiState: StateFlow<GradesUiState> = combine(
        gradesRepository.getAllGradesStream(),
        classRepository.getAllClassesStream(),
        userCourseRepository.getAllUserCoursesStream(),
        settingsRepository.getSettingsStream(),
        _selectedGroups
    ) { grades, allClasses, courses, settings, selectedGroupsRaw ->

        fun normalizeGroupCode(v: String?): String = v?.trim()?.lowercase().orEmpty()
        fun normalizeSubject(v: String?): String = v?.trim()?.lowercase().orEmpty()
        fun normalizeType(v: String?): String {
            val raw = v?.trim().orEmpty()
            return if (raw.isBlank()) "Inne" else raw
        }

        fun subjectTypeKey(subject: String?, classType: String?): String {
            return "${normalizeSubject(subject)}|${normalizeType(classType).lowercase()}"
        }

        val allCoursesForUi = mutableListOf<UserCourseEntity>()
        settings?.selectedGroupCode?.let { main ->
            allCoursesForUi.add(
                UserCourseEntity(
                    id = -1,
                    groupCode = main,
                    fieldOfStudy = settings.fieldOfStudy ?: main,
                    semester = settings.currentSemester,
                    selectedSubgroup = settings.selectedSubgroup
                )
            )
        }
        courses.forEach { course ->
            val code = normalizeGroupCode(course.groupCode)
            if (allCoursesForUi.none { normalizeGroupCode(it.groupCode) == code }) {
                allCoursesForUi.add(course)
            }
        }

        val allUserCodes = allCoursesForUi.map { normalizeGroupCode(it.groupCode) }.toSet()

        val activeCodes: Set<String> = run {
            val selectedNormalized = selectedGroupsRaw
                .map { normalizeGroupCode(it) }
                .filter { it in allUserCodes }
                .toSet()

            if (selectedNormalized.isNotEmpty()) selectedNormalized else allUserCodes
        }

        // Sprawdzamy prawa dostępu (filtry i podgrupy) na poziomie całych list
        val userEnrollments = SubgroupMatcher.buildUserEnrollments(settings, courses, activeCodes)
        val activeClasses = allClasses.filter { clazz ->
            SubgroupMatcher.isClassVisible(
                clazz.groupCode,
                clazz.classType,
                clazz.subgroup,
                userEnrollments
            )
        }

        val subjectTypeToAllGroups = allClasses
            .groupBy { subjectTypeKey(it.subjectName, it.classType) }
            .mapValues { (_, list) ->
                list.map { normalizeGroupCode(it.groupCode) }
                    .filter { it.isNotBlank() }
                    .toSet()
            }

        val subjectTypeToActiveGroups = activeClasses
            .groupBy { subjectTypeKey(it.subjectName, it.classType) }
            .mapValues { (_, list) ->
                list.map { normalizeGroupCode(it.groupCode) }
                    .filter { it.isNotBlank() }
                    .toSet()
            }

        val subjectToAllGroups = allClasses
            .groupBy { normalizeSubject(it.subjectName) }
            .mapValues { (_, list) ->
                list.map { normalizeGroupCode(it.groupCode) }
                    .filter { it.isNotBlank() }
                    .toSet()
            }

        val subjectToActiveGroups = activeClasses
            .groupBy { normalizeSubject(it.subjectName) }
            .mapValues { (_, list) ->
                list.map { normalizeGroupCode(it.groupCode) }
                    .filter { it.isNotBlank() }
                    .toSet()
            }

        val courseMap = allCoursesForUi.associateBy { normalizeGroupCode(it.groupCode) }

        val activeGradesWithGroup = grades.mapNotNull { grade ->
            val typeKey = subjectTypeKey(grade.subjectName, grade.classType)
            val allGroupsForTypeKey = subjectTypeToAllGroups[typeKey].orEmpty()
            val activeGroupsForTypeKey = subjectTypeToActiveGroups[typeKey].orEmpty()

            if (allGroupsForTypeKey.isNotEmpty()) {
                val resolvedGroup = activeGroupsForTypeKey.sorted().firstOrNull() ?: return@mapNotNull null
                return@mapNotNull grade to resolvedGroup
            }

            val subjectKey = normalizeSubject(grade.subjectName)
            val allGroupsForSubject = subjectToAllGroups[subjectKey].orEmpty()
            val activeGroupsForSubject = subjectToActiveGroups[subjectKey].orEmpty()

            if (allGroupsForSubject.isEmpty()) return@mapNotNull grade to null

            val resolvedGroup = activeGroupsForSubject.sorted().firstOrNull() ?: return@mapNotNull null
            grade to resolvedGroup
        }

        val activeGrades = activeGradesWithGroup.map { it.first }

        data class SubjectBucketKey(val subjectKey: String, val groupCode: String?)

        val archiveGroupCode = "ARCHIWUM"

        val classesByBucket = activeClasses.groupBy {
            SubjectBucketKey(
                subjectKey = normalizeSubject(it.subjectName),
                groupCode = normalizeGroupCode(it.groupCode).ifBlank { null }
            )
        }

        val gradesByBucket = activeGradesWithGroup.groupBy {
            SubjectBucketKey(
                subjectKey = normalizeSubject(it.first.subjectName),
                groupCode = it.second ?: archiveGroupCode
            )
        }

        val allBuckets = (classesByBucket.keys + gradesByBucket.keys)
            .distinct()
            .sortedWith(compareBy<SubjectBucketKey>({ it.groupCode ?: "" }, { it.subjectKey }))

        // Budujemy mapę przedmiotów do wyświetlenia na UI
        val subjects = allBuckets.mapNotNull { bucketKey ->
            val classesForSubject = classesByBucket[bucketKey].orEmpty()
            val gradesForSubject = gradesByBucket[bucketKey].orEmpty().map { it.first }

            if (classesForSubject.isEmpty() && gradesForSubject.isEmpty()) return@mapNotNull null

            val displayName = classesForSubject.firstOrNull()?.subjectName
                ?: gradesForSubject.firstOrNull()?.subjectName
                ?: bucketKey.subjectKey

            val typesFromClasses = classesForSubject.map { normalizeType(it.classType) }
            val typesFromGrades = gradesForSubject.map { normalizeType(it.classType) }
            val allTypes = (typesFromClasses + typesFromGrades).distinct().sorted()

            // Wyliczamy osobne średnie dla danego rodzaju zajęć (np. dla Labów)
            val typeStates = allTypes.map { typeName ->
                val gradesForType = gradesForSubject.filter { normalizeType(it.classType) == typeName }
                val avgRaw = GradeCalculator.calculateGPA(gradesForType)
                ClassTypeState(
                    name = typeName,
                    average = if (avgRaw > 0.0) avgRaw else null,
                    grades = gradesForType
                )
            }

            val subjAvgRaw = GradeCalculator.calculateGPA(gradesForSubject)
            val mappedGroup = bucketKey.groupCode
                ?: subjectToActiveGroups[bucketKey.subjectKey].orEmpty().sorted().firstOrNull()
                ?: subjectToAllGroups[bucketKey.subjectKey].orEmpty().sorted().firstOrNull()

            val courseName = if (mappedGroup == archiveGroupCode) {
                "Dodatkowe / Inne"
            } else if (!mappedGroup.isNullOrBlank()) {
                val c = courseMap[mappedGroup]
                c?.fieldOfStudy ?: c?.groupCode ?: mappedGroup
            } else "Inne / Dawne"

            val uniqueKey = "${bucketKey.groupCode ?: archiveGroupCode}|${bucketKey.subjectKey}"

            SubjectState(
                uniqueKey = uniqueKey,
                code = displayName.take(3).uppercase(),
                name = displayName,
                courseName = courseName,
                average = if (subjAvgRaw > 0.0) subjAvgRaw else null,
                types = typeStates
            )
        }.sortedWith(compareBy<SubjectState>({ it.courseName.lowercase() }, { it.name.lowercase() }))

        val overallRaw = GradeCalculator.calculateGPA(activeGrades)

        val activeCourseNames = allCoursesForUi
            .filter { normalizeGroupCode(it.groupCode) in activeCodes }
            .map { it.fieldOfStudy?.ifBlank { it.groupCode } ?: it.groupCode }
            .distinct()

        val gradesByCourseName = activeGradesWithGroup.groupBy { (_, groupCode) ->
            if (groupCode == null || groupCode == archiveGroupCode) {
                "Dodatkowe / Inne"
            } else {
                val c = courseMap[groupCode]
                c?.fieldOfStudy?.ifBlank { c.groupCode } ?: c?.groupCode ?: groupCode
            }
        }

        // Srednie dla calych kierunkow
        val courseAverages = buildList {
            activeCourseNames.forEach { courseName ->
                val gradesForCourse = gradesByCourseName[courseName].orEmpty().map { it.first }
                val avgRaw = GradeCalculator.calculateGPA(gradesForCourse)
                add(courseName to if (avgRaw > 0.0) avgRaw else null)
            }

            if (gradesByCourseName.containsKey("Dodatkowe / Inne")) {
                val gradesForArchive = gradesByCourseName["Dodatkowe / Inne"].orEmpty().map { it.first }
                val avgRaw = GradeCalculator.calculateGPA(gradesForArchive)
                add("Dodatkowe / Inne" to if (avgRaw > 0.0) avgRaw else null)
            }
        }.sortedBy { it.first.lowercase() }

        GradesUiState(
            subjects = subjects,
            average = if (overallRaw > 0.0) overallRaw else null,
            courseAverages = courseAverages,
            isLoading = false,
            allGrades = grades,
            userCourses = allCoursesForUi,
            selectedGroupCodes = activeCodes,
            isPlanSelected = (settings?.selectedGroupCode?.isNotBlank() == true) || courses.isNotEmpty()
        )
    }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            GradesUiState(isLoading = true)
        )

    fun toggleGroupVisibility(groupCode: String) {
        val normalized = groupCode.trim().lowercase()
        val current = _selectedGroups.value.toMutableSet()

        if (current.contains(normalized) && current.size <= 1) return

        if (current.contains(normalized)) current.remove(normalized) else current.add(normalized)
        _selectedGroups.value = current
    }

    suspend fun isPlanSelected(): Boolean {
        val settings = settingsRepository.getSettingsStream().first()
        val courses = userCourseRepository.getAllUserCoursesStream().first()
        return !settings?.selectedGroupCode.isNullOrBlank() || courses.isNotEmpty()
    }

    fun deleteGrade(grade: GradeEntity) { viewModelScope.launch { gradesRepository.deleteGrade(grade) } }
    fun duplicateGrade(grade: GradeEntity) { viewModelScope.launch { gradesRepository.insertGrade(grade.copy(id = 0)) } }
    fun saveGrade(grade: GradeEntity) { viewModelScope.launch { if (grade.id == 0) gradesRepository.insertGrade(grade) else gradesRepository.updateGrade(grade) } }
}
package com.example.my_uz_android.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.data.models.SettingsEntity
import com.example.my_uz_android.data.models.UserCourseEntity
import com.example.my_uz_android.data.models.UserGender
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import com.example.my_uz_android.data.repositories.UniversityRepository
import com.example.my_uz_android.data.repositories.UserCourseRepository
import com.example.my_uz_android.util.NetworkResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val AUTO_SAVE_DEBOUNCE_MS = 1000L
private const val SAVED_FEEDBACK_MS = 3000L
private const val GROUP_SUGGESTIONS_LIMIT = 8

@OptIn(FlowPreview::class)
class AccountViewModel(
    private val settingsRepository: SettingsRepository,
    private val universityRepository: UniversityRepository,
    private val classRepository: ClassRepository,
    private val userCourseRepository: UserCourseRepository
) : ViewModel() {

    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _userSurname = MutableStateFlow("")
    val userSurname = _userSurname.asStateFlow()

    private val _selectedGender = MutableStateFlow<UserGender?>(null)
    val selectedGender = _selectedGender.asStateFlow()

    private val _isAnonymous = MutableStateFlow(false)
    val isAnonymous = _isAnonymous.asStateFlow()

    private val _faculty = MutableStateFlow("")
    val faculty = _faculty.asStateFlow()

    private val _fieldOfStudy = MutableStateFlow("")
    val fieldOfStudy = _fieldOfStudy.asStateFlow()

    private val _studyMode = MutableStateFlow("")
    val studyMode = _studyMode.asStateFlow()

    private val _selectedSubgroups = MutableStateFlow<Set<String>>(emptySet())
    val selectedSubgroups = _selectedSubgroups.asStateFlow()

    private val _mainFaculty = MutableStateFlow("")
    val mainFaculty = _mainFaculty.asStateFlow()

    private val _mainFieldOfStudy = MutableStateFlow("")
    val mainFieldOfStudy = _mainFieldOfStudy.asStateFlow()

    private val _mainStudyMode = MutableStateFlow("")
    val mainStudyMode = _mainStudyMode.asStateFlow()

    private val _mainSelectedSubgroups = MutableStateFlow<Set<String>>(emptySet())
    val mainSelectedSubgroups = _mainSelectedSubgroups.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup = _selectedGroup.asStateFlow()

    private val _availableSubgroups = MutableStateFlow<List<String>>(emptyList())
    val availableSubgroups = _availableSubgroups.asStateFlow()

    private val _additionalUserCourses = MutableStateFlow<List<UserCourseEntity>>(emptyList())
    val additionalUserCourses = _additionalUserCourses.asStateFlow()

    val additionalGroups = _additionalUserCourses
        .map { courses -> courses.map { it.groupCode } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _additionalSubgroupsMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val additionalSubgroupsMap = _additionalSubgroupsMap.asStateFlow()

    private val _availableDirections = MutableStateFlow<List<String>>(emptyList())
    val availableDirections = _availableDirections.asStateFlow()

    private val _activeDirection = MutableStateFlow<String?>(null)
    val activeDirection = _activeDirection.asStateFlow()

    private val _directionToFieldMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val directionToFieldMap = _directionToFieldMap.asStateFlow()

    private val _allGroups = MutableStateFlow<List<String>>(emptyList())
    val allGroups = _allGroups.asStateFlow()

    private val _groupSearchQuery = MutableStateFlow("")
    val groupSearchQuery = _groupSearchQuery.asStateFlow()

    private val _groupCodeToField = MutableStateFlow<Map<String, String>>(emptyMap())

    val filteredGroups = combine(_groupSearchQuery, _allGroups, _groupCodeToField) { query, groups, codeToField ->
        buildFilteredGroups(query, groups, codeToField)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _loadingSubgroupsFor = MutableStateFlow<Set<String>>(emptySet())
    val loadingSubgroupsFor = _loadingSubgroupsFor.asStateFlow()

    private val _isSavedFeedback = MutableStateFlow(false)
    val isSavedFeedback = _isSavedFeedback.asStateFlow()

    private val _triggerSave = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private var currentSettings: SettingsEntity? = null
    private var isEditingProfile = false

    init {
        loadAllGroups()
        loadCurrentData()
        observeAutoSave()
    }

    private fun observeAutoSave() = viewModelScope.launch {
        _triggerSave
            .debounce(AUTO_SAVE_DEBOUNCE_MS)
            .collect { performAutoSave() }
    }

    private fun loadAllGroups() = viewModelScope.launch {
        when (val res = universityRepository.getGroupCodes()) {
            is NetworkResult.Success -> {
                _allGroups.value = (res.data ?: emptyList())
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .toList()
            }

            is NetworkResult.Error -> _allGroups.value = emptyList()
        }
    }

    private fun parseSubgroups(csv: String?): Set<String> {
        return csv
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()
    }

    private fun normalizeSubgroups(raw: List<String>): List<String> {
        return raw
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.equals("null", true) }
            .distinct()
            .sorted()
            .toList()
    }

    private fun normalizeSubgroupCsv(raw: Set<String>, hasMainGroup: Boolean): String? {
        if (!hasMainGroup) return null
        return raw
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .joinToString(",")
    }

    private fun normalizeForSearch(input: String): String = input.filter { it.isLetterOrDigit() }

    private fun buildFilteredGroups(
        query: String,
        groups: List<String>,
        codeToField: Map<String, String>
    ): List<String> {
        val q = query.trim().lowercase()
        if (q.isBlank()) return emptyList()

        val normalizedQ = normalizeForSearch(q)

        return groups
            .asSequence()
            .filter { code ->
                val codeLower = code.lowercase()
                val codeNormalized = normalizeForSearch(codeLower)
                val fieldLower = (codeToField[code] ?: "").lowercase()
                val fieldNormalized = normalizeForSearch(fieldLower)

                codeLower.contains(q) ||
                        codeNormalized.contains(normalizedQ) ||
                        fieldLower.contains(q) ||
                        fieldNormalized.contains(normalizedQ)
            }
            .sortedBy { it.lowercase() }
            .take(GROUP_SUGGESTIONS_LIMIT)
            .toList()
    }

    private fun loadCurrentData() = viewModelScope.launch {
        _isLoading.value = true

        combine(
            settingsRepository.getSettingsStream(),
            userCourseRepository.getAllUserCoursesStream()
        ) { settings, courses -> settings to courses }
            .collect { (settings, courses) ->
                if (settings != null) {
                    currentSettings = settings
                    val hasMainGroup = !settings.selectedGroupCode.isNullOrBlank()

                    if (!isEditingProfile) {
                        val parts = (settings.userName ?: "").split(" ", limit = 2)
                        _userName.value = parts.getOrElse(0) { "" }
                        _userSurname.value = parts.getOrElse(1) { "" }
                        _selectedGender.value = when (settings.gender?.uppercase()) {
                            UserGender.STUDENTKA.name, "STUDENTKA" -> UserGender.STUDENTKA
                            else -> UserGender.STUDENT
                        }
                        _isAnonymous.value = settings.isAnonymous
                        _selectedGroup.value = settings.selectedGroupCode

                        _mainFaculty.value = settings.faculty ?: ""
                        _mainFieldOfStudy.value = settings.fieldOfStudy ?: ""
                        _mainStudyMode.value = settings.studyMode ?: ""

                        settings.selectedGroupCode?.let { group ->
                            if (_availableSubgroups.value.isEmpty()) {
                                launch { loadSubgroupsForGroup(group) }
                            }
                            _mainSelectedSubgroups.value = parseSubgroups(settings.selectedSubgroup)
                        } ?: run {
                            _availableSubgroups.value = emptyList()
                            _mainSelectedSubgroups.value = emptySet()
                        }
                    }

                    val activeDir = settings.activeDirectionCode ?: settings.selectedGroupCode
                    _activeDirection.value = activeDir

                    if (activeDir == settings.selectedGroupCode && hasMainGroup) {
                        _faculty.value = settings.faculty ?: ""
                        _fieldOfStudy.value = settings.fieldOfStudy ?: ""
                        _studyMode.value = settings.studyMode ?: ""
                        _selectedSubgroups.value = parseSubgroups(settings.selectedSubgroup)
                    } else {
                        val activeCourse = courses.find { it.groupCode == activeDir }
                        if (activeCourse != null) {
                            _faculty.value = activeCourse.faculty ?: ""
                            _fieldOfStudy.value = activeCourse.fieldOfStudy ?: ""
                            _studyMode.value = activeCourse.studyMode ?: ""
                            _selectedSubgroups.value = parseSubgroups(activeCourse.selectedSubgroup)
                        } else {
                            _faculty.value = ""
                            _fieldOfStudy.value = ""
                            _studyMode.value = ""
                            _selectedSubgroups.value = emptySet()
                        }
                    }

                    val directionMap = buildDirectionFieldMap(settings, courses)
                    _directionToFieldMap.value = directionMap
                    _groupCodeToField.value = directionMap
                    _availableDirections.value = buildDirections(settings.selectedGroupCode, courses)
                }

                _additionalUserCourses.value = courses
                _additionalSubgroupsMap.update { map ->
                    map.filterKeys { code -> courses.any { it.groupCode == code } }
                }

                courses.forEach { course ->
                    if (!_additionalSubgroupsMap.value.containsKey(course.groupCode)) {
                        launch { loadSubgroupsForAdditional(course.groupCode) }
                    }
                }

                _isLoading.value = false
            }
    }

    private fun buildDirectionFieldMap(
        settings: SettingsEntity,
        courses: List<UserCourseEntity>
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        settings.selectedGroupCode?.let { code ->
            map[code] = settings.fieldOfStudy ?: ""
        }
        courses.forEach { course ->
            map[course.groupCode] = course.fieldOfStudy ?: ""
        }
        return map
    }

    private fun buildDirections(mainCode: String?, courses: List<UserCourseEntity>): List<String> {
        return buildList {
            mainCode?.let { add(it) }
            addAll(courses.map { it.groupCode })
        }.distinct()
    }

    fun exitEditMode() {
        isEditingProfile = false
        _triggerSave.tryEmit(Unit)
    }

    fun setUserName(value: String) {
        isEditingProfile = true
        _userName.value = value
        _triggerSave.tryEmit(Unit)
    }

    fun setUserSurname(value: String) {
        isEditingProfile = true
        _userSurname.value = value
        _triggerSave.tryEmit(Unit)
    }

    fun setGender(gender: UserGender) {
        isEditingProfile = true
        _selectedGender.value = gender
        _triggerSave.tryEmit(Unit)
    }

    fun setGroupSearchQuery(query: String) {
        _groupSearchQuery.value = query
    }

    fun setActiveDirection(code: String) = viewModelScope.launch {
        currentSettings?.let { settings ->
            settingsRepository.insertSettings(settings.copy(activeDirectionCode = code))
        }
    }

    fun selectGroup(groupCode: String) {
        if (_selectedGroup.value == null) {
            isEditingProfile = true
            _selectedGroup.value = groupCode
            _groupSearchQuery.value = ""

            viewModelScope.launch {
                _loadingSubgroupsFor.update { it + groupCode }

                val detailsDef = async { universityRepository.getGroupDetails(groupCode) }
                val subgroupsDef = async { universityRepository.getSubgroups(groupCode) }

                when (val detailsRes = detailsDef.await()) {
                    is NetworkResult.Success -> {
                        _mainFaculty.value = detailsRes.data?.fieldInfo?.faculty ?: ""
                        _mainFieldOfStudy.value = detailsRes.data?.fieldInfo?.name ?: groupCode
                        _mainStudyMode.value = detailsRes.data?.studyMode ?: ""
                    }

                    is NetworkResult.Error -> _mainFieldOfStudy.value = groupCode
                }

                when (val subgroupsRes = subgroupsDef.await()) {
                    is NetworkResult.Success -> {
                        _availableSubgroups.value = normalizeSubgroups(subgroupsRes.data ?: emptyList())
                    }

                    is NetworkResult.Error -> _availableSubgroups.value = emptyList()
                }

                _mainSelectedSubgroups.value = emptySet()
                _loadingSubgroupsFor.update { it - groupCode }
                _triggerSave.tryEmit(Unit)
            }
        } else {
            addAdditionalGroup(groupCode)
            _groupSearchQuery.value = ""
        }
    }

    fun clearMainGroup() {
        isEditingProfile = true
        val previousMainGroup = _selectedGroup.value
        _selectedGroup.value = null
        _selectedSubgroups.value = emptySet()
        _mainSelectedSubgroups.value = emptySet()
        _availableSubgroups.value = emptyList()
        _mainFaculty.value = ""
        _mainFieldOfStudy.value = ""
        _mainStudyMode.value = ""

        if (_activeDirection.value == previousMainGroup) {
            _activeDirection.value = _additionalUserCourses.value.firstOrNull()?.groupCode
        }

        _triggerSave.tryEmit(Unit)
    }

    private suspend fun loadSubgroupsForGroup(groupCode: String) {
        _loadingSubgroupsFor.update { it + groupCode }

        when (val res = universityRepository.getSubgroups(groupCode)) {
            is NetworkResult.Success -> _availableSubgroups.value = normalizeSubgroups(res.data ?: emptyList())
            is NetworkResult.Error -> _availableSubgroups.value = emptyList()
        }

        _loadingSubgroupsFor.update { it - groupCode }
    }

    fun toggleMainSubgroup(subgroup: String) {
        isEditingProfile = true
        val current = _mainSelectedSubgroups.value.toMutableSet()
        if (current.contains(subgroup)) current.remove(subgroup) else current.add(subgroup)
        _mainSelectedSubgroups.value = current
        _triggerSave.tryEmit(Unit)
    }

    fun addAdditionalGroup(code: String) = viewModelScope.launch {
        if (code == _selectedGroup.value || _additionalUserCourses.value.any { it.groupCode == code }) return@launch

        val tempCourse = UserCourseEntity(
            groupCode = code,
            fieldOfStudy = "Wczytywanie...",
            selectedSubgroup = "",
            faculty = "",
            studyMode = "",
            semester = 1
        )
        userCourseRepository.insertUserCourse(tempCourse)

        _loadingSubgroupsFor.update { it + code }

        val detailsDef = async { universityRepository.getGroupDetails(code) }
        val subgroupsDef = async { universityRepository.getSubgroups(code) }

        val detailsRes = detailsDef.await()
        val field = if (detailsRes is NetworkResult.Success) detailsRes.data?.fieldInfo?.name ?: code else code
        val faculty = if (detailsRes is NetworkResult.Success) detailsRes.data?.fieldInfo?.faculty ?: "" else ""
        val mode = if (detailsRes is NetworkResult.Success) detailsRes.data?.studyMode ?: "" else ""

        val semester = if (detailsRes is NetworkResult.Success) {
            val sem = detailsRes.data?.semester?.lowercase().orEmpty()
            if (sem.contains("letni")) 2 else 1
        } else {
            1
        }

        val currentCourses = userCourseRepository.getAllUserCoursesStream().first()
        val courseToUpdate = currentCourses.find { it.groupCode == code }

        if (courseToUpdate != null) {
            userCourseRepository.updateUserCourse(
                courseToUpdate.copy(
                    fieldOfStudy = field,
                    faculty = faculty,
                    studyMode = mode,
                    semester = semester
                )
            )
        } else {
            userCourseRepository.updateUserCourse(
                tempCourse.copy(
                    fieldOfStudy = field,
                    faculty = faculty,
                    studyMode = mode,
                    semester = semester
                )
            )
        }

        when (val subgroupsRes = subgroupsDef.await()) {
            is NetworkResult.Success -> {
                _additionalSubgroupsMap.update { map ->
                    map + (code to normalizeSubgroups(subgroupsRes.data ?: emptyList()))
                }
            }

            is NetworkResult.Error -> Unit
        }

        _loadingSubgroupsFor.update { it - code }
        _triggerSave.tryEmit(Unit)
    }

    private suspend fun loadSubgroupsForAdditional(code: String) {
        _loadingSubgroupsFor.update { it + code }

        when (val res = universityRepository.getSubgroups(code)) {
            is NetworkResult.Success -> {
                _additionalSubgroupsMap.update {
                    it + (code to normalizeSubgroups(res.data ?: emptyList()))
                }
            }

            is NetworkResult.Error -> Unit
        }

        _loadingSubgroupsFor.update { it - code }
    }

    fun removeAdditionalCourse(course: UserCourseEntity) = viewModelScope.launch {
        userCourseRepository.deleteUserCourse(course)

        _additionalSubgroupsMap.update { it - course.groupCode }

        if (_activeDirection.value == course.groupCode) {
            _activeDirection.value = _selectedGroup.value
            currentSettings?.let { settings ->
                settingsRepository.insertSettings(settings.copy(activeDirectionCode = _selectedGroup.value))
            }
        }

        val updatedCourses = _additionalUserCourses.value.filter { it.groupCode != course.groupCode }
        _additionalUserCourses.value = updatedCourses

        if (updatedCourses.isEmpty() && _selectedGroup.value.isNullOrBlank()) {
            _activeDirection.value = null
            _faculty.value = ""
            _fieldOfStudy.value = ""
            _studyMode.value = ""
            _selectedSubgroups.value = emptySet()
            _mainSelectedSubgroups.value = emptySet()
        }

        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            classRepository.syncGroupClasses(course.groupCode, emptyList())
        }

        _triggerSave.tryEmit(Unit)
    }
    fun updateAdditionalSubgroup(course: UserCourseEntity, subgroup: String) {
        isEditingProfile = true

        val currentSet = parseSubgroups(course.selectedSubgroup).toMutableSet()
        if (currentSet.contains(subgroup)) currentSet.remove(subgroup) else currentSet.add(subgroup)

        viewModelScope.launch {
            userCourseRepository.updateUserCourse(
                course.copy(selectedSubgroup = currentSet.joinToString(","))
            )
            _triggerSave.tryEmit(Unit)
        }
    }

    private suspend fun performAutoSave() {
        _isLoading.value = true

        val fullName = "${_userName.value.trim()} ${_userSurname.value.trim()}".trim()
        val gender = (_selectedGender.value ?: UserGender.STUDENT).name
        val newMainGroup = _selectedGroup.value
        val mainSubgroups = normalizeSubgroupCsv(_mainSelectedSubgroups.value, hasMainGroup = !newMainGroup.isNullOrBlank())

        val newActiveDirection = _activeDirection.value ?: newMainGroup

        val newSettings = currentSettings?.copy(
            userName = fullName,
            gender = gender,
            selectedGroupCode = newMainGroup,
            selectedSubgroup = mainSubgroups,
            faculty = _mainFaculty.value,
            fieldOfStudy = _mainFieldOfStudy.value,
            studyMode = _mainStudyMode.value,
            activeDirectionCode = newActiveDirection
        ) ?: SettingsEntity(
            id = 1,
            userName = fullName,
            gender = gender,
            selectedGroupCode = newMainGroup,
            selectedSubgroup = mainSubgroups,
            faculty = _mainFaculty.value,
            fieldOfStudy = _mainFieldOfStudy.value,
            studyMode = _mainStudyMode.value,
            isAnonymous = false,
            activeDirectionCode = newActiveDirection
        )

        settingsRepository.insertSettings(newSettings)
        currentSettings = newSettings
        isEditingProfile = false

        kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
            newMainGroup?.let { groupCode ->
                universityRepository.refreshSchedule(groupCode, mainSubgroups, classRepository)
            }

            _additionalUserCourses.value.forEach { course ->
                universityRepository.refreshSchedule(course.groupCode, course.selectedSubgroup, classRepository)
            }
        }

        _isLoading.value = false
        showSavedFeedback()
    }
    private fun showSavedFeedback() = viewModelScope.launch {
        _isSavedFeedback.value = true
        delay(SAVED_FEEDBACK_MS)
        _isSavedFeedback.value = false
    }
}
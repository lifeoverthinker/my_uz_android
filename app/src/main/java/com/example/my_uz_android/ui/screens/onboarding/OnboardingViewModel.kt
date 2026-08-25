package com.example.my_uz_android.ui.screens.onboarding

/**
 * ViewModel procesu onboardingu odpowiedzialny za stan kroków i zapis konfiguracji.
 * Integruje dane użytkownika z ustawieniami aplikacji oraz źródłami uczelnianymi,
 * aby po pierwszym uruchomieniu przygotować kompletny profil pracy.
 * Zaktualizowano o wsparcie dla Wielu Kierunków.
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.SettingsEntity
import com.example.my_uz_android.data.models.UserCourseEntity
import com.example.my_uz_android.data.models.UserGender
import com.example.my_uz_android.data.repositories.ClassRepository
import com.example.my_uz_android.data.repositories.SettingsRepository
import com.example.my_uz_android.data.repositories.UniversityRepository
import com.example.my_uz_android.data.repositories.UserCourseRepository
import com.example.my_uz_android.util.NetworkResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Zarządza stanem i logiką procesu onboardingu.
 *
 * @param settingsRepository Repozytorium ustawień aplikacji.
 * @param universityRepository Repozytorium danych uczelnianych.
 * @param classRepository Repozytorium zajęć.
 * @param userCourseRepository Repozytorium obsługujące dodatkowe kierunki studiów.
 */
class OnboardingViewModel(
    private val settingsRepository: SettingsRepository,
    private val universityRepository: UniversityRepository,
    private val classRepository: ClassRepository,
    private val userCourseRepository: UserCourseRepository
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Zwiększyliśmy ilość stron z 6 na 7
    val totalPages = 7

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userSurname = MutableStateFlow("")
    val userSurname: StateFlow<String> = _userSurname.asStateFlow()

    private val _selectedGender = MutableStateFlow<UserGender?>(null)
    val selectedGender: StateFlow<UserGender?> = _selectedGender.asStateFlow()

    // --- STAN DLA KIERUNKU GŁÓWNEGO ---
    private val _groupSearchQuery = MutableStateFlow("")
    val groupSearchQuery: StateFlow<String> = _groupSearchQuery.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _availableSubgroups = MutableStateFlow<List<String>>(emptyList())
    val availableSubgroups: StateFlow<List<String>> = _availableSubgroups.asStateFlow()

    private val _selectedSubgroups = MutableStateFlow<Set<String>>(emptySet())
    val selectedSubgroups: StateFlow<Set<String>> = _selectedSubgroups.asStateFlow()

    // --- STAN DLA KIERUNKÓW DODATKOWYCH (ETAP 4) ---
    private val _additionalCourses = MutableStateFlow<List<Pair<String, Set<String>>>>(emptyList())
    val additionalCourses: StateFlow<List<Pair<String, Set<String>>>> = _additionalCourses.asStateFlow()

    private val _extraGroupSearchQuery = MutableStateFlow("")
    val extraGroupSearchQuery: StateFlow<String> = _extraGroupSearchQuery.asStateFlow()

    private val _selectedExtraGroup = MutableStateFlow<String?>(null)
    val selectedExtraGroup: StateFlow<String?> = _selectedExtraGroup.asStateFlow()

    private val _availableExtraSubgroups = MutableStateFlow<List<String>>(emptyList())
    val availableExtraSubgroups: StateFlow<List<String>> = _availableExtraSubgroups.asStateFlow()

    private val _selectedExtraSubgroups = MutableStateFlow<Set<String>>(emptySet())
    val selectedExtraSubgroups: StateFlow<Set<String>> = _selectedExtraSubgroups.asStateFlow()


    private val _allGroups = MutableStateFlow<List<String>>(emptyList())

    val filteredGroups: StateFlow<List<String>> = combine(
        _groupSearchQuery,
        _allGroups
    ) { query, allGroups ->
        if (query.isBlank()) {
            emptyList()
        } else {
            allGroups.filter { it.contains(query, ignoreCase = true) }
                .sorted()
                .take(5)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtry wyszukiwarki tylko dla drugiego kierunku
    val filteredExtraGroups: StateFlow<List<String>> = combine(
        _extraGroupSearchQuery,
        _allGroups
    ) { query, allGroups ->
        if (query.isBlank()) emptyList()
        else allGroups.filter { it.contains(query, ignoreCase = true) }.sorted().take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            when (val result = universityRepository.getGroupCodes()) {
                is NetworkResult.Success -> {
                    val groups = (result.data ?: emptyList())
                        .filter { !it.isNullOrBlank() && it != "null" }
                        .sorted()
                    _allGroups.value = groups
                }
                else -> {
                    _allGroups.value = emptyList()
                }
            }
        }
    }

    /** Przechodzi do następnego kroku onboardingu. */
    fun onNextClick() {
        if (_currentPage.value < totalPages - 1) {
            _currentPage.value += 1
        }
    }

    /** Wraca do poprzedniego kroku onboardingu. */
    fun onBackClick() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
        }
    }

    /** Ustawia imię użytkownika w stanie formularza. */
    fun setUserName(name: String) { _userName.value = name }

    /** Ustawia nazwisko użytkownika w stanie formularza. */
    fun setUserSurname(surname: String) { _userSurname.value = surname }

    /** Ustawia formę zwrotu użytkownika. */
    fun setGender(gender: UserGender) { _selectedGender.value = gender }

    // --- LOGIKA GŁÓWNEGO KIERUNKU ---
    fun setGroupSearchQuery(query: String) {
        _groupSearchQuery.value = query
        if (query.isBlank()) {
            _selectedGroup.value = null
            _availableSubgroups.value = emptyList()
            _selectedSubgroups.value = emptySet()
        }
    }

    fun selectGroup(group: String) {
        _selectedGroup.value = group
        _groupSearchQuery.value = group
        _selectedSubgroups.value = emptySet()
        loadSubgroupsForGroup(group)
    }

    private fun loadSubgroupsForGroup(group: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = universityRepository.getSubgroups(group)) {
                is NetworkResult.Success -> {
                    val subgroups = (result.data ?: emptyList())
                        .filter { !it.isNullOrBlank() && it != "null" }
                        .sorted()
                    _availableSubgroups.value = subgroups
                }
                else -> _availableSubgroups.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun toggleSubgroup(subgroup: String) {
        val current = _selectedSubgroups.value.toMutableSet()
        if (current.contains(subgroup)) current.remove(subgroup) else current.add(subgroup)
        _selectedSubgroups.value = current
    }


    // --- LOGIKA DODATKOWEGO KIERUNKU ---
    fun setExtraGroupSearchQuery(query: String) {
        _extraGroupSearchQuery.value = query
        if (query.isBlank()) {
            _selectedExtraGroup.value = null
            _availableExtraSubgroups.value = emptyList()
            _selectedExtraSubgroups.value = emptySet()
        }
    }

    fun selectExtraGroup(group: String) {
        _selectedExtraGroup.value = group
        _extraGroupSearchQuery.value = group
        _selectedExtraSubgroups.value = emptySet()
        loadSubgroupsForExtraGroup(group)
    }

    private fun loadSubgroupsForExtraGroup(group: String) {
        viewModelScope.launch {
            _isLoading.value = true
            when (val result = universityRepository.getSubgroups(group)) {
                is NetworkResult.Success -> {
                    val subgroups = (result.data ?: emptyList())
                        .filter { !it.isNullOrBlank() && it != "null" }
                        .sorted()
                    _availableExtraSubgroups.value = subgroups
                }
                else -> _availableExtraSubgroups.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun toggleExtraSubgroup(subgroup: String) {
        val current = _selectedExtraSubgroups.value.toMutableSet()
        if (current.contains(subgroup)) current.remove(subgroup) else current.add(subgroup)
        _selectedExtraSubgroups.value = current
    }

    fun confirmAddExtraCourse() {
        val group = _selectedExtraGroup.value ?: return
        val subgroups = _selectedExtraSubgroups.value
        // Nadpisujemy wpis dla tej samej grupy, żeby uniknąć duplikatów.
        _additionalCourses.value = _additionalCourses.value
            .filterNot { it.first == group } + (group to subgroups)
        setExtraGroupSearchQuery("") // Czyszczenie, by dodać kolejny!
    }

    /**
     * Przechodzi dalej z opcjonalnego kroku dodatkowych kierunków.
     * Jeśli użytkownik ma rozpoczęty wybór grupy, zapisujemy go automatycznie,
     * żeby nie stracić danych po kliknięciu "Dalej".
     */
    fun onAdditionalCoursesNextClick() {
        if (!_selectedExtraGroup.value.isNullOrBlank()) {
            confirmAddExtraCourse()
        }
        onNextClick()
    }

    fun removeExtraCourse(group: String) {
        _additionalCourses.value = _additionalCourses.value.filter { it.first != group }
    }


    // --- ZAPIS DANYCH ---
    fun skipOnboarding(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true

            val currentSettings = settingsRepository.getSettingsStream().firstOrNull()

            val guestSettings = SettingsEntity(
                id = currentSettings?.id ?: 0,
                userName = "Gość",
                isAnonymous = true,
                selectedGroupCode = null,
                activeDirectionCode = null,
                selectedSubgroup = null,
                gender = null,
                isFirstRun = false,
                isDarkMode = currentSettings?.isDarkMode ?: false
            )

            settingsRepository.insertSettings(guestSettings)

            _isLoading.value = false
            onSuccess()
        }
    }

    fun saveOnboardingData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var fetchedFaculty: String? = null
                var fetchedFieldOfStudy: String? = null
                var fetchedStudyMode: String? = null

                val groupCode = _selectedGroup.value
                if (!groupCode.isNullOrEmpty()) {
                    val detailsResult = runCatching { universityRepository.getGroupDetails(groupCode) }.getOrNull()
                    if (detailsResult is NetworkResult.Success && detailsResult.data != null) {
                        val details = detailsResult.data
                        fetchedFaculty = details.fieldInfo?.faculty
                        fetchedFieldOfStudy = details.fieldInfo?.name
                        fetchedStudyMode = details.studyMode
                    }
                }

                val currentSettings = settingsRepository.getSettingsStream().firstOrNull()
                val fullName = "${_userName.value.trim()} ${_userSurname.value.trim()}".trim()
                val genderString = (_selectedGender.value ?: UserGender.STUDENT).name
                val subgroupsString = _selectedSubgroups.value.joinToString(",")

                val newSettings = SettingsEntity(
                    id = currentSettings?.id ?: 0,
                    userName = fullName,
                    isAnonymous = false,
                    selectedGroupCode = groupCode,
                    activeDirectionCode = groupCode,
                    selectedSubgroup = subgroupsString,
                    gender = genderString,
                    isFirstRun = false,
                    isDarkMode = currentSettings?.isDarkMode ?: false,
                    faculty = fetchedFaculty,
                    fieldOfStudy = fetchedFieldOfStudy,
                    studyMode = fetchedStudyMode
                )
                settingsRepository.insertSettings(newSettings)

                _additionalCourses.value.forEach { (courseGroup, subgroups) ->
                    var extraField: String? = null
                    val detailsResult = runCatching { universityRepository.getGroupDetails(courseGroup) }.getOrNull()
                    if (detailsResult is NetworkResult.Success && detailsResult.data != null) {
                        extraField = detailsResult.data.fieldInfo?.name
                    }

                    userCourseRepository.insertUserCourse(
                        UserCourseEntity(
                            groupCode = courseGroup,
                            fieldOfStudy = extraField,
                            semester = currentSettings?.currentSemester ?: 1,
                            selectedSubgroup = subgroups.joinToString(",")
                        )
                    )
                }
                onSuccess()
            } catch (_: Exception) {
                // Logowanie błędu
            } finally {
                _isLoading.value = false
            }
        }
    }
}
package com.example.my_uz_android.ui.screens.calendar.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.FavoriteEntity
import com.example.my_uz_android.data.repositories.FavoritesRepository
import com.example.my_uz_android.data.repositories.GroupSearchMeta
import com.example.my_uz_android.data.repositories.TeacherDetailsDto
import com.example.my_uz_android.data.repositories.UniversityRepository
import com.example.my_uz_android.util.NetworkResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class SearchUiState(
    val searchQuery: String = "",
    val searchResults: List<SearchResultItem> = emptyList(),
    val isLoading: Boolean = false
)

data class SearchResultItem(
    val name: String,
    val type: String, // "group" | "teacher"
    val isFavorite: Boolean = false,
    val studyField: String? = null,
    val email: String? = null,
    val institute: String? = null
)

private data class GroupSearchIndexItemScheduleSearchVM(
    val code: String,
    val primaryStudyField: String?,
    val searchableTexts: List<String>
)

@OptIn(FlowPreview::class)
class ScheduleSearchViewModel(
    private val universityRepository: UniversityRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var allGroupsIndex: List<GroupSearchIndexItemScheduleSearchVM> = emptyList()
    private var allTeachers: List<TeacherDetailsDto> = emptyList()

    private val queryFlow = MutableStateFlow("")

    private val remoteGroupsCache = mutableMapOf<String, List<String>>()

    init {
        observeFavoritesScheduleSearchVM()
        preloadSearchDataScheduleSearchVM()
        observeQueryChangesScheduleSearchVM()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        queryFlow.value = query
    }

    private fun observeFavoritesScheduleSearchVM() {
        viewModelScope.launch {
            favoritesRepository.favoritesStream.collect { favorites ->
                val favoriteKeys = favorites.map { it.resourceId to it.type }.toSet()
                _uiState.update { state ->
                    state.copy(
                        searchResults = state.searchResults.map { result ->
                            result.copy(isFavorite = favoriteKeys.contains(result.name to result.type))
                        }
                    )
                }
            }
        }
    }

    private fun preloadSearchDataScheduleSearchVM() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val groupsDeferred = async { universityRepository.getGroupsForSearch() }
            val teachersDeferred = async { universityRepository.getAllTeachersWithDetails() }

            val groupsRes = groupsDeferred.await()
            val teachersRes = teachersDeferred.await()

            if (groupsRes is NetworkResult.Success) {
                val groups = (groupsRes.data ?: emptyList())
                    .filter { it.code.isNotBlank() }
                    .distinctBy { it.code }

                allGroupsIndex = groups.map { group ->
                    GroupSearchIndexItemScheduleSearchVM(
                        code = group.code,
                        primaryStudyField = group.studyFields.firstOrNull { it.isNotBlank() },
                        searchableTexts = buildGroupSearchableTextsScheduleSearchVM(group)
                    )
                }
            }

            if (teachersRes is NetworkResult.Success) {
                allTeachers = (teachersRes.data ?: emptyList())
                    .filter { !it.name.isNullOrBlank() }
            }

            _uiState.update { it.copy(isLoading = false) }

            val existingQuery = _uiState.value.searchQuery
            if (existingQuery.isNotBlank()) {
                performSearchScheduleSearchVM(existingQuery)
            }
        }
    }

    private fun observeQueryChangesScheduleSearchVM() {
        viewModelScope.launch {
            queryFlow
                .debounce(220)
                .distinctUntilChanged()
                .collect { query ->
                    performSearchScheduleSearchVM(query)
                }
        }
    }

    private suspend fun performSearchScheduleSearchVM(query: String) {
        val trimmedQuery = query.trim()
        val words = trimmedQuery
            .normalizeForSearchScheduleSearchVM()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        if (words.isEmpty()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }

        val favorites = favoritesRepository.favoritesStream.first()
        val favoriteKeys = favorites.map { it.resourceId to it.type }.toSet()

        val localMatchedGroups = allGroupsIndex
            .asSequence()
            .filter { groupItem ->
                // Pozwala mieszać kryteria: np. fragment kodu grupy + fragment kierunku.
                words.all { word ->
                    groupItem.searchableTexts.any { searchable ->
                        matchesQueryScheduleSearchVM(searchable, listOf(word))
                    }
                }
            }
            .map { it.code }
            .toMutableSet()

        val remoteMatchedGroups = fetchRemoteGroupsScheduleSearchVM(trimmedQuery)

        val mergedGroupCodes = linkedSetOf<String>().apply {
            addAll(localMatchedGroups)
            addAll(remoteMatchedGroups.filter { it.isNotBlank() })
        }

        val studyFieldByCode = allGroupsIndex.associate { it.code to it.primaryStudyField }

        val matchedGroups = mergedGroupCodes
            .asSequence()
            .take(30)
            .map { groupCode ->
                SearchResultItem(
                    name = groupCode,
                    type = "group",
                    isFavorite = favoriteKeys.contains(groupCode to "group"),
                    studyField = studyFieldByCode[groupCode]
                )
            }

        val matchedTeachers = allTeachers
            .asSequence()
            .filter { teacher ->
                val safeName = teacher.name ?: return@filter false
                matchesQueryScheduleSearchVM(safeName, words)
            }
            .take(30)
            .mapNotNull { teacher ->
                val safeName = teacher.name ?: return@mapNotNull null
                SearchResultItem(
                    name = safeName,
                    type = "teacher",
                    isFavorite = favoriteKeys.contains(safeName to "teacher"),
                    email = teacher.email,
                    institute = teacher.institute
                )
            }

        _uiState.update {
            it.copy(searchResults = (matchedGroups + matchedTeachers).toList())
        }
    }

    private suspend fun fetchRemoteGroupsScheduleSearchVM(query: String): List<String> {
        val cacheKey = query.lowercase()
        remoteGroupsCache[cacheKey]?.let { return it }

        val result = when (val remote = universityRepository.searchGroups(query)) {
            is NetworkResult.Success -> (remote.data ?: emptyList())
            is NetworkResult.Error -> emptyList()
        }

        remoteGroupsCache[cacheKey] = result
        return result
    }

    private fun buildGroupSearchableTextsScheduleSearchVM(group: GroupSearchMeta): List<String> {
        val normalizedCode = group.code.trim()
        val compact = normalizedCode.replace(Regex("[^\\p{L}\\p{N}]"), " ")

        return buildList {
            add(normalizedCode)
            add(compact)
            // Dodajemy nazwy kierunków do indeksu wyszukiwania
            group.studyFields.forEach { field ->
                if (field.isNotBlank()) {
                    add(field.trim())
                }
            }
        }.distinct()
    }


    private fun matchesQueryScheduleSearchVM(candidate: String, words: List<String>): Boolean {
        val normalizedCandidate = candidate.normalizeForSearchScheduleSearchVM()
        val compactCandidate = normalizedCandidate.compactForCodeSearchScheduleSearchVM()

        return words.all { word ->
            val compactWord = word.compactForCodeSearchScheduleSearchVM()
            normalizedCandidate.contains(word) || compactCandidate.contains(compactWord)
        }
    }

    private fun String.normalizeForSearchScheduleSearchVM(): String {
        val manual = this.lowercase()
            .replace("ł", "l")
            .replace("ą", "a")
            .replace("ć", "c")
            .replace("ę", "e")
            .replace("ń", "n")
            .replace("ó", "o")
            .replace("ś", "s")
            .replace("ź", "z")
            .replace("ż", "z")

        val normalized = Normalizer.normalize(manual, Normalizer.Form.NFD)
        return "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(normalized, "")
    }

    private fun String.compactForCodeSearchScheduleSearchVM(): String {
        return this.filter { it.isLetterOrDigit() }
    }

    fun toggleFavorite(result: SearchResultItem) {
        viewModelScope.launch {
            if (result.isFavorite) {
                favoritesRepository.deleteFavoriteByResourceIdAndType(result.name, result.type)
            } else {
                favoritesRepository.insertFavoriteIfAbsent(
                    FavoriteEntity(
                        name = result.name,
                        type = result.type,
                        resourceId = result.name
                    )
                )
            }
        }
    }
}
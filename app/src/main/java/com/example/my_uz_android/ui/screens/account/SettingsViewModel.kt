package com.example.my_uz_android.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.my_uz_android.data.models.*
import com.example.my_uz_android.data.repositories.*
import com.example.my_uz_android.ui.theme.ClassColorPalette
import com.example.my_uz_android.util.BackupManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private const val SAVE_FEEDBACK_DURATION_MS = 1200L

/**
 * Stan UI ekranu ustawień.
 */
data class SettingsUiState(
    val settings: SettingsEntity? = null,
    val uniqueClassTypes: List<String> = emptyList(),
    val classColorMap: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val importMessage: String? = null,
    val backupPreview: ManualBackupData? = null,
    val showImportDialog: Boolean = false,
    val isSaved: Boolean = false
)

/**
 * ViewModel ekranu ustawień.
 *
 * Zarządza zapisem preferencji aplikacji (motyw, język, powiadomienia, kolory zajęć)
 * oraz operacjami backup/import.
 */
class SettingsViewModel(
    private val backupManager: BackupManager,
    private val settingsRepository: SettingsRepository,
    private val universityRepository: UniversityRepository,
    private val classRepository: ClassRepository,
    private val tasksRepository: TasksRepository,
    private val gradesRepository: GradesRepository,
    private val absenceRepository: AbsenceRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val gson = Gson()

    init {
        observeSettingsAndClassTypes()
    }

    private fun observeSettingsAndClassTypes() {
        viewModelScope.launch {
            combine(
                settingsRepository.getSettingsStream(),
                classRepository.getAllClassesStream()
            ) { settings, classes ->
                val uniqueTypes = classes
                    .asSequence()
                    .map { it.classType.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .toList()

                settings to uniqueTypes
            }
                .distinctUntilChanged()
                .collect { (settings, uniqueTypes) ->
                    val parsedColorMap = parseClassColorMap(settings?.classColorsJson)
                    val ensuredMap = ensureColorMapForTypes(
                        settings = settings,
                        uniqueTypes = uniqueTypes,
                        currentMap = parsedColorMap
                    )

                    _uiState.update { state ->
                        state.copy(
                            settings = settings,
                            uniqueClassTypes = uniqueTypes,
                            classColorMap = ensuredMap,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun parseClassColorMap(json: String?): Map<String, Int> {
        return try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson<Map<String, Int>>(json ?: "{}", type) ?: emptyMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private suspend fun ensureColorMapForTypes(
        settings: SettingsEntity?,
        uniqueTypes: List<String>,
        currentMap: Map<String, Int>
    ): Map<String, Int> {
        if (settings == null || uniqueTypes.isEmpty()) return currentMap

        val normalizedMap = currentMap.toMutableMap()
        var changed = false

        uniqueTypes.forEach { type ->
            if (!normalizedMap.containsKey(type)) {
                normalizedMap[type] = abs(type.hashCode()) % ClassColorPalette.size
                changed = true
            }
        }

        if (changed) {
            settingsRepository.insertSettings(settings.copy(classColorsJson = gson.toJson(normalizedMap)))
        }

        return normalizedMap
    }

    private fun triggerSaveFeedback() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaved = true) }
            delay(SAVE_FEEDBACK_DURATION_MS)
            _uiState.update { it.copy(isSaved = false) }
        }
    }

    private fun updateSettings(transform: (SettingsEntity) -> SettingsEntity) {
        val current = _uiState.value.settings ?: return
        viewModelScope.launch {
            settingsRepository.insertSettings(transform(current))
            triggerSaveFeedback()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        updateSettings { it.copy(themeMode = mode.name) }
    }

    /**
     * Ustawia język aplikacji, zapisując wyłącznie wspierane wartości `pl` lub `en`.
     */
    fun setAppLanguage(languageCode: String) {
        val normalizedCode = when (languageCode) {
            "en" -> "en"
            else -> "pl"
        }
        updateSettings { it.copy(appLanguage = normalizedCode) }
    }

    fun updateClassColor(classType: String, colorIndex: Int) {
        val current = _uiState.value.settings ?: return
        val currentMap = _uiState.value.classColorMap.toMutableMap()
        currentMap[classType.trim()] = colorIndex

        viewModelScope.launch {
            settingsRepository.insertSettings(current.copy(classColorsJson = gson.toJson(currentMap)))
            triggerSaveFeedback()
        }
    }

    // Włączenie/wyłączenie powiadomień głównych
    fun toggleNotifications(enabled: Boolean) {
        updateSettings { it.copy(notificationsEnabled = enabled) }
    }

    // Włączenie/wyłączenie przypomnień 15 minut przed zajęciami
    fun toggleClassesNotifications(enabled: Boolean) {
        updateSettings { it.copy(notificationsClasses = enabled) }
    }

    suspend fun createBackupJson(selectedTypes: Set<BackupDataType>): String =
        withContext(Dispatchers.IO) {
            val backup = ManualBackupData(
                settings = if (BackupDataType.SETTINGS in selectedTypes) settingsRepository.getSettingsStream().first() else null,
                classes = if (BackupDataType.CLASSES in selectedTypes) classRepository.getAllClassesStream().first() else emptyList(),
                tasks = if (BackupDataType.TASKS in selectedTypes) tasksRepository.getAllTasks().first() else emptyList(),
                grades = if (BackupDataType.GRADES in selectedTypes) gradesRepository.getAllGradesStream().first() else emptyList(),
                absences = if (BackupDataType.ABSENCES in selectedTypes) absenceRepository.getAllAbsencesStream().first() else emptyList()
            )
            gson.toJson(backup)
        }

    fun previewBackupFile(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, importMessage = null) }
            try {
                val data = gson.fromJson(jsonString, ManualBackupData::class.java)
                _uiState.update {
                    it.copy(
                        backupPreview = data,
                        showImportDialog = true,
                        isLoading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        importMessage = "Błędny format pliku backupu",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun confirmImport(selectedTypes: Set<BackupDataType>) {
        val data = _uiState.value.backupPreview ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    showImportDialog = false,
                    importMessage = null
                )
            }

            try {
                if (BackupDataType.SETTINGS in selectedTypes) {
                    settingsRepository.clearSettings()
                    data.settings?.let { settingsRepository.insertSettings(it) }
                }

                if (BackupDataType.CLASSES in selectedTypes) {
                    classRepository.deleteAllClasses()
                    classRepository.insertClasses(data.classes)
                }

                if (BackupDataType.TASKS in selectedTypes) {
                    tasksRepository.deleteAllTasks()
                    tasksRepository.insertTasks(data.tasks)
                }

                if (BackupDataType.GRADES in selectedTypes) {
                    gradesRepository.deleteAllGrades()
                    gradesRepository.insertGrades(data.grades)
                }

                if (BackupDataType.ABSENCES in selectedTypes) {
                    absenceRepository.deleteAllAbsences()
                    absenceRepository.insertAbsences(data.absences)
                }

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            importMessage = "Import zakończony pomyślnie",
                            backupPreview = null
                        )
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            importMessage = "Błąd importu danych"
                        )
                    }
                }
            }
        }
    }

    fun cancelImport() {
        _uiState.update { it.copy(showImportDialog = false, backupPreview = null) }
    }

    fun exportDatabase(onResult: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { backupManager.exportData() }
                .onSuccess { onResult(it) }
        }
    }

    fun importDatabase(json: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { backupManager.importData(json) }
                .onSuccess { onSuccess() }
                .onFailure { onError(it.localizedMessage ?: "Nieznany błąd podczas importu") }
        }
    }
}
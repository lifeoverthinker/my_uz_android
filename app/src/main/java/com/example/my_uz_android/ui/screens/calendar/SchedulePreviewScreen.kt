package com.example.my_uz_android.ui.screens.calendar

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.ui.AppViewModelProvider
import com.example.my_uz_android.ui.components.PreviewTopAppBar
import com.example.my_uz_android.ui.components.SubgroupFilterDialog
import com.example.my_uz_android.ui.components.TeacherInfoDialog
import com.example.my_uz_android.ui.screens.calendar.components.ScheduleView
import com.example.my_uz_android.ui.theme.MyUZTheme
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.atStartOfMonth
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.abs
import androidx.compose.foundation.layout.height
import androidx.compose.ui.res.stringResource

private fun Modifier.calendarDaySwipeGestureSchedulePreviewScreen(
    selectedDate: LocalDate,
    isMonthView: Boolean,
    density: androidx.compose.ui.unit.Density,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit
): Modifier {
    return pointerInput(selectedDate, isMonthView) {
        val swipeThresholdPx = with(density) { 72.dp.toPx() }
        var accumulatedDx by mutableFloatStateOf(0f)
        var accumulatedDy by mutableFloatStateOf(0f)

        detectHorizontalDragGestures(
            onDragStart = {
                accumulatedDx = 0f
                accumulatedDy = 0f
            },
            onHorizontalDrag = { change, dragAmount ->
                accumulatedDx += dragAmount
                accumulatedDy += abs(change.position.y - change.previousPosition.y)
            },
            onDragEnd = {
                val isHorizontalIntent = abs(accumulatedDx) > accumulatedDy
                val passedThreshold = abs(accumulatedDx) >= swipeThresholdPx

                if (isHorizontalIntent && passedThreshold) {
                    if (accumulatedDx < 0) onSwipeNext() else onSwipePrevious()
                }
            }
        )
    }
}

@Composable
fun SchedulePreviewScreen(
    navController: NavController,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onClassClick: (ClassEntity, Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val classes = uiState.visibleClasses
    val classColorMap = uiState.classColorMap
    val planName = uiState.selectedPlanName

    val type = when (val source = uiState.currentSource) {
        is ScheduleSource.Preview -> source.type
        is ScheduleSource.Favorite -> source.type
        else -> "group"
    }
    val isFavorite = uiState.favorites.any { it.resourceId == planName && it.type == type }
    val isTeacher = type == "teacher"

    val teacherData = remember(classes) {
        classes.firstOrNull { !it.teacherEmail.isNullOrBlank() }
            ?: classes.firstOrNull { !it.teacherInstitute.isNullOrBlank() }
            ?: classes.firstOrNull { it.teacherName?.isNotBlank() == true }
    }

    val isDark = when (uiState.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> MaterialTheme.colorScheme.surface.luminance() < 0.5f
    }

    SchedulePreviewScreenContent(
        classes = classes,
        tasks = uiState.tasks,
        classColorMap = classColorMap,
        planName = planName,
        isFavorite = isFavorite,
        isTeacher = isTeacher,
        teacherData = teacherData,
        isLoading = uiState.isLoading,
        isDarkMode = isDark,
        onBackClick = { navController.popBackStack() },
        onFavoriteClick = {
            viewModel.toggleFavorite(
                planName,
                if (isTeacher) "teacher" else "group"
            )
        },
        onClassClick = { classEntity ->
            onClassClick(classEntity, isTeacher)
        }
    )
}

@Composable
fun SchedulePreviewScreenContent(
    classes: List<ClassEntity>,
    tasks: List<TaskEntity>,
    classColorMap: Map<String, Int>,
    planName: String,
    isFavorite: Boolean,
    isTeacher: Boolean,
    teacherData: ClassEntity?,
    isLoading: Boolean,
    isDarkMode: Boolean,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onClassClick: (ClassEntity) -> Unit
) {
    var showTeacherInfo by remember { mutableStateOf(false) }
    var showSubgroupFilter by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val emailCopiedMessage = stringResource(R.string.dialog_teacher_email_copied)

    val availableSubgroups = remember(classes) {
        classes.map { it.subgroup ?: "" }.distinct().sorted()
    }

    var selectedSubgroups by rememberSaveable(
        planName,
        isTeacher,
        stateSaver = listSaver<Set<String>, String>(
            save = { it.toList() },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(availableSubgroups) {
        val availableSet = availableSubgroups.toSet()
        val keptSelection = selectedSubgroups.intersect(availableSet)
        val reconciledSelection = when {
            availableSet.isEmpty() -> emptySet()
            keptSelection.isEmpty() -> availableSet
            else -> keptSelection
        }

        if (reconciledSelection != selectedSubgroups) {
            selectedSubgroups = reconciledSelection
        }
    }

    val filteredClasses = remember(classes, selectedSubgroups) {
        if (selectedSubgroups.isEmpty()) classes
        else classes.filter { selectedSubgroups.contains(it.subgroup ?: "") }
    }

    val currentDate = remember { LocalDate.now(ZoneId.of("Europe/Warsaw")) }
    val currentMonth = remember { YearMonth.now(ZoneId.of("Europe/Warsaw")) }
    val startMonth = remember { currentMonth.minusMonths(24) }
    val endMonth = remember { currentMonth.plusMonths(24) }

    var selectedDateEpochDay by rememberSaveable { mutableStateOf(currentDate.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay)
    var isMonthView by rememberSaveable { mutableStateOf(false) }
    var swipeDirection by remember { mutableStateOf<DaySwipeDirection?>(null) }

    val weekState = rememberWeekCalendarState(
        startDate = startMonth.atStartOfMonth(),
        endDate = endMonth.atEndOfMonth(),
        firstDayOfWeek = DayOfWeek.MONDAY
    )
    val monthState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstDayOfWeek = DayOfWeek.MONDAY,
        firstVisibleMonth = currentMonth
    )

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    fun navigateDay(offsetDays: Long, direction: DaySwipeDirection) {
        val newDate = selectedDate.plusDays(offsetDays)
        if (newDate == selectedDate) return

        swipeDirection = direction
        selectedDateEpochDay = newDate.toEpochDay()

        scope.launch {
            if (isMonthView) monthState.scrollToMonth(YearMonth.from(newDate))
            else weekState.scrollToWeek(newDate)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            PreviewTopAppBar(
                title = if (isTeacher) stringResource(R.string.preview_teacher_plan) else stringResource(
                    R.string.preview_group_plan
                ),
                subtitle = if (isTeacher) (teacherData?.teacherName ?: planName) else planName,
                isFavorite = isFavorite,
                onBackClick = onBackClick,
                onFavoriteClick = onFavoriteClick,
                actionIcon = if (isTeacher) R.drawable.ic_info_circle else R.drawable.ic_filter,
                onActionClick = {
                    if (isTeacher) showTeacherInfo = true else showSubgroupFilter = true
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Crossfade(
            targetState = isLoading,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .calendarDaySwipeGestureSchedulePreviewScreen(
                    selectedDate = selectedDate,
                    isMonthView = isMonthView,
                    density = density,
                    onSwipeNext = { navigateDay(1, DaySwipeDirection.NEXT) },
                    onSwipePrevious = { navigateDay(-1, DaySwipeDirection.PREVIOUS) }
                ),
            label = "ScheduleLoadingTransition"
        ) { loading ->
            when {
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                classes.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_info_circle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.calendar_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.preview_no_classes_msg),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    ScheduleView(
                        calendarState = monthState,
                        weekState = weekState,
                        selectedDate = selectedDate,
                        isMonthView = isMonthView,
                        swipeDirection = swipeDirection,
                        onDateSelected = { date ->
                            swipeDirection = null
                            selectedDateEpochDay = date.toEpochDay()
                            if (isMonthView) {
                                isMonthView = false
                                scope.launch { weekState.scrollToWeek(date) }
                            }
                        },
                        onToggleView = {
                            isMonthView = !isMonthView
                            scope.launch {
                                if (isMonthView) monthState.scrollToMonth(
                                    YearMonth.from(
                                        selectedDate
                                    )
                                )
                                else weekState.scrollToWeek(selectedDate)
                            }
                        },
                        classes = filteredClasses,
                        tasks = emptyList(),
                        classColorMap = classColorMap,
                        onClassClick = onClassClick,
                        isDarkMode = isDarkMode,
                        isTeacherPlan = isTeacher,
                        showHeader = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showTeacherInfo) {
        TeacherInfoDialog(
            onDismiss = { showTeacherInfo = false },
            fullName = teacherData?.teacherName ?: planName,
            department = teacherData?.teacherInstitute
                ?: stringResource(R.string.preview_no_department),
            email = teacherData?.teacherEmail.orEmpty(),
            onEmailCopied = {
                scope.launch {
                    snackbarHostState.showSnackbar(message = emailCopiedMessage)
                }
            }
        )
    }

    if (showSubgroupFilter) {
        SubgroupFilterDialog(
            subgroups = availableSubgroups,
            selectedSubgroups = selectedSubgroups,
            onDismiss = { showSubgroupFilter = false },
            onSelectionChange = { selectedSubgroups = it }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SchedulePreviewScreenPreview() {
    MyUZTheme {
        SchedulePreviewScreenContent(
            classes = listOf(
                ClassEntity(
                    id = 1,
                    subjectName = "Programowanie Obiektowe",
                    classType = "Wykład",
                    teacherName = "Dr inż. Jan Kowalski",
                    room = "A-2 101",
                    startTime = "08:15",
                    endTime = "09:45",
                    date = LocalDate.now().toString(),
                    dayOfWeek = 1,
                    groupCode = "12IN",
                    subgroup = "L1"
                )
            ),
            tasks = emptyList(),
            classColorMap = emptyMap(),
            planName = "Dr inż. Jan Kowalski",
            isFavorite = false,
            isTeacher = true,
            teacherData = null,
            isLoading = false,
            isDarkMode = false,
            onBackClick = {},
            onFavoriteClick = {},
            onClassClick = {}
        )
    }
}
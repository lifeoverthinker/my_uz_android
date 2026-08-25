package com.example.my_uz_android.ui.screens.calendar

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.data.models.UserCourseEntity
import com.example.my_uz_android.ui.components.CalendarTopAppBar
import com.example.my_uz_android.ui.components.TopBarActionIcon
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
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private fun Modifier.calendarDaySwipeGestureCalendarScreen(
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
fun CalendarScreen(
    onOpenDrawer: () -> Unit,
    onSearchClick: () -> Unit,
    onTasksClick: () -> Unit,
    onAccountClick: () -> Unit,
    onClassClick: (ClassEntity) -> Unit,
    onShowPreview: () -> Unit,
    viewModel: CalendarViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.selectMyPlan(forceRefresh = false)
    }

    CalendarScreenContent(
        uiState = uiState,
        onOpenDrawer = onOpenDrawer,
        onSearchClick = onSearchClick,
        onAccountClick = onAccountClick,
        onClassClick = onClassClick,
        onDateSelected = viewModel::setSelectedDate,
        onToggleMonthView = viewModel::setMonthView,
        onToggleGroupVisibility = viewModel::toggleGroupVisibility,
        onRefresh = viewModel::refreshMyPlan
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreenContent(
    uiState: CalendarUiState,
    onOpenDrawer: () -> Unit,
    onSearchClick: () -> Unit,
    onAccountClick: () -> Unit,
    onClassClick: (ClassEntity) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onToggleMonthView: (Boolean) -> Unit,
    onToggleGroupVisibility: (String) -> Unit,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val selectedDate = uiState.selectedDate
    val isMonthView = uiState.isMonthView

    val currentMonth = remember { YearMonth.from(selectedDate) }
    val startMonth = remember { currentMonth.minusMonths(24) }
    val endMonth = remember { currentMonth.plusMonths(24) }

    val weekState = rememberWeekCalendarState(
        startDate = startMonth.atStartOfMonth(),
        endDate = endMonth.atEndOfMonth(),
        firstVisibleWeekDate = selectedDate,
        firstDayOfWeek = DayOfWeek.MONDAY
    )
    val monthState = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = DayOfWeek.MONDAY
    )

    var swipeDirection by remember { mutableStateOf<DaySwipeDirection?>(null) }
    var isFilterExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedDate, isMonthView) {
        if (isMonthView) {
            val currentVisibleMonth = monthState.firstVisibleMonth.yearMonth
            if (YearMonth.from(selectedDate) != currentVisibleMonth) {
                monthState.animateScrollToMonth(YearMonth.from(selectedDate))
            }
        } else {
            val currentWeekDays = weekState.firstVisibleWeek.days
            if (currentWeekDays.isNotEmpty() && currentWeekDays.none { it.date == selectedDate }) {
                weekState.animateScrollToWeek(selectedDate)
            }
        }
    }

    fun navigateDay(offsetDays: Long, direction: DaySwipeDirection) {
        val newDate = selectedDate.plusDays(offsetDays)
        if (newDate == selectedDate) return

        swipeDirection = direction
        onDateSelected(newDate)

        scope.launch {
            if (isMonthView) monthState.scrollToMonth(YearMonth.from(newDate))
            else weekState.scrollToWeek(newDate)
        }
    }

    val visibleMonth = if (isMonthView) {
        monthState.firstVisibleMonth.yearMonth
    } else {
        weekState.firstVisibleWeek.days.firstOrNull()?.date?.let { YearMonth.from(it) } ?: YearMonth.from(selectedDate)
    }

    val currentLocale = Locale.getDefault()
    val monthName = visibleMonth.month
        .getDisplayName(TextStyle.FULL_STANDALONE, currentLocale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString() }

    val currentRealWorldYear = YearMonth.now(ZoneId.of("Europe/Warsaw")).year
    val calendarTitle = if (visibleMonth.year == currentRealWorldYear) monthName else "$monthName ${visibleMonth.year}"

    val isDark = when (uiState.themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> MaterialTheme.colorScheme.surface.luminance() < 0.5f
    }

    Scaffold(
        topBar = {
            CalendarTopAppBar(
                title = calendarTitle,
                isExpanded = isMonthView,
                onNavigationClick = onOpenDrawer,
                onTitleClick = { onToggleMonthView(!isMonthView) },
                actions = {
                    if (uiState.userCourses.size > 1) {
                        Box {
                            TopBarActionIcon(
                                icon = R.drawable.ic_filter,
                                onClick = { isFilterExpanded = true },
                                isFilled = true
                            )

                            DropdownMenu(
                                expanded = isFilterExpanded,
                                onDismissRequest = { isFilterExpanded = false }
                            ) {
                                uiState.userCourses.forEach { course ->
                                    val normalizedCode = course.groupCode.trim().lowercase()
                                    val isSelected = uiState.selectedGroupCodes.contains(normalizedCode)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = course.fieldOfStudy ?: course.groupCode,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        leadingIcon = {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = null
                                            )
                                        },
                                        onClick = { onToggleGroupVisibility(course.groupCode) }
                                    )
                                }
                            }
                        }
                    }

                    TopBarActionIcon(
                        icon = R.drawable.ic_search,
                        onClick = onSearchClick,
                        isFilled = true
                    )
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .calendarDaySwipeGestureCalendarScreen(
                    selectedDate = selectedDate,
                    isMonthView = isMonthView,
                    density = density,
                    onSwipeNext = { navigateDay(1, DaySwipeDirection.NEXT) },
                    onSwipePrevious = { navigateDay(-1, DaySwipeDirection.PREVIOUS) }
                )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ScheduleView(
                        calendarState = monthState,
                        weekState = weekState,
                        selectedDate = selectedDate,
                        isMonthView = isMonthView,
                        swipeDirection = swipeDirection,
                        onDateSelected = { date ->
                            swipeDirection = null
                            onDateSelected(date)
                        },
                        onToggleView = { onToggleMonthView(!isMonthView) },
                        classes = uiState.visibleClasses,
                        tasks = uiState.tasks,
                        classColorMap = uiState.classColorMap,
                        onClassClick = onClassClick,
                        isDarkMode = isDark,
                        modifier = Modifier.weight(1f),
                        showHeader = false
                    )
                }

                if (uiState.isLoading && uiState.visibleClasses.isEmpty()) {
                    CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                }
            }
        }
    }
}
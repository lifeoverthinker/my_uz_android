package com.example.my_uz_android.ui.screens.calendar.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.ui.components.ClassCard
import com.example.my_uz_android.ui.components.ClassCardType
import com.example.my_uz_android.ui.screens.calendar.DaySwipeDirection
import com.example.my_uz_android.ui.theme.ClassColorPalette
import com.example.my_uz_android.ui.theme.InterFontFamily
import com.example.my_uz_android.ui.theme.getAppAccentColor
import com.example.my_uz_android.ui.theme.getAppBackgroundColor
import com.example.my_uz_android.ui.theme.taskPriorityDotColor
import com.kizitonwose.calendar.compose.CalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.WeekCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs
import androidx.compose.ui.res.stringResource

private val PolandZone = ZoneId.of("Europe/Warsaw")
private val HourHeight = 60.dp
private val HourColWidth = 56.dp
private const val InitialScrollDelayMsScheduleView = 10L

private fun parseMinutesFromTimeScheduleView(time: String, fallbackMinutes: Int): Int {
    val parts = time.split(":")
    if (parts.size < 2) return fallbackMinutes
    val hour = parts[0].toIntOrNull() ?: return fallbackMinutes
    val minute = parts[1].toIntOrNull() ?: return fallbackMinutes
    return (hour * 60 + minute).coerceAtLeast(0)
}

private fun taskDateInPolandScheduleView(task: TaskEntity): LocalDate {
    return Instant.ofEpochMilli(task.dueDate).atZone(PolandZone).toLocalDate()
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScheduleView(
    calendarState: CalendarState,
    weekState: WeekCalendarState,
    selectedDate: LocalDate,
    isMonthView: Boolean,
    swipeDirection: DaySwipeDirection? = null,
    onDateSelected: (LocalDate) -> Unit,
    onToggleView: () -> Unit,
    classes: List<ClassEntity>,
    tasks: List<TaskEntity>,
    classColorMap: Map<String, Int>,
    onClassClick: (ClassEntity) -> Unit,
    isDarkMode: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    isTeacherPlan: Boolean = false,
    availableDirections: List<String> = emptyList(),
    selectedDirections: Set<String> = emptySet(),
    onDirectionToggled: (String, Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    showHeader: Boolean = false
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    val tasksByDate = remember(tasks) {
        tasks.groupBy(::taskDateInPolandScheduleView)
    }

    LaunchedEffect(selectedDate, classes) {
        delay(InitialScrollDelayMsScheduleView)

        val isToday = selectedDate == LocalDate.now(PolandZone)
        val classesForDay = classes.filter { it.date == selectedDate.toString() }

        val targetMinute = when {
            isToday -> {
                val now = LocalTime.now(PolandZone)
                (now.hour * 60 + now.minute - 60).coerceAtLeast(0)
            }

            classesForDay.isNotEmpty() -> {
                val firstClassMinute = classesForDay.minOfOrNull {
                    parseMinutesFromTimeScheduleView(it.startTime, fallbackMinutes = 8 * 60)
                } ?: (8 * 60)
                (firstClassMinute - 60).coerceAtLeast(0)
            }

            else -> 8 * 60
        }

        scrollState.scrollTo(with(density) { targetMinute.dp.toPx() }.toInt())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showHeader) {
            val visibleMonth = if (isMonthView) {
                calendarState.firstVisibleMonth.yearMonth
            } else {
                val weekDays = weekState.firstVisibleWeek.days
                if (weekDays.isNotEmpty()) YearMonth.from(weekDays.first().date) else YearMonth.now(PolandZone)
            }

            val monthName = visibleMonth.month
                .getDisplayName(JavaTextStyle.FULL_STANDALONE, Locale.getDefault())
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable { onToggleView() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.calendar_title_month_year, monthName, visibleMonth.year),
                        style = TextStyle(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Icon(
                        painter = painterResource(
                            if (isMonthView) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        if (availableDirections.isNotEmpty()) {
            CalendarFilterRow(
                availableDirections = availableDirections,
                selectedDirections = selectedDirections,
                onDirectionToggled = onDirectionToggled
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.width(HourColWidth))
            Box(modifier = Modifier.weight(1f)) {
                DaysOfWeekTitle(daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.width(HourColWidth))
            Box(modifier = Modifier.weight(1f)) {
                if (isMonthView) {
                    HorizontalCalendar(
                        state = calendarState,
                        dayContent = { day ->
                            CalendarDay(
                                date = day.date,
                                isDateInMonth = day.position == DayPosition.MonthDate,
                                isSelected = selectedDate == day.date,
                                tasksForDay = tasksByDate[day.date].orEmpty(),
                                onClick = onDateSelected
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    WeekCalendar(
                        state = weekState,
                        dayContent = { day ->
                            CalendarDay(
                                date = day.date,
                                isDateInMonth = true,
                                isSelected = selectedDate == day.date,
                                tasksForDay = tasksByDate[day.date].orEmpty(),
                                onClick = onDateSelected
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = {
                val isNext = swipeDirection != DaySwipeDirection.PREVIOUS
                val enterOffset: (Int) -> Int = { fullWidth -> if (isNext) fullWidth else -fullWidth }
                val exitOffset: (Int) -> Int = { fullWidth -> if (isNext) -fullWidth else fullWidth }

                (slideInHorizontally(
                    animationSpec = tween(300),
                    initialOffsetX = enterOffset
                ) + fadeIn(animationSpec = tween(300))).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = exitOffset
                    ) + fadeOut(animationSpec = tween(300))
                ).using(SizeTransform(clip = false))
            },
            label = "DayOnlyTransition",
            modifier = Modifier.fillMaxSize()
        ) { animatedDate ->
            val animatedClassesForDay = remember(classes, animatedDate) {
                classes.filter { it.date == animatedDate.toString() }.sortedBy { it.startTime }
            }

            DayTimelineContent(
                selectedDate = animatedDate,
                classesForDay = animatedClassesForDay,
                classColorMap = classColorMap,
                onClassClick = onClassClick,
                isDarkMode = isDarkMode,
                isTeacherPlan = isTeacherPlan,
                scrollState = scrollState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterRow(
    availableDirections: List<String>,
    selectedDirections: Set<String>,
    onDirectionToggled: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(availableDirections) { direction ->
            val isSelected = selectedDirections.contains(direction)
            FilterChip(
                selected = isSelected,
                onClick = { onDirectionToggled(direction, !isSelected) },
                label = { Text(text = direction, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Done,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun DayTimelineContent(
    selectedDate: LocalDate,
    classesForDay: List<ClassEntity>,
    classColorMap: Map<String, Int>,
    onClassClick: (ClassEntity) -> Unit,
    isDarkMode: Boolean,
    isTeacherPlan: Boolean,
    scrollState: androidx.compose.foundation.ScrollState
) {
    if (classesForDay.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            com.example.my_uz_android.ui.components.EmptyStateMessage(
                title = stringResource(R.string.empty_calendar_title),
                subtitle = stringResource(R.string.empty_calendar_subtitle),
                message = stringResource(R.string.empty_calendar_message),
                iconRes = R.drawable.calendar_rafiki
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    repeat(24) { hour -> HourRow(hour = hour) }
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = HourColWidth)
                ) {
                    val eventLayouts = remember(classesForDay) {
                        com.example.my_uz_android.util.calculateEventLayouts(classesForDay)
                    }

                    eventLayouts.forEach { layoutInfo ->
                        ScheduledClassItem(
                            classEntity = layoutInfo.classEntity,
                            selectedDate = selectedDate,
                            classColorMap = classColorMap,
                            colIndex = layoutInfo.colIndex,
                            totalCols = layoutInfo.totalCols,
                            maxWidthDp = maxWidth,
                            onClassClick = onClassClick,
                            isDarkMode = isDarkMode,
                            isTeacherPlan = isTeacherPlan
                        )
                    }
                }

                if (selectedDate == LocalDate.now(PolandZone)) {
                    CurrentTimeIndicator()
                }
            }
        }
    }
}

@Composable
fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                text = dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
                    .replaceFirstChar { it.titlecase() }
                    .take(1),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CalendarDay(
    date: LocalDate,
    isDateInMonth: Boolean,
    isSelected: Boolean,
    tasksForDay: List<TaskEntity> = emptyList(),
    onClick: (LocalDate) -> Unit
) {
    val isToday = date == LocalDate.now(PolandZone)
    val primaryColor = MaterialTheme.colorScheme.primary

    val textColor = if (isDateInMonth) {
        when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isToday -> primaryColor
            else -> MaterialTheme.colorScheme.onSurface
        }
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val circleSize = if (isSelected || isToday) 28.dp else 24.dp
    val circleColor = if (isSelected && isDateInMonth) primaryColor else Color.Transparent
    val borderModifier = if (isToday && !isSelected && isDateInMonth) {
        Modifier.border(1.dp, primaryColor, CircleShape)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(circleColor)
                    .then(borderModifier)
                    .clickable(enabled = isDateInMonth) { onClick(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    ),
                    color = textColor,
                    textAlign = TextAlign.Center
                )
            }

            if (tasksForDay.isNotEmpty() && isDateInMonth) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    tasksForDay.take(4).forEach { task ->
                        val dotColor = taskPriorityDotColor(task.priority)

                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun HourRow(hour: Int) {
    val textYOffset = (-8).dp
    val horizontalLineStart = HourColWidth - 8.dp
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HourHeight)
    ) {
        if (hour != 0) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = horizontalLineStart)
                    .align(Alignment.TopStart),
                thickness = 1.dp,
                color = dividerColor
            )
        }

        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .padding(start = HourColWidth),
            thickness = 1.dp,
            color = dividerColor
        )

        if (hour != 0) {
            Text(
                text = String.format("%02d:00", hour),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .width(horizontalLineStart)
                    .padding(end = 8.dp)
                    .offset(y = textYOffset)
                    .align(Alignment.TopStart),
                textAlign = TextAlign.Start,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun ScheduledClassItem(
    classEntity: ClassEntity,
    selectedDate: LocalDate,
    classColorMap: Map<String, Int>,
    colIndex: Int,
    totalCols: Int,
    maxWidthDp: Dp,
    onClassClick: (ClassEntity) -> Unit,
    isDarkMode: Boolean,
    isTeacherPlan: Boolean = false
) {
    val (startMinutes, durationMinutes) = remember(classEntity) {
        val start = parseMinutesFromTimeScheduleView(classEntity.startTime, fallbackMinutes = 8 * 60)
        val end = parseMinutesFromTimeScheduleView(classEntity.endTime, fallbackMinutes = start + 90)
        start to maxOf(15, end - start)
    }

    val columnWidth = maxWidthDp / totalCols
    val startXOffset = columnWidth * colIndex

    val now = LocalDateTime.now(PolandZone)
    val classStartDateTime = runCatching {
        val parts = classEntity.startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        LocalDateTime.of(selectedDate, LocalTime.of(hour, minute))
    }.getOrElse { now.minusDays(1) }

    val isFuture = now.isBefore(classStartDateTime)

    val assignedColorIndex = classColorMap[classEntity.classType]
        ?: abs(classEntity.classType.hashCode()) % ClassColorPalette.size

    val bgColor = getAppBackgroundColor(assignedColorIndex, isDarkMode)
    val accentColor = getAppAccentColor(assignedColorIndex, isDarkMode)

    Box(
        modifier = Modifier
            .offset(x = startXOffset, y = startMinutes.dp)
            .width(columnWidth)
            .height(durationMinutes.dp)
            .padding(end = 4.dp, bottom = 2.dp)
    ) {
        ClassCard(
            classItem = classEntity,
            type = ClassCardType.CALENDAR,
            backgroundColor = bgColor,
            accentColor = accentColor,
            showBadge = isFuture,
            isTeacherPlan = isTeacherPlan,
            onClick = { onClassClick(classEntity) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CurrentTimeIndicator() {
    var currentTime by remember { mutableStateOf(LocalTime.now(PolandZone)) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now(PolandZone)
            delay(60_000)
        }
    }

    val minutesFromMidnight = currentTime.hour * 60 + currentTime.minute
    val indicatorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = minutesFromMidnight.dp)
            .height(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        HorizontalDivider(
            color = indicatorColor,
            thickness = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = HourColWidth)
        )
        Box(
            modifier = Modifier
                .padding(start = HourColWidth - 5.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
    }
}
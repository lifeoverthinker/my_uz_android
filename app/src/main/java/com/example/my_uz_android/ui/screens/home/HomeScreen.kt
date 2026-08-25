package com.example.my_uz_android.ui.screens.home

/**
 * Ekran Główny aplikacji. (Pulpit/Dashboard).
 * Zawiera skrót najważniejszych nadchodzących zajęć, zadań i wydarzeń dla zalogowanego studenta.
 */

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.my_uz_android.R
import com.example.my_uz_android.ui.AppViewModelProvider
import com.example.my_uz_android.ui.components.EmptyStateMessage
import com.example.my_uz_android.ui.components.EventCard
import com.example.my_uz_android.ui.components.TaskCard
import com.example.my_uz_android.ui.screens.home.components.UpcomingClasses
import com.example.my_uz_android.ui.theme.extendedColors
import com.example.my_uz_android.ui.screens.notifications.NotificationsViewModel
import com.example.my_uz_android.ui.components.DashboardEmptyCard
import com.example.my_uz_android.ui.theme.getAppBackgroundColor
import com.example.my_uz_android.ui.theme.getAppAccentColor
import com.example.my_uz_android.ui.components.TopBarActionIcon
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
    notificationsViewModel: NotificationsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onClassClick: (Int) -> Unit,
    onEventClick: (Int) -> Unit,
    onTaskClick: (Int) -> Unit,
    onSetupPlanClick: () -> Unit,
    onAccountClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onAddGradeClick: () -> Unit = {},
    onAddAbsenceClick: () -> Unit = {},
    onAddTaskClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var initialRefreshRequested by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(uiState.studyFields, uiState.hasAnyClasses, uiState.isLoading, uiState.isRefreshing) {
        if (!initialRefreshRequested && !uiState.isLoading && !uiState.isRefreshing) {
            val shouldAutoRefresh = uiState.studyFields.isNotEmpty() && !uiState.hasAnyClasses
            if (shouldAutoRefresh) {
                initialRefreshRequested = true
                viewModel.refreshSchedule()
            }
        }
        if (uiState.hasAnyClasses) {
            initialRefreshRequested = true
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val topSectionBackground = extendedColors.homeTopBackground
    val headerContentColor = MaterialTheme.colorScheme.onSurface
    val subHeaderContentColor = MaterialTheme.colorScheme.onSurfaceVariant

    val view = LocalView.current
    val defaultStatusBarColor = MaterialTheme.colorScheme.background
    val defaultNavBarColor = MaterialTheme.colorScheme.surface
    val homeStatusBarColor = topSectionBackground
    val homeNavBarColor = MaterialTheme.colorScheme.surface

    if (!view.isInEditMode) {
        DisposableEffect(isDark) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = homeStatusBarColor.toArgb()
                window.navigationBarColor = homeNavBarColor.toArgb()
                val insets = WindowCompat.getInsetsController(window, view)
                insets.isAppearanceLightStatusBars = !isDark
                insets.isAppearanceLightNavigationBars = !isDark
            }
            onDispose {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    window.statusBarColor = defaultStatusBarColor.toArgb()
                    window.navigationBarColor = defaultNavBarColor.toArgb()
                    val insets = WindowCompat.getInsetsController(window, view)
                    insets.isAppearanceLightStatusBars = !isDark
                    insets.isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
    val headerDateString = uiState.currentDateReference.format(formatter).replaceFirstChar { it.uppercase() }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(topSectionBackground)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(72.dp)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = headerDateString,
                            style = MaterialTheme.typography.titleSmall,
                            color = headerContentColor
                        )

                        val unreadCount by notificationsViewModel.unreadCount.collectAsState(initial = 0)

                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.offset(x = (-8).dp, y = 8.dp)
                                    )
                                }
                            }
                        ) {
                            TopBarActionIcon(
                                icon = R.drawable.ic_mail,
                                onClick = onNotificationsClick,
                                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                isFilled = true
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val firstName = uiState.userName.split(" ").firstOrNull()
                            ?: stringResource(R.string.default_user_title)
                        Text(
                            text = "${stringResource(R.string.greeting_morning)}, $firstName! 👋",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = headerContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.home_university_title),
                            style = MaterialTheme.typography.bodyMedium,
                            color = subHeaderContentColor
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::refreshSchedule,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp), modifier = Modifier.fillMaxSize()) {

                        item {
                            val displayClasses = if (uiState.todaysClasses.isNotEmpty()) uiState.todaysClasses else uiState.tomorrowClasses
                            val dayLabel = if (uiState.todaysClasses.isNotEmpty()) {
                                stringResource(R.string.dzisiaj)
                            } else if (uiState.tomorrowClasses.isNotEmpty()) {
                                stringResource(R.string.jutro)
                            } else {
                                stringResource(R.string.no_classes_title)
                            }

                            UpcomingClasses(
                                classes = displayClasses,
                                isPlanSelected = uiState.studyFields.isNotEmpty(),
                                emptyMessage = stringResource(R.string.no_classes_message),
                                dayLabel = dayLabel,
                                classColorMap = uiState.classColorMap,
                                isDarkMode = isDark,
                                onClassClick = onClassClick,
                                onSetupPlanClick = onSetupPlanClick
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(painter = painterResource(id = R.drawable.ic_book_open), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                    Text(text = stringResource(R.string.tasks_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface)
                                }
                                if (uiState.upcomingTasks.isEmpty()) {
                                    DashboardEmptyCard(
                                        title = stringResource(R.string.tasks_empty_card_title),
                                        message = stringResource(R.string.tasks_empty_card_message),
                                        iconRes = R.drawable.ic_check_circle_broken,
                                        containerColor = getAppBackgroundColor(1, isDark),
                                        accentColor = getAppAccentColor(1, isDark),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        itemsIndexed(uiState.upcomingTasks) { _, task ->
                                            TaskCard(
                                                task = task,
                                                onTaskClick = { onTaskClick(task.id) },
                                                modifier = Modifier.width(264.dp),
                                                backgroundColor = getAppBackgroundColor(1, isDark)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(painter = painterResource(id = R.drawable.ic_marker_pin), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                                    Text(text = stringResource(R.string.events_title), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = MaterialTheme.colorScheme.onSurface)
                                }
                                if (uiState.todaysEvents.isEmpty()) {
                                    DashboardEmptyCard(
                                        title = stringResource(R.string.events_empty_card_title),
                                        message = stringResource(R.string.events_empty_card_message),
                                        iconRes = R.drawable.ic_marker_pin,
                                        containerColor = getAppBackgroundColor(2, isDark),
                                        accentColor = getAppAccentColor(2, isDark),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(uiState.todaysEvents) { event ->
                                            EventCard(event = event, onClick = { onEventClick(event.id) }, modifier = Modifier.width(264.dp))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 24.dp), contentAlignment = Alignment.CenterStart) {
                                Text(text = stringResource(R.string.home_footer_label), style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.outline))
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}
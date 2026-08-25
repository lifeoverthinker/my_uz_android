package com.example.my_uz_android.ui.screens.calendar.tasks

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.ui.AppViewModelProvider
import com.example.my_uz_android.ui.components.EmptyStateMessage
import com.example.my_uz_android.ui.components.TaskCard
import com.example.my_uz_android.ui.components.TopAppBar
import com.example.my_uz_android.ui.components.TopBarActionIcon
import com.example.my_uz_android.ui.screens.calendar.CalendarViewModel
import com.example.my_uz_android.ui.screens.calendar.components.CalendarDrawerContent
import com.example.my_uz_android.ui.theme.InterFontFamily
import com.example.my_uz_android.ui.theme.getAppBackgroundColor
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import androidx.compose.ui.res.stringResource

private fun taskDateTasksScreen(dueDateMillis: Long): LocalDate {
    return Instant.ofEpochMilli(dueDateMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    onNavigateBack: () -> Unit,
    onAddTaskClick: () -> Unit,
    onTaskClick: (Int) -> Unit,
    onCalendarClick: () -> Unit,
    onAccountClick: () -> Unit,
    viewModel: TasksViewModel = viewModel(factory = AppViewModelProvider.Factory),
    calendarViewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val tasks by viewModel.tasksStream.collectAsStateWithLifecycle(initialValue = emptyList())
    val calendarUiState by calendarViewModel.uiState.collectAsStateWithLifecycle()
    val sharedCode by viewModel.sharedCode.collectAsStateWithLifecycle()
    val isSharing by viewModel.isSharing.collectAsStateWithLifecycle()
    val shareError by viewModel.shareError.collectAsStateWithLifecycle()
    val importStatus by viewModel.importStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showConfirmShareDialog by remember { mutableStateOf(false) }
    var showGeneratedCodeDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }
    var showImportSuccessDialog by remember { mutableStateOf(false) }
    var lastImportMessage by remember { mutableStateOf("") }

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedTasks by rememberSaveable(
        stateSaver = listSaver<Set<Int>, Int>(
            save = { it.toList() },
            restore = { it.toSet() }
        )
    ) { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(shareError) {
        val message = shareError ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        viewModel.clearShareError()
    }

    LaunchedEffect(importStatus) {
        val status = importStatus ?: return@LaunchedEffect
        if (status.contains("zaimportowano", ignoreCase = true)) {
            lastImportMessage = status
            showImportSuccessDialog = true
        } else {
            Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
        }
        viewModel.clearImportStatus()
    }

    LaunchedEffect(sharedCode) {
        if (sharedCode != null) showGeneratedCodeDialog = true
    }

    val groupedTasks = remember(tasks, selectedTab) {
        tasks
            .filter { task -> if (selectedTab == 0) !task.isCompleted else task.isCompleted }
            .sortedBy { it.dueDate }
            .groupBy { YearMonth.from(taskDateTasksScreen(it.dueDate)) }
    }

    LaunchedEffect(tasks, selectedTab, isSelectionMode) {
        if (!isSelectionMode) {
            if (selectedTasks.isNotEmpty()) selectedTasks = emptySet()
            return@LaunchedEffect
        }

        val visibleIds = tasks
            .asSequence()
            .filter { task -> if (selectedTab == 0) !task.isCompleted else task.isCompleted }
            .map { it.id }
            .toSet()

        val reconciledSelection = selectedTasks.intersect(visibleIds)
        if (reconciledSelection != selectedTasks) {
            selectedTasks = reconciledSelection
        }
        if (selectedTasks.isEmpty()) {
            isSelectionMode = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CalendarDrawerContent(
                favorites = calendarUiState.favorites,
                selectedResourceId = calendarUiState.selectedResourceId,
                currentScreen = "tasks",
                onMyPlanClick = {
                    calendarViewModel.selectMyPlan()
                    scope.launch { drawerState.close() }
                    onCalendarClick()
                },
                onTasksClick = { scope.launch { drawerState.close() } },
                onFavoriteClick = { fav ->
                    calendarViewModel.selectFavoritePlan(fav)
                    scope.launch { drawerState.close() }
                    onCalendarClick()
                },
                onCloseDrawer = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TasksTopBar(
                    isSelectionMode = isSelectionMode,
                    selectedTab = selectedTab,
                    onSelectionClose = {
                        isSelectionMode = false
                        selectedTasks = emptySet()
                    },
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onAddClick = onAddTaskClick,
                    onShareClick = { showShareOptionsDialog = true },
                    onTabChange = { selectedTab = it }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (groupedTasks.isEmpty()) {
                    EmptyStateMessage(
                        title = if (selectedTab == 0) stringResource(R.string.tasks_empty_active) else stringResource(
                            R.string.tasks_empty_completed
                        ),
                        subtitle = stringResource(R.string.tasks_empty_subtitle),
                        message = stringResource(R.string.tasks_empty_msg),
                        iconRes = R.drawable.to_do_list_rafiki
                    )
                } else {
                    TasksList(
                        groupedTasks = groupedTasks,
                        isSelectionMode = isSelectionMode,
                        selectedTasks = selectedTasks,
                        onTaskClick = { id ->
                            if (isSelectionMode) {
                                selectedTasks =
                                    if (selectedTasks.contains(id)) selectedTasks - id else selectedTasks + id
                            } else {
                                onTaskClick(id)
                            }
                        },
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onDeleteRequest = { taskToDelete = it }
                    )
                }

                if (isSelectionMode && selectedTasks.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    ) {
                        Button(
                            onClick = { showConfirmShareDialog = true },
                            shape = RoundedCornerShape(100.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.tasks_confirm_selected,
                                    selectedTasks.size
                                ),
                                style = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareOptionsDialog) {
        ShareOptionsModal(
            onSelectAll = {
                viewModel.shareMyTasks()
                showShareOptionsDialog = false
            },
            onSelectTasks = {
                isSelectionMode = true
                showShareOptionsDialog = false
            },
            onImportClick = {
                showShareOptionsDialog = false
                showImportDialog = true
            },
            onDismiss = { showShareOptionsDialog = false }
        )
    }

    if (showConfirmShareDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmShareDialog = false },
            title = {
                Text(
                    stringResource(R.string.tasks_share_confirm_title),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            },
            text = { Text(stringResource(R.string.tasks_share_confirm_msg, selectedTasks.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.shareMyTasks(selectedTasks)
                        showConfirmShareDialog = false
                        isSelectionMode = false
                        selectedTasks = emptySet()
                    }
                ) { Text(stringResource(R.string.tasks_share_btn)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmShareDialog = false
                }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showGeneratedCodeDialog) {
        GeneratedCodeModal(
            code = sharedCode,
            isLoading = isSharing,
            onCopy = {
                sharedCode?.let {
                    clipboardManager.setText(AnnotatedString(it))
                    Toast.makeText(
                        context,
                        context.getString(R.string.tasks_code_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                    showGeneratedCodeDialog = false
                    viewModel.clearSharedCode()
                }
            },
            onDismiss = {
                showGeneratedCodeDialog = false
                viewModel.clearSharedCode()
            }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onImport = {
                viewModel.importTasks(it)
                showImportDialog = false
            },
            onDismiss = { showImportDialog = false }
        )
    }

    if (showImportSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showImportSuccessDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    stringResource(R.string.tasks_imported_title),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
            },
            text = {
                Text(
                    lastImportMessage,
                    style = TextStyle(fontFamily = InterFontFamily, fontSize = 16.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showImportSuccessDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.tasks_imported_btn)) }
            }
        )
    }

    if (taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Text(
                    stringResource(R.string.tasks_delete_title),
                    style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)
                )
            },
            text = { Text(stringResource(R.string.tasks_delete_msg, taskToDelete?.title ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Zielony komentarz: usuwamy wyłącznie po explicit confirm.
                        taskToDelete?.let { viewModel.deleteTask(it) }
                        taskToDelete = null
                    }
                ) {
                    Text(
                        stringResource(R.string.btn_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    taskToDelete = null
                }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }
}

@Composable
fun TasksTopBar(
    isSelectionMode: Boolean,
    selectedTab: Int,
    onSelectionClose: () -> Unit,
    onOpenDrawer: () -> Unit,
    onAddClick: () -> Unit,
    onShareClick: () -> Unit,
    onTabChange: (Int) -> Unit
) {
    TopAppBar(
        title = stringResource(R.string.tasks_screen_title),
        navigationIcon = if (isSelectionMode) R.drawable.ic_close else R.drawable.ic_menu,
        onNavigationClick = if (isSelectionMode) onSelectionClose else onOpenDrawer,
        isNavigationIconFilled = true,
        actions = {
            TopBarActionIcon(icon = R.drawable.ic_plus, onClick = onAddClick, isFilled = true)
            if (!isSelectionMode) {
                TopBarActionIcon(
                    icon = R.drawable.ic_share,
                    onClick = onShareClick,
                    isFilled = true
                )
            }
        },
        bottomContent = {
            Column {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { onTabChange(0) },
                            text = {
                                Text(
                                    stringResource(R.string.tasks_active),
                                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = TextStyle(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { onTabChange(1) },
                            text = {
                                Text(
                                    stringResource(R.string.tasks_completed),
                                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = TextStyle(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp
                )
            }
        }
    )
}

@Composable
fun TasksList(
    groupedTasks: Map<YearMonth, List<TaskEntity>>,
    isSelectionMode: Boolean,
    selectedTasks: Set<Int>,
    onTaskClick: (Int) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteRequest: (TaskEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
    ) {
        groupedTasks.forEach { (month, tasks) ->
            item {
                Text(
                    text = month.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
                        .replaceFirstChar { it.titlecase() } + " ${month.year}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
                val isSelected = isSelectionMode && selectedTasks.contains(task.id)
                val date = taskDateTasksScreen(task.dueDate)
                val prevDate = tasks.getOrNull(index - 1)?.let { taskDateTasksScreen(it.dueDate) }
                val isFirstInDay = prevDate != date

                SwipeToDismissItem(
                    task = task,
                    isSelectionMode = isSelectionMode,
                    isSelected = isSelected,
                    onTaskClick = { onTaskClick(task.id) },
                    onToggleTask = { onToggleTask(task) },
                    onDeleteRequest = { onDeleteRequest(task) },
                    date = date,
                    showDate = isFirstInDay
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissItem(
    task: TaskEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onTaskClick: () -> Unit,
    onToggleTask: () -> Unit,
    onDeleteRequest: () -> Unit,
    date: LocalDate,
    showDate: Boolean
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (isSelectionMode) return@rememberSwipeToDismissBoxState false
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleTask()
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteRequest()
                    false
                }

                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = !isSelectionMode,
        enableDismissFromEndToStart = !isSelectionMode,
        backgroundContent = {
            val bgColor = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            ) {
                val icon = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Done
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    else -> null
                }
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    ) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val cardColor = if (isSelected && isSelectionMode) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        } else {
            getAppBackgroundColor(1, isDark)
        }
        val borderColor = if (isSelected && isSelectionMode) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelectionMode) Color.Transparent else MaterialTheme.colorScheme.surface)
                .clickable(enabled = isSelectionMode) { onTaskClick() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.width(36.dp)) {
                if (showDate) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(
                                JavaTextStyle.SHORT,
                                Locale.getDefault()
                            ),
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight(500),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight(500),
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onTaskClick() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            TaskCard(
                task = task,
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                onTaskClick = onTaskClick,
                backgroundColor = cardColor
            )
        }
    }
}

@Composable
fun ShareOptionsModal(
    onSelectAll: () -> Unit,
    onSelectTasks: () -> Unit,
    onImportClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.tasks_share_title),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = onSelectAll,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(stringResource(R.string.tasks_share_all))
                }

                OutlinedButton(
                    onClick = onSelectTasks,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Text(stringResource(R.string.tasks_share_select))
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                TextButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_import),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(R.string.tasks_import_title),
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun GeneratedCodeModal(
    code: String?,
    isLoading: Boolean,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.tasks_share_code_title),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(
                            text = code ?: "------",
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 42.sp,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Button(
                    onClick = onCopy,
                    enabled = code != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_copy),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tasks_copy_code))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.btn_close),
                        style = TextStyle(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    )
}

@Composable
fun ImportDialog(onImport: (String) -> Unit, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.tasks_import_title),
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.tasks_import_hint),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.uppercase() },
                    label = { Text(stringResource(R.string.tasks_code_label)) },
                    placeholder = { Text(stringResource(R.string.tasks_code_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(code) },
                enabled = code.length == 6
            ) { Text(stringResource(R.string.btn_import)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}
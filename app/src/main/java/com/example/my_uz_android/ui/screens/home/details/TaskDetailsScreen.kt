package com.example.my_uz_android.ui.screens.home.details

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.ui.components.TopAppBar
import com.example.my_uz_android.ui.components.TopBarActionIcon
import com.example.my_uz_android.ui.theme.MyUZTheme
import com.example.my_uz_android.ui.theme.getAppBackgroundColor
import com.example.my_uz_android.ui.theme.taskPriorityDotColor
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource

@Composable
fun TaskDetailsScreenRoute(
    viewModel: TaskDetailsViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Int) -> Unit,
    onDuplicateClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val task = uiState.task

    if (uiState.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (task == null) {
        EmptyDetailsState(
            title = stringResource(R.string.task_details_empty_title),
            description = stringResource(R.string.task_details_empty_message)
        )
        return
    }

    TaskDetailsScreen(
        task = task,
        onBackClick = onNavigateBack,
        onEditClick = { onEditClick(task.id) },
        onDeleteClick = { viewModel.deleteTask(onSuccess = onNavigateBack) },
        onDuplicateClick = onDuplicateClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    task: TaskEntity,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDuplicateClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val squareColor = getAppBackgroundColor(1, isDark) // 1 = ColorSetBlue

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = "",
                    navigationIcon = R.drawable.ic_close,
                    isNavigationIconFilled = true,
                    onNavigationClick = onBackClick,
                    actions = {
                        TopBarActionIcon(
                            icon = R.drawable.ic_edit,
                            isFilled = true,
                            onClick = onEditClick
                        )
                        Box {
                            TopBarActionIcon(
                                icon = R.drawable.ic_dots_vertical,
                                isFilled = true,
                                onClick = { showMenu = true }
                            )
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.btn_duplicate)) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_copy),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDuplicateClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_trash),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    },
                    containerColor = Color.Transparent
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(squareColor, RoundedCornerShape(4.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatDate(task.dueDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(taskPriorityDotColor(task.priority), CircleShape)
                                )
                                Text(
                                    text = stringResource(taskPriorityLabelResTaskDetails(task.priority)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (!task.subjectName.isNullOrBlank()) {
                    DetailRowCard(
                        iconRes = R.drawable.ic_graduation_hat,
                        label = stringResource(R.string.label_subject_details),
                        value = formatSentenceCaseTaskDetails(task.subjectName ?: "")
                    )
                }

                if (task.hasReminder && task.reminderTime != null) {
                    DetailRowCard(
                        iconRes = R.drawable.ic_bell,
                        label = stringResource(R.string.label_set_reminder),
                        value = formatDateTime(task.reminderTime)
                    )
                }

                task.description?.let {
                    if (it.isNotBlank()) {
                        DetailRowCard(
                            iconRes = R.drawable.ic_menu_2,
                            label = stringResource(R.string.label_description),
                            value = formatSentenceCaseTaskDetails(it),
                            isMultiline = true
                        )
                    }
                }
            }

            if (showDeleteDialog) {
                DeleteConfirmationDialog(
                    onConfirm = {
                        onDeleteClick()
                        showDeleteDialog = false
                    },
                    onDismiss = { showDeleteDialog = false },
                    itemType = stringResource(R.string.item_type_task)
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    itemType: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirmation_title)) },
        text = { Text(stringResource(R.string.delete_confirmation_message, itemType)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.btn_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun DetailRowCard(
    iconRes: Int,
    label: String?,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    isMultiline: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = if (isMultiline) 3.dp else 0.dp)
                    .size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = value.ifEmpty { "-" },
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = valueColor
                )
            }
        }
    }
}

@Composable
private fun EmptyDetailsState(
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        .format(Date(timestamp))
        .replaceFirstChar { it.uppercase() }
}

private fun formatDateTime(timestamp: Long): String {
    return SimpleDateFormat("EEEE, d MMMM yyyy, HH:mm", Locale.getDefault())
        .format(Date(timestamp))
        .replaceFirstChar { it.uppercase() }
}

private fun taskPriorityLabelResTaskDetails(priority: Int): Int {
    return when (priority) {
        0 -> R.string.priority_low
        2 -> R.string.priority_high
        else -> R.string.priority_medium
    }
}

private fun formatSentenceCaseTaskDetails(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return trimmed
    val lowercased = trimmed.lowercase(Locale.getDefault())
    return lowercased.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Preview(showBackground = true)
@Composable
private fun TaskDetailsScreenPreview() {
    MyUZTheme {
        TaskDetailsScreen(
            task = TaskEntity(
                id = 1,
                title = "Przygotować prezentację",
                description = "Dodać slajdy o architekturze Compose.",
                dueDate = System.currentTimeMillis(),
                isCompleted = false,
                subjectName = "Programowanie",
                classType = "L",
                hasReminder = true,
                reminderTime = System.currentTimeMillis() + 3600_000,
                priority = 1
            ),
            onBackClick = {},
            onEditClick = {},
            onDeleteClick = {},
            onDuplicateClick = {}
        )
    }
}
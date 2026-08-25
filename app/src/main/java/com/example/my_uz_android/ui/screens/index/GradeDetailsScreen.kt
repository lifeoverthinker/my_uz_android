package com.example.my_uz_android.ui.screens.index

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.my_uz_android.R
import com.example.my_uz_android.ui.components.TopAppBar
import com.example.my_uz_android.ui.components.TopBarActionIcon
import com.example.my_uz_android.ui.theme.MyUZTheme
import com.example.my_uz_android.ui.theme.getAppBackgroundColor
import com.example.my_uz_android.util.ClassTypeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource

@Composable
fun GradeDetailsScreenRoute(
    viewModel: GradeDetailsViewModel,
    onNavigateBack: () -> Unit,
    onEditGrade: (Int) -> Unit,
    onDuplicateGrade: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val grade = uiState.grade

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

    if (grade == null) {
        EmptyDetailsState(
            title = stringResource(R.string.grade_details_empty_title),
            description = stringResource(R.string.grade_details_empty_message)
        )
        return
    }

    val gradeText = when {
        grade.isPoints -> "${if (grade.grade % 1.0 == 0.0) grade.grade.toInt() else grade.grade} pkt"
        grade.grade == -1.0 -> stringResource(R.string.activity_plus)
        else -> grade.grade.toString()
    }

    GradeDetailsScreen(
        title = grade.description ?: if (grade.isPoints) stringResource(R.string.label_points) else stringResource(R.string.label_grade_single),
        dateMillis = grade.date,
        subjectName = grade.subjectName,
        classType = ClassTypeUtils.getFullName(grade.classType),
        gradeText = gradeText,
        isPointsOrActivity = grade.isPoints || grade.grade == -1.0,
        weightText = grade.weight.toString(),
        comment = grade.comment,
        onNavigateBack = onNavigateBack,
        onEditGrade = { onEditGrade(grade.id) },
        onDeleteGrade = { viewModel.deleteGrade(onSuccess = onNavigateBack) },
        onDuplicateClick = onDuplicateGrade
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeDetailsScreen(
    title: String,
    dateMillis: Long,
    subjectName: String,
    classType: String,
    gradeText: String,
    isPointsOrActivity: Boolean,
    weightText: String?,
    comment: String?,
    onNavigateBack: () -> Unit,
    onEditGrade: () -> Unit,
    onDeleteGrade: () -> Unit,
    onDuplicateClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val squareColor = getAppBackgroundColor(5, isDark) // 5 = ColorSetPink

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = "",
                    navigationIcon = R.drawable.ic_close,
                    isNavigationIconFilled = true,
                    onNavigationClick = onNavigateBack,
                    containerColor = Color.Transparent,
                    actions = {
                        TopBarActionIcon(
                            icon = R.drawable.ic_edit,
                            isFilled = true,
                            onClick = onEditGrade
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
                    }
                )
            },
            containerColor = Color.Transparent
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
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
                                    .background(
                                        color = squareColor,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatGradeDetailsDate(dateMillis),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val gradeLabel = if (isPointsOrActivity) {
                    stringResource(R.string.label_points_activity)
                } else {
                    if (!weightText.isNullOrEmpty()) stringResource(R.string.grade_weight_format, weightText) else stringResource(R.string.label_grade_single)
                }

                DetailRowCard(
                    iconRes = R.drawable.ic_trophy,
                    label = gradeLabel,
                    value = gradeText,
                    isValueHighlight = true
                )

                DetailRowCard(
                    iconRes = R.drawable.ic_graduation_hat,
                    label = stringResource(R.string.label_subject_details),
                    value = subjectName
                )

                DetailRowCard(
                    iconRes = R.drawable.ic_stand,
                    label = stringResource(R.string.label_class_type_details),
                    value = classType
                )

                if (!comment.isNullOrEmpty()) {
                    DetailRowCard(
                        iconRes = R.drawable.ic_menu_2,
                        label = stringResource(R.string.label_comment),
                        value = comment,
                        isMultiline = true
                    )
                }
            }

            if (showDeleteDialog) {
                GradeDeleteDialog(
                    onConfirm = {
                        onDeleteGrade()
                        showDeleteDialog = false
                    },
                    onDismiss = { showDeleteDialog = false },
                    itemType = stringResource(R.string.item_type_grade)
                )
            }
        }
    }
}

@Composable
private fun GradeDeleteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    itemType: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_confirmation_title)) },
        text = { Text(stringResource(R.string.delete_confirmation_message_alt, itemType)) },
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
    isValueHighlight: Boolean = false,
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
                    style = if (isValueHighlight) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = if (isValueHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

private fun formatGradeDetailsDate(timestamp: Long): String {
    return SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        .format(Date(timestamp))
        .replaceFirstChar { it.uppercase() }
}

@Preview(showBackground = true)
@Composable
fun GradeDetailsScreenPreview() {
    MyUZTheme {
        GradeDetailsScreen(
            title = "Kolokwium 1",
            dateMillis = System.currentTimeMillis(),
            subjectName = "Matematyka Dyskretna",
            classType = "Wykład",
            gradeText = "4.5",
            isPointsOrActivity = false,
            weightText = "3",
            comment = "Zadania otwarte poszły świetnie, ale brakło czasu na ostatnie zadanie.",
            onNavigateBack = {},
            onEditGrade = {},
            onDeleteGrade = {},
            onDuplicateClick = {}
        )
    }
}
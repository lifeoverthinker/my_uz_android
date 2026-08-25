package com.example.my_uz_android.ui.screens.index

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.GradeEntity
import com.example.my_uz_android.ui.AppViewModelProvider
import com.example.my_uz_android.ui.components.EmptyStateFigma
import com.example.my_uz_android.ui.components.TopAppBar
import com.example.my_uz_android.ui.components.TopBarActionIcon
import com.example.my_uz_android.ui.theme.InterFontFamily
import com.example.my_uz_android.util.ClassTypeUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SubjectGradesScreen(
    subjectKey: String,
    preselectedClassType: String? = null,
    onBackClick: () -> Unit,
    onGradeClick: (Int) -> Unit,
    onAddGradeClick: (String, String) -> Unit,
    viewModel: GradesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val subject = uiState.subjects.find { it.uniqueKey == subjectKey }
    val normalizedTypeFilter = preselectedClassType?.trim()?.lowercase()
    var showFinalGradeDialog by remember { mutableStateOf(false) }
    var gradeToDelete by remember { mutableStateOf<GradeEntity?>(null) }

    val visibleTypes = subject?.types
        ?.filter { normalizedTypeFilter.isNullOrBlank() || it.name.trim().lowercase() == normalizedTypeFilter }
        .orEmpty()

    if (showFinalGradeDialog) {
        FinalGradeDialog(
            onConfirm = { finalGradeValue ->
                // NAPRAWA: Zapisujemy wagę jako Int (1 zamiast 1.0)
                val newFinalGrade = GradeEntity(
                    subjectName = subject?.name ?: "Nieznany",
                    classType = visibleTypes.firstOrNull()?.name ?: "Inne",
                    grade = finalGradeValue,
                    weight = 1,
                    description = "Ocena końcowa",
                    date = System.currentTimeMillis(),
                    isPoints = false
                )
                viewModel.saveGrade(newFinalGrade)
                showFinalGradeDialog = false
            },
            onDismiss = { showFinalGradeDialog = false }
        )
    }

    if (gradeToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteGrade(gradeToDelete!!)
                gradeToDelete = null
            },
            onDismiss = { gradeToDelete = null },
            itemType = stringResource(R.string.item_type_grade)
        )
    }

    val classTypeSubtitle = when {
        visibleTypes.isEmpty() -> ""
        visibleTypes.size == 1 -> ClassTypeUtils.getFullName(visibleTypes.first().name)
        else -> visibleTypes.joinToString(", ") { ClassTypeUtils.getFullName(it.name) }
    }

    val allGrades = visibleTypes
        .flatMap { type -> type.grades.map { it to type.name } }
        .sortedByDescending { it.first.date }

    val hasAnyGrades = allGrades.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = subject?.name ?: stringResource(R.string.default_subject_name),
                subtitle = classTypeSubtitle,
                navigationIcon = R.drawable.ic_chevron_left,
                onNavigationClick = onBackClick,
                isNavigationIconFilled = true,
                actions = {
                    TopBarActionIcon(
                        icon = R.drawable.ic_plus,
                        isFilled = true,
                        onClick = {
                            val preferredType = visibleTypes.firstOrNull()?.name
                                ?: subject?.types?.firstOrNull()?.name
                                    .orEmpty()
                            onAddGradeClick(subject?.name.orEmpty(), preferredType)
                        }
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        if (subject == null || !hasAnyGrades) {
            EmptyStateFigma(
                title = stringResource(R.string.subject_grades_empty_title),
                subtitle = stringResource(R.string.subject_grades_empty_subtitle),
                message = stringResource(R.string.subject_grades_empty_message),
                iconRes = R.drawable.grades_rafiki,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                illustrationSize = 221.7868.dp,
                containerHeight = 582.dp
            )
            return@Scaffold
        }

        val totalPoints = allGrades.map { it.first }.filter { it.isPoints }.sumOf { it.grade }
        val activitiesCount = allGrades.map { it.first }.count { it.grade == -1.0 }
        val overallAverage = subject.average?.let { String.format("%.1f", it) } ?: "-"
        val pointsText = if (totalPoints % 1.0 == 0.0) totalPoints.toInt().toString() else totalPoints.toString()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DetailSummaryItem(
                        value = overallAverage,
                        label = stringResource(R.string.label_average),
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                    )
                    DetailSummaryItem(
                        value = if (totalPoints > 0) pointsText else "-",
                        label = stringResource(R.string.label_points),
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                    )
                    DetailSummaryItem(
                        value = activitiesCount.toString(),
                        label = stringResource(R.string.label_activities),
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.subject_grades_title),
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.add_final_grade),
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showFinalGradeDialog = true }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        allGrades.forEachIndexed { index, (grade, _) ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        gradeToDelete = grade
                                        false
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                backgroundContent = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.error),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.btn_delete),
                                            tint = Color.White,
                                            modifier = Modifier.padding(horizontal = 24.dp)
                                        )
                                    }
                                },
                                content = {
                                    GradeCardRow(grade = grade, onClick = { onGradeClick(grade.id) })
                                }
                            )

                            if (index < allGrades.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FinalGradeDialog(
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    val grades = listOf(2.0, 3.0, 3.5, 4.0, 4.5, 5.0)
    var selectedGrade by remember { mutableStateOf(grades.last()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.final_grade_dialog_title), style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.final_grade_dialog_message), style = TextStyle(fontFamily = InterFontFamily))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grades.forEach { grade ->
                        FilterChip(
                            selected = selectedGrade == grade,
                            onClick = { selectedGrade = grade },
                            label = { Text(grade.toString()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedGrade) }) { Text(stringResource(R.string.btn_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancel)) }
        }
    )
}

@Composable
private fun DetailSummaryItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
            color = valueColor
        )
        Text(
            text = label,
            style = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// Pojedynczy wiersz oceny na liście
@Composable
private fun GradeCardRow(grade: GradeEntity, onClick: () -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
    }
    val dateStr = formatter.format(Instant.ofEpochMilli(grade.date))

    val displayValue = when {
        grade.grade == -1.0 -> "+"
        grade.grade % 1.0 == 0.0 -> grade.grade.toInt().toString()
        else -> grade.grade.toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayValue,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = grade.description.takeIf { !it.isNullOrBlank() } ?: (if (grade.isPoints) stringResource(R.string.label_points) else stringResource(R.string.activity)),
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateStr,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // NAPRAWA: Z uwagi na fakt, że weight to Int, usunąłem modulo z wyświetlania - teraz wpisuje po prostu wartość np. 1
        if (!grade.isPoints && grade.weight > 0 && grade.grade != -1.0) {
            Text(
                text = stringResource(R.string.weight_label, grade.weight),
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
package com.example.my_uz_android.ui.screens.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.AbsenceEntity
import com.example.my_uz_android.ui.components.AbsencesEmptyState
import com.example.my_uz_android.util.ClassTypeUtils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource

@Composable
fun AbsencesScreen(
    viewModel: AbsencesViewModel,
    onAddAbsenceClick: (String, String) -> Unit,
    onEditAbsenceClick: (Int) -> Unit
) {
    val absencesState by viewModel.absencesState.collectAsStateWithLifecycle()
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    var expandedSubjectKeys by rememberSaveable { mutableStateOf(setOf<String>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (absencesState.isEmpty()) {
            AbsencesEmptyState(
                iconRes = R.drawable.students_rafiki,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(absencesState, key = { it.subjectName }) { subjectGroup ->
                    ExpandableAbsenceCard(
                        subjectData = subjectGroup,
                        isExpanded = expandedSubjectKeys.contains(subjectGroup.subjectName),
                        onExpandedChange = { expanded ->
                            expandedSubjectKeys = if (expanded) {
                                expandedSubjectKeys + subjectGroup.subjectName
                            } else {
                                expandedSubjectKeys - subjectGroup.subjectName
                            }
                        },
                        onDeleteAbsence = { viewModel.deleteAbsence(it) },
                        onAddClick = { type -> onAddAbsenceClick(subjectGroup.subjectName, type) },
                        onUpdateLimit = { type, limit ->
                            viewModel.updateLimit(subjectGroup.subjectName, type, limit)
                        },
                        onEditAbsenceClick = onEditAbsenceClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableAbsenceCard(
    subjectData: SubjectAbsences,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDeleteAbsence: (AbsenceEntity) -> Unit,
    onAddClick: (String) -> Unit,
    onUpdateLimit: (String, Int) -> Unit,
    onEditAbsenceClick: (Int) -> Unit
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
    var showLimitDialogForType by remember { mutableStateOf<AbsenceTypeGroup?>(null) }
    var absenceToDelete by remember { mutableStateOf<AbsenceEntity?>(null) }

    if (showLimitDialogForType != null) {
        LimitEditDialog(
            currentLimit = showLimitDialogForType!!.limit,
            onConfirm = {
                onUpdateLimit(showLimitDialogForType!!.classType, it)
                showLimitDialogForType = null
            },
            onDismiss = { showLimitDialogForType = null }
        )
    }

    if (absenceToDelete != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDeleteAbsence(absenceToDelete!!)
                absenceToDelete = null
            },
            onDismiss = { absenceToDelete = null },
            itemType = stringResource(R.string.item_type_absence)
        )
    }

    val cardBackground = MaterialTheme.colorScheme.surfaceContainerLow

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBackground, RoundedCornerShape(8.dp))
            .then(if (isExpanded) Modifier.padding(bottom = 16.dp) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth()
                .clickable { onExpandedChange(!isExpanded) }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = subjectData.subjectName,
                    style = TextStyle(
                        fontWeight = FontWeight(500),
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                subjectData.types.forEachIndexed { typeIndex, typeGroup ->
                    // NAPRAWA: Zliczamy do limitu tylko nieusprawiedliwione!
                    val count = typeGroup.absences.count { !it.isExcused }
                    val limit = typeGroup.limit
                    val isLimitReached = count >= limit

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = ClassTypeUtils.getFullName(typeGroup.classType),
                                    style = TextStyle(
                                        fontWeight = FontWeight(500),
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isLimitReached) MaterialTheme.colorScheme.errorContainer
                                            else MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { showLimitDialogForType = typeGroup }
                                        .padding(8.dp),
                                ) {
                                    Text(
                                        text = "$count/$limit",
                                        style = TextStyle(
                                            fontWeight = FontWeight(500),
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = if (isLimitReached) MaterialTheme.colorScheme.onErrorContainer
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.Start,
                            ) {
                                val formatter = remember {
                                    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                                        .withZone(ZoneId.systemDefault())
                                }

                                typeGroup.absences.forEach { absence ->
                                    val dateStr = formatter.format(Instant.ofEpochMilli(absence.date))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (absence.isExcused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onEditAbsenceClick(absence.id) }
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = dateStr + if (absence.isExcused) " (${stringResource(R.string.status_excused)})" else "",
                                            style = TextStyle(
                                                fontWeight = FontWeight(500),
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            ),
                                            color = if (absence.isExcused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.ic_close),
                                            contentDescription = stringResource(R.string.btn_delete),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { absenceToDelete = absence },
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddClick(typeGroup.classType) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plus),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.add_absence),
                                style = TextStyle(
                                    fontWeight = FontWeight(400),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (typeIndex < subjectData.types.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LimitEditDialog(
    currentLimit: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var textValue by remember { mutableStateOf(currentLimit.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_limit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.edit_limit_message))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 2) textValue = it
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(textValue.toIntOrNull() ?: currentLimit) }) {
                Text(stringResource(R.string.btn_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
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
        }
    )
}

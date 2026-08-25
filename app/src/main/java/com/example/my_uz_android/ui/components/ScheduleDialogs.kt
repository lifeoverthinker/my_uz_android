package com.example.my_uz_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.my_uz_android.util.ClassTypeUtils
import androidx.compose.ui.res.stringResource
import com.example.my_uz_android.R

// Okienko ze szczegółami i mailem wykładowcy (z opcją skopiowania)
@Composable
fun TeacherInfoDialog(
    onDismiss: () -> Unit,
    fullName: String,
    department: String,
    email: String,
    onEmailCopied: () -> Unit = {}
) {
    val clipboardManager = LocalClipboardManager.current
    val normalizedEmail = email.trim()
    val emailText = if (normalizedEmail.isNotBlank()) {
        normalizedEmail
    } else {
        stringResource(R.string.dialog_teacher_no_email)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_teacher_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TeacherInfoField(
                    label = stringResource(R.string.dialog_teacher_data_label),
                    value = fullName,
                    emphasize = true
                )
                TeacherInfoField(
                    label = stringResource(R.string.dialog_teacher_department_label),
                    value = department
                )
                TeacherEmailField(
                    label = stringResource(R.string.dialog_teacher_email_address_label),
                    email = emailText,
                    isCopyEnabled = normalizedEmail.isNotBlank(),
                    onClick = {
                        clipboardManager.setText(AnnotatedString(normalizedEmail))
                        onEmailCopied()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

@Composable
private fun TeacherInfoField(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TeacherEmailField(
    label: String,
    email: String,
    icon: ImageVector = Icons.Outlined.ContentCopy,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    isCopyEnabled: Boolean,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(containerColor)
                .clickable(enabled = isCopyEnabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Dialog wyboru podgrup w podglądzie planu
@Composable
fun SubgroupFilterDialog(
    subgroups: List<String>,
    selectedSubgroups: Set<String>,
    onDismiss: () -> Unit,
    onSelectionChange: (Set<String>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_filter_subgroups_title)) },
        text = {
            LazyColumn {
                items(subgroups) { subgroup ->
                    val subgroupLabel = if (subgroup.isBlank()) {
                        stringResource(R.string.label_whole_group)
                    } else {
                        subgroup
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val newSelection = if (selectedSubgroups.contains(subgroup)) {
                                    selectedSubgroups - subgroup
                                } else {
                                    selectedSubgroups + subgroup
                                }
                                onSelectionChange(newSelection)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedSubgroups.contains(subgroup),
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = subgroupLabel)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_done)) }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}

// Dialog filtrowania planu (grupy, podgrupy, typy zajęć)
@Composable
fun FilterDialog(
    groups: List<String>,
    selectedGroups: Set<String>,
    classTypes: List<String>,
    selectedClassTypes: Set<String>,
    onGroupSelected: (String, Boolean) -> Unit,
    onClassTypeSelected: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_filter_schedule_title)) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (groups.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.label_subgroups),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                        )
                    }
                    items(groups) { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onGroupSelected(group, !selectedGroups.contains(group)) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedGroups.contains(group),
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = group,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                if (classTypes.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.label_type),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp, top = 16.dp)
                        )
                    }
                    items(classTypes) { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onClassTypeSelected(type, !selectedClassTypes.contains(type)) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedClassTypes.contains(type),
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = ClassTypeUtils.getFullName(type),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_done))
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
}
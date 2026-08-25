package com.example.my_uz_android.ui.screens.home.details

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.ui.AppViewModelProvider
import com.example.my_uz_android.ui.components.TopAppBar
import com.example.my_uz_android.ui.screens.calendar.CalendarViewModel
import com.example.my_uz_android.ui.theme.MyUZTheme
import com.example.my_uz_android.ui.theme.getAppBackgroundColor
import com.example.my_uz_android.util.ClassTypeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.res.stringResource

@Composable
fun ClassDetailsScreen(
    classId: Int,
    onBackClick: () -> Unit,
    isTeacherPlan: Boolean = false,
    sharedCalendarViewModel: CalendarViewModel,
    viewModel: ClassDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val calendarUiState by sharedCalendarViewModel.uiState.collectAsState()

    LaunchedEffect(classId) {
        if (classId == -1) {
            viewModel.setTemporaryClass(calendarUiState.temporaryClassForDetails)
        }
    }

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

    val classEntity = uiState.classEntity
    if (classEntity == null) {
        EmptyDetailsState(
            title = stringResource(R.string.class_details_empty_title),
            description = stringResource(R.string.class_details_empty_message)
        )
        return
    }

    ClassDetailsContent(
        classEntity = classEntity,
        onBackClick = onBackClick,
        isTeacherPlan = isTeacherPlan
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailsContent(
    classEntity: ClassEntity,
    onBackClick: () -> Unit,
    isTeacherPlan: Boolean = false
) {
    val dateOrDayText = if (!classEntity.date.isNullOrBlank()) {
        try {
            val date = LocalDate.parse(classEntity.date)
            val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
            date.format(formatter).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            classEntity.date
        }
    } else {
        val day = DayOfWeek.of(if (classEntity.dayOfWeek == 0) 7 else classEntity.dayOfWeek)
        day.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }
    }

    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val squareColor = getAppBackgroundColor(0, isDark) // 0 = ColorSetPurple

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
                    actions = {},
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
                                    .background(squareColor, RoundedCornerShape(4.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = classEntity.subjectName,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$dateOrDayText • ${classEntity.startTime} - ${classEntity.endTime}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!classEntity.room.isNullOrBlank()) {
                    DetailRowCard(
                        iconRes = R.drawable.ic_marker_pin,
                        label = stringResource(R.string.label_place),
                        value = stringResource(R.string.label_room_format, classEntity.room ?: "")
                    )
                }

                if (!isTeacherPlan) {
                    if (!classEntity.teacherName.isNullOrBlank()) {
                        DetailRowCard(
                            iconRes = R.drawable.ic_user,
                            label = stringResource(R.string.label_teacher),
                            value = classEntity.teacherName,
                            isMultiline = false
                        )
                    }
                } else {
                    val groupInfo = buildString {
                        append(classEntity.groupCode)
                        if (!classEntity.subgroup.isNullOrBlank()) append(" (Podgrupa: ${classEntity.subgroup})")
                    }
                    if (groupInfo.isNotBlank()) {
                        DetailRowCard(
                            iconRes = R.drawable.ic_users,
                            label = stringResource(R.string.label_group),
                            value = groupInfo
                        )
                    }
                }

                DetailRowCard(
                    iconRes = R.drawable.ic_stand,
                    label = stringResource(R.string.label_class_type_details),
                    value = ClassTypeUtils.getFullName(classEntity.classType)
                )
            }
        }
    }
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

@Preview(showBackground = true)
@Composable
private fun ClassDetailsContentPreview() {
    MyUZTheme {
        ClassDetailsContent(
            classEntity = ClassEntity(
                id = 1,
                subjectName = "Algorytmy i Struktury Danych",
                classType = "W",
                room = "A-101",
                startTime = "08:15",
                endTime = "09:45",
                dayOfWeek = 1,
                date = "2026-04-02",
                groupCode = "INF-1A",
                subgroup = "1",
                teacherName = "dr Jan Kowalski",
                teacherEmail = "jan.kowalski@uczelnia.pl",
                teacherInstitute = "Instytut Informatyki",
                colorHex = "#3D84FF"
            ),
            onBackClick = {},
            isTeacherPlan = false
        )
    }
}
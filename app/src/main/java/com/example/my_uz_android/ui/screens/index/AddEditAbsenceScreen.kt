package com.example.my_uz_android.ui.screens.index

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.my_uz_android.R
import com.example.my_uz_android.ui.components.DatePicker
import com.example.my_uz_android.ui.components.TopAppBar
import com.example.my_uz_android.ui.theme.MyUZTheme
import com.example.my_uz_android.util.ClassTypeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource

// Obsługa stanu i logiki dodawania/edycji nieobecności
@Composable
fun AbsenceAddEditScreenRoute(
    viewModel: AddEditAbsenceViewModel,
    prefilledSubject: String? = null,
    prefilledClassType: String? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()

    // Po udanym zapisie wracamy do poprzedniego ekranu
    LaunchedEffect(isSaved) {
        if (isSaved) onNavigateBack()
    }

    // Inicjalizacja nowych danych na podstawie przekazanych argumentów (np. z widoku kalendarza)
    LaunchedEffect(prefilledSubject, prefilledClassType, uiState.id, uiState.subjectName) {
        if (uiState.id == 0 && uiState.subjectName == null) {
            viewModel.initNewAbsence(prefilledSubject, prefilledClassType)
        }
    }

    // Zmienne lokalne, aby uniknąć opóźnień (lagów) podczas wpisywania tekstu
    var localSubject by remember(uiState.subjectName) { mutableStateOf(uiState.subjectName ?: "") }
    var localClassType by remember(uiState.classType) { mutableStateOf(uiState.classType ?: "") }

    val subjectsList = uiState.availableSubjects.map { it.first }
    val classTypesList = uiState.availableSubjects
        .find { it.first == localSubject }
        ?.second
        .orEmpty()

    AbsenceAddEditScreen(
        isEditMode = uiState.id != 0,
        selectedSubject = localSubject,
        onSubjectChange = {
            localSubject = it
            viewModel.updateSubjectName(it)
            viewModel.updateClassType("") // Resetujemy rodzaj zajęć przy zmianie przedmiotu
            localClassType = ""
        },
        subjectsList = subjectsList,
        selectedClassType = localClassType,
        onClassTypeChange = {
            localClassType = it
            viewModel.updateClassType(it)
        },
        classTypesList = classTypesList,
        dateMillis = uiState.date,
        onDateChange = viewModel::updateDate,
        // Pobieramy status usprawiedliwienia PROSTO z ViewModelu (naprawiony bug!)
        isExcused = uiState.isExcused,
        onExcusedChange = viewModel::updateIsExcused,
        reason = uiState.description,
        onReasonChange = viewModel::updateDescription,
        onSaveClick = { viewModel.saveAbsence() },
        onNavigateBack = onNavigateBack
    )
}

// Kolory ramek pól tekstowych w formularzu
@Composable
private fun absenceOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    cursorColor = MaterialTheme.colorScheme.primary
)

private val AbsenceAppShape = RoundedCornerShape(16.dp)

// Klikalny wiersz wyboru (np. data)
@Composable
private fun AbsenceClickableFieldRow(
    label: String,
    value: String,
    iconRes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(AbsenceAppShape)
            .clickable(onClick = onClick),
        shape = AbsenceAppShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconRes != null) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 1.dp
            )
        }
    }
}

// Widok formularza nieobecności
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AbsenceAddEditScreen(
    isEditMode: Boolean,
    selectedSubject: String,
    onSubjectChange: (String) -> Unit,
    subjectsList: List<String>,
    selectedClassType: String,
    onClassTypeChange: (String) -> Unit,
    classTypesList: List<String>,
    dateMillis: Long,
    onDateChange: (Long) -> Unit,
    isExcused: Boolean,
    onExcusedChange: (Boolean) -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = if (isEditMode) stringResource(R.string.edit_absence_title) else stringResource(R.string.add_absence_title),
                    navigationIcon = R.drawable.ic_close,
                    isNavigationIconFilled = true,
                    onNavigationClick = onNavigateBack,
                    actions = {
                        Button(
                            onClick = onSaveClick,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .heightIn(min = 48.dp),
                            enabled = selectedSubject.isNotBlank() && selectedClassType.isNotBlank()
                        ) {
                            Text(stringResource(R.string.btn_save), style = MaterialTheme.typography.labelLarge)
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
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Wybór daty
                AbsenceClickableFieldRow(
                    label = stringResource(R.string.label_absence_date),
                    value = formatAbsenceAddEditDate(dateMillis),
                    iconRes = R.drawable.ic_calendar,
                    onClick = { showDatePicker = true }
                )

                // Wybór przedmiotu
                AbsenceAppDropdownMenu(
                    label = stringResource(R.string.label_subject_field),
                    selectedOption = selectedSubject,
                    options = subjectsList,
                    onOptionSelected = onSubjectChange,
                    iconRes = R.drawable.ic_book_open
                )

                // Wybór rodzaju zajęć (pokazywany tylko po wybraniu przedmiotu)
                AnimatedVisibility(
                    visible = selectedSubject.isNotEmpty() && classTypesList.isNotEmpty(),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.label_class_type_field),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            classTypesList.forEach { type ->
                                FilterChip(
                                    selected = selectedClassType == type,
                                    onClick = { onClassTypeChange(type) },
                                    label = { Text(ClassTypeUtils.getFullName(type)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                // Przełącznik "Usprawiedliwiona"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExcusedChange(!isExcused) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check),
                                contentDescription = null,
                                tint = if (isExcused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.status_excused),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Switch(
                            checked = isExcused,
                            onCheckedChange = onExcusedChange
                        )
                    }
                }

                // Pole opisu / powodu
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text(stringResource(R.string.label_reason_optional)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = absenceOutlinedTextFieldColors(),
                    shape = AbsenceAppShape
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showDatePicker) {
            DatePicker(
                date = dateMillis,
                onDateSelected = {
                    onDateChange(it)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }
    }
}

// Menu rozwijane wyboru przedmiotu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceAppDropdownMenu(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    iconRes: Int? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = iconRes?.let {
                {
                    Icon(
                        painter = painterResource(it),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
            colors = absenceOutlinedTextFieldColors(), // Używa spójnych kolorów
            shape = AbsenceAppShape
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatAbsenceAddEditDate(millis: Long): String {
    return SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(millis))
}

@Preview(showBackground = true)
@Composable
fun AbsenceAddEditScreenPreview() {
    MyUZTheme {
        AbsenceAddEditScreen(
            isEditMode = false,
            selectedSubject = "Matematyka",
            onSubjectChange = {},
            subjectsList = listOf("Matematyka", "Fizyka"),
            selectedClassType = "W",
            onClassTypeChange = {},
            classTypesList = listOf("W", "L"),
            dateMillis = System.currentTimeMillis(),
            onDateChange = {},
            isExcused = false,
            onExcusedChange = {},
            reason = "",
            onReasonChange = {},
            onSaveClick = {},
            onNavigateBack = {}
        )
    }
}
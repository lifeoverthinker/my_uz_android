package com.example.my_uz_android.ui.screens.onboarding

/**
 * Ekran onboardingu prowadzący użytkownika przez konfigurację profilu i planu.
 * Zawiera kroki formularzowe oraz kroki informacyjne, a po zakończeniu zapisuje
 * ustawienia startowe używane w dalszym działaniu aplikacji.
 */

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.UserGender
import com.example.my_uz_android.ui.AppViewModelProvider
import com.example.my_uz_android.ui.theme.MyUZTheme
import kotlin.math.abs

@Composable
private fun getIllustrationResId(currentPage: Int): Int = when (currentPage) {
    0 -> R.drawable.college_students_rafiki
    1 -> R.drawable.hello_rafiki
    2 -> R.drawable.settings_rafiki
    3 -> R.drawable.students_rafiki
    4 -> R.drawable.calendar_rafiki
    5 -> R.drawable.grades_rafiki
    6 -> R.drawable.happy_student_rafiki
    else -> R.drawable.ic_user
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalLayoutApi::class)
/**
 * Główny kontener procesu onboardingu z nawigacją między krokami.
 *
 * @param viewModel ViewModel zarządzający stanem i zapisem konfiguracji.
 * @param onNavigateToOnboarding Callback zgodnościowy dla nawigacji do onboardingu.
 * @param onNavigateToHome Callback nawigacji do ekranu głównego.
 * @param onFinishOnboarding Callback wywoływany po zakończeniu onboardingu.
 */
@Composable
fun LandingScreen(
    viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onFinishOnboarding: () -> Unit = { onNavigateToHome() }
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedGender by viewModel.selectedGender.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val totalPages = viewModel.totalPages
    val extraGroupSearchQuery by viewModel.extraGroupSearchQuery.collectAsState()
    val selectedExtraGroup by viewModel.selectedExtraGroup.collectAsState()
    val availableExtraSubgroups by viewModel.availableExtraSubgroups.collectAsState()
    val selectedExtraSubgroups by viewModel.selectedExtraSubgroups.collectAsState()
    val additionalCourses by viewModel.additionalCourses.collectAsState()
    val filteredExtraGroups by viewModel.filteredExtraGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val isKeyboardVisible = WindowInsets.isImeVisible

    MyUZTheme {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (currentPage < totalPages - 1) {
                        TextButton(
                            onClick = {
                                viewModel.skipOnboarding { onFinishOnboarding() }
                            },
                            enabled = !isLoading
                        ) {
                            Text(
                                stringResource(R.string.onboarding_skip),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!isKeyboardVisible) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp)
                            .navigationBarsPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PageIndicators(totalPages = totalPages, currentPage = currentPage)
                        Spacer(Modifier.height(24.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            when (currentPage) {
                                0 -> {
                                    Button(
                                        onClick = { viewModel.onNextClick() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        Text(stringResource(R.string.onboarding_start), style = MaterialTheme.typography.labelLarge)
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_chevron_right),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                totalPages - 1 -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { viewModel.onBackClick() },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            enabled = !isLoading
                                        ) {
                                            Icon(painterResource(R.drawable.ic_chevron_left), null, Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.onboarding_back))
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.saveOnboardingData { onFinishOnboarding() }
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            enabled = !isLoading
                                        ) {
                                            if (isLoading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Text(stringResource(R.string.onboarding_finish))
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    // Nawigacja dla ekranu dodatkowych kierunków
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { viewModel.onBackClick() },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_chevron_left), null, Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.onboarding_back))
                                        }
                                        Button(
                                            onClick = { viewModel.onAdditionalCoursesNextClick() },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            enabled = !isLoading
                                        ) {
                                            Text(stringResource(R.string.onboarding_next))
                                            Spacer(Modifier.width(8.dp))
                                            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp))
                                        }
                                    }
                                }
                                else -> {
                                    val isNextEnabled = when(currentPage) {
                                        1 -> selectedGender != null && userName.isNotBlank()
                                        2 -> !selectedGroup.isNullOrBlank()
                                        else -> true
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { viewModel.onBackClick() },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(painterResource(R.drawable.ic_chevron_left), null, Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.onboarding_back))
                                        }
                                        Button(
                                            onClick = { viewModel.onNextClick() },
                                            modifier = Modifier.weight(1f).height(48.dp),
                                            enabled = isNextEnabled
                                        ) {
                                            Text(stringResource(R.string.onboarding_next))
                                            Spacer(Modifier.width(8.dp))
                                            Icon(painterResource(R.drawable.ic_chevron_right), null, Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        FooterText()
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally(animationSpec = tween(400)) { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally(animationSpec = tween(400)) { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "OnboardingContent"
                ) { page ->
                    var accumulatedDragX by remember(page) { mutableFloatStateOf(0f) }

                    Box(
                        modifier = Modifier.pointerInput(
                            page,
                            selectedGender,
                            userName,
                            selectedGroup,
                            isLoading
                        ) {
                            val threshold = 72.dp.toPx()

                            detectHorizontalDragGestures(
                                onDragStart = { accumulatedDragX = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    accumulatedDragX += dragAmount
                                },
                                onDragEnd = {
                                    if (abs(accumulatedDragX) < threshold) return@detectHorizontalDragGestures

                                    // Swipe w lewo = kolejny krok, swipe w prawo = poprzedni krok.
                                    if (accumulatedDragX < 0f) {
                                        when (page) {
                                            1 -> if (selectedGender != null && userName.isNotBlank()) viewModel.onNextClick()
                                            2 -> if (!selectedGroup.isNullOrBlank()) viewModel.onNextClick()
                                            3 -> if (!isLoading) viewModel.onAdditionalCoursesNextClick()
                                            in 0 until totalPages - 1 -> viewModel.onNextClick()
                                        }
                                    } else {
                                        viewModel.onBackClick()
                                    }

                                    accumulatedDragX = 0f
                                }
                            )
                        }
                    ) {
                        when (page) {
                            0 -> WelcomeStepContent()
                            1 -> PersonalizationStepContent(viewModel)
                            2 -> GroupSelectionStepContent(viewModel)
                            3 -> AdditionalCoursesStepContent(viewModel)
                            4 -> CalendarFeatureStepContent()
                            5 -> GradesFeatureStepContent()
                            6 -> FinalStepContent()
                        }
                    }
                }
            }
        }
    }
}

// --- DODATKOWY KROK: DODATKOWE KIERUNKI ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdditionalCoursesStepContent(viewModel: OnboardingViewModel) {
    val extraGroupSearchQuery by viewModel.extraGroupSearchQuery.collectAsState()
    val selectedExtraGroup by viewModel.selectedExtraGroup.collectAsState()
    val availableExtraSubgroups by viewModel.availableExtraSubgroups.collectAsState()
    val selectedExtraSubgroups by viewModel.selectedExtraSubgroups.collectAsState()
    val additionalCourses by viewModel.additionalCourses.collectAsState()
    val filteredExtraGroups by viewModel.filteredExtraGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val focusManager = LocalFocusManager.current
    var expanded by remember { mutableStateOf(false) }

    ResponsiveOnboardingStep(illustrationResId = R.drawable.students_rafiki) {
        OnboardingTexts(
            title = stringResource(R.string.onboarding_extra_title),
            subtitle = stringResource(R.string.onboarding_extra_subtitle),
            description = stringResource(R.string.onboarding_extra_desc)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = expanded && filteredExtraGroups.isNotEmpty(),
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = extraGroupSearchQuery,
                    onValueChange = {
                        viewModel.setExtraGroupSearchQuery(it)
                        expanded = true
                    },
                    placeholder = { Text(stringResource(R.string.onboarding_group_hint)) },
                    label = { Text(stringResource(R.string.onboarding_group_label)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(24.dp)) },
                    trailingIcon = {
                        if (extraGroupSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setExtraGroupSearchQuery("") }) {
                                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.onboarding_clear), Modifier.size(24.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenu(
                    expanded = expanded && filteredExtraGroups.isNotEmpty(),
                    onDismissRequest = { expanded = false }
                ) {
                    filteredExtraGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group) },
                            onClick = {
                                viewModel.selectExtraGroup(group)
                                expanded = false
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            if (!isLoading && extraGroupSearchQuery.isNotEmpty() && filteredExtraGroups.isEmpty() && selectedExtraGroup == null) {
                Text(
                    text = stringResource(R.string.onboarding_group_not_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                )
            }
        }

        AnimatedVisibility(visible = selectedExtraGroup != null && availableExtraSubgroups.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.onboarding_select_subgroups),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.Center) {
                    availableExtraSubgroups.forEach { subgroup ->
                        FilterChip(
                            selected = selectedExtraSubgroups.contains(subgroup),
                            onClick = { viewModel.toggleExtraSubgroup(subgroup) },
                            label = { Text(subgroup) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }

        if (additionalCourses.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.onboarding_added_groups),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                additionalCourses.forEach { (group, subgroups) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group + if (subgroups.isNotEmpty()) " (${subgroups.joinToString(", ")})" else "",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeExtraCourse(group) }) {
                            Icon(painterResource(R.drawable.ic_close), stringResource(R.string.btn_delete), Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- EKRANY ZAWARTOŚCI ---

@Composable
        /**
         * Renderuje krok powitalny z opisem możliwości aplikacji.
         */
fun WelcomeStepContent() {
    ResponsiveOnboardingStep(illustrationResId = R.drawable.college_students_rafiki) {
        OnboardingTexts(
            title = stringResource(R.string.onboarding_welcome_title),
            subtitle = stringResource(R.string.onboarding_welcome_subtitle),
            description = stringResource(R.string.onboarding_welcome_desc)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * Renderuje krok personalizacji użytkownika (forma zwrotu oraz dane osobowe).
 *
 * @param viewModel ViewModel onboardingu.
 */
@Composable
fun PersonalizationStepContent(viewModel: OnboardingViewModel) {
    val selectedGender by viewModel.selectedGender.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userSurname by viewModel.userSurname.collectAsState()
    val focusManager = LocalFocusManager.current

    ResponsiveOnboardingStep(illustrationResId = R.drawable.hello_rafiki) {
        OnboardingTexts(
            title = stringResource(R.string.onboarding_personalization_title),
            subtitle = stringResource(R.string.onboarding_personalization_subtitle),
            description = stringResource(R.string.onboarding_personalization_desc)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.onboarding_return_form),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilterChip(
                    selected = selectedGender == UserGender.STUDENT,
                    onClick = { viewModel.setGender(UserGender.STUDENT) },
                    label = {
                        Text(
                            stringResource(R.string.edit_personal_data_student),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    leadingIcon = null
                )

                FilterChip(
                    selected = selectedGender == UserGender.STUDENTKA,
                    onClick = { viewModel.setGender(UserGender.STUDENTKA) },
                    label = {
                        Text(
                            stringResource(R.string.edit_personal_data_studentka),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    leadingIcon = null
                )
            }
        }

        AnimatedVisibility(
            visible = selectedGender != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.onboarding_your_data),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userName,
                    onValueChange = { viewModel.setUserName(it) },
                    label = { Text(stringResource(R.string.onboarding_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userSurname,
                    onValueChange = { viewModel.setUserSurname(it) },
                    label = { Text(stringResource(R.string.onboarding_surname_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
/**
 * Renderuje krok wyboru grupy oraz podgrup planu zajęć.
 *
 * @param viewModel ViewModel onboardingu.
 */
@Composable
fun GroupSelectionStepContent(viewModel: OnboardingViewModel) {
    val searchQuery by viewModel.groupSearchQuery.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val availableSubgroups by viewModel.availableSubgroups.collectAsState()
    val selectedSubgroups by viewModel.selectedSubgroups.collectAsState()
    val filteredGroups by viewModel.filteredGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    ResponsiveOnboardingStep(illustrationResId = R.drawable.settings_rafiki) {
        OnboardingTexts(
            title = stringResource(R.string.onboarding_group_title),
            subtitle = stringResource(R.string.onboarding_group_subtitle),
            description = stringResource(R.string.onboarding_group_desc)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = expanded && filteredGroups.isNotEmpty(),
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        viewModel.setGroupSearchQuery(it)
                        expanded = true
                    },
                    placeholder = { Text(stringResource(R.string.onboarding_group_hint)) },
                    label = { Text(stringResource(R.string.onboarding_group_label)) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(24.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setGroupSearchQuery("") }) {
                                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.onboarding_clear), Modifier.size(24.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.medium
                )

                ExposedDropdownMenu(
                    expanded = expanded && filteredGroups.isNotEmpty(),
                    onDismissRequest = { expanded = false }
                ) {
                    filteredGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group) },
                            onClick = {
                                viewModel.selectGroup(group)
                                expanded = false
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
            }

            if (!isLoading && searchQuery.isNotEmpty() && filteredGroups.isEmpty() && selectedGroup == null) {
                Text(
                    text = stringResource(R.string.onboarding_group_not_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                )
            }
        }

        AnimatedVisibility(visible = selectedGroup != null && availableSubgroups.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.onboarding_select_subgroups),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.Center) {
                    availableSubgroups.forEach { subgroup ->
                        FilterChip(
                            selected = selectedSubgroups.contains(subgroup),
                            onClick = { viewModel.toggleSubgroup(subgroup) },
                            label = { Text(subgroup) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
        /**
         * Renderuje krok informacyjny dotyczący modułu kalendarza.
         */
fun CalendarFeatureStepContent() {
    InfoStepContent(
        pageIndex = 3,
        title = stringResource(R.string.onboarding_calendar_title),
        subtitle = stringResource(R.string.onboarding_calendar_subtitle),
        description = stringResource(R.string.onboarding_calendar_desc)
    )
}

@Composable
        /**
         * Renderuje krok informacyjny dotyczący modułu ocen.
         */
fun GradesFeatureStepContent() {
    InfoStepContent(
        pageIndex = 4,
        title = stringResource(R.string.onboarding_grades_title),
        subtitle = stringResource(R.string.onboarding_grades_subtitle),
        description = stringResource(R.string.onboarding_grades_desc)
    )
}

@Composable
        /**
         * Renderuje końcowy krok onboardingowy.
         */
fun FinalStepContent() {
    InfoStepContent(
        pageIndex = 5,
        title = stringResource(R.string.onboarding_final_title),
        subtitle = stringResource(R.string.onboarding_final_subtitle),
        description = stringResource(R.string.onboarding_final_desc)
    )
}

@Composable
        /**
         * Renderuje generyczny krok informacyjny onboardingowy.
         *
         * @param pageIndex Indeks kroku używany do doboru ilustracji.
         * @param title Tytuł kroku.
         * @param subtitle Podtytuł kroku.
         * @param description Opis kroku.
         */
fun InfoStepContent(pageIndex: Int, title: String, subtitle: String, description: String) {
    val resId = when(pageIndex) {
        3 -> R.drawable.calendar_rafiki
        4 -> R.drawable.grades_rafiki
        5 -> R.drawable.happy_student_rafiki
        else -> R.drawable.ic_user
    }
    ResponsiveOnboardingStep(illustrationResId = resId) {
        OnboardingTexts(title, subtitle, description)
    }
}

// --- WSPÓLNE HELPERY ---

@Composable
        /**
         * Wspólny kontener kroku onboardingowego z ilustracją i treścią.
         *
         * @param illustrationResId Id zasobu ilustracji kroku.
         * @param content Slot zawartości kroku.
         */
fun ResponsiveOnboardingStep(
    illustrationResId: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = 200.dp, max = 350.dp)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = illustrationResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
        /**
         * Renderuje zestaw tekstów opisujących dany krok onboardingu.
         *
         * @param title Tytuł kroku.
         * @param subtitle Podtytuł kroku.
         * @param description Opis kroku.
         */
fun OnboardingTexts(title: String, subtitle: String, description: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
        /**
         * Renderuje wskaźniki aktualnej pozycji w procesie onboardingu.
         *
         * @param totalPages Liczba wszystkich kroków.
         * @param currentPage Aktualny krok.
         */
fun PageIndicators(totalPages: Int, currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPages) { index ->
            val isActive = index == currentPage
            val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            val width = if (isActive) 24.dp else 8.dp
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
                    .animateContentSize()
            )
        }
    }
}

@Composable
        /**
         * Renderuje stopkę informacyjną ekranu onboardingu.
         */
fun FooterText() {
    Text(
        text = stringResource(R.string.onboarding_footer),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline
    )
}
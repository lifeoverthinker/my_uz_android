package com.example.my_uz_android.ui.components

/**
 * Komponenty pływającego przycisku akcji (FAB) dla ekranu głównego.
 * Plik zawiera rozwijane menu skrótów do najczęściej wykonywanych akcji
 * użytkownika wraz z konfiguracją pojedynczych pozycji.
 */

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.my_uz_android.R
import com.example.my_uz_android.ui.AppViewModelProvider
import com.example.my_uz_android.ui.screens.home.HomeViewModel
import com.example.my_uz_android.ui.theme.InterFontFamily
import androidx.compose.ui.res.stringResource

/**
 * Model pojedynczej akcji wyświetlanej w rozwijanym menu FAB.
 *
 * @param label Etykieta akcji widoczna w menu.
 * @param iconRes Id zasobu ikony akcji.
 * @param onClick Callback wykonywany po wyborze akcji.
 */
data class FabAction(
    val label: String,
    val iconRes: Int,
    val onClick: () -> Unit
)

/**
 * Renderuje rozwijany FAB z akcjami dodawania ocen, nieobecności i zadań.
 *
 * @param onAddGrade Akcja uruchamiana dla dodawania oceny.
 * @param onAddAbsence Akcja uruchamiana dla dodawania nieobecności.
 * @param onAddTask Akcja uruchamiana dla dodawania zadania.
 * @param homeViewModel ViewModel ekranu głównego przekazywany przez fabrykę.
 */
@Composable
fun Fab(
    onAddGrade: () -> Unit,
    onAddAbsence: () -> Unit,
    onAddTask: () -> Unit,
    endOffset: androidx.compose.ui.unit.Dp = 20.dp,
    bottomOffset: androidx.compose.ui.unit.Dp = 24.dp,
    homeViewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    BackHandler(enabled = isExpanded) {
        isExpanded = false
    }

    val addGradeLabel = stringResource(R.string.fab_add_grade)
    val addAbsenceLabel = stringResource(R.string.fab_add_absence)
    val addTaskLabel = stringResource(R.string.fab_add_task)

    val actions = remember(onAddGrade, onAddAbsence, onAddTask, addGradeLabel, addAbsenceLabel, addTaskLabel) {
        listOf(
            FabAction(addGradeLabel, R.drawable.ic_trophy, onAddGrade),
            FabAction(addAbsenceLabel, R.drawable.ic_clock, onAddAbsence),
            FabAction(addTaskLabel, R.drawable.ic_book_open, onAddTask)
        )
    }

    // Scrim: White for light mode, dark for dark mode
    val scrimColor = if (isDark) {
        Color.Black.copy(alpha = 0.75f)
    } else {
        Color.White.copy(alpha = 0.9f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim overlay - Czysty scrim bez duplikatu imienia
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isExpanded = false }
            )
        }

        // Kontener na przyciski
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = endOffset, bottom = bottomOffset)
        ) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    actions.forEach { action ->
                        FabMenuItem(
                            label = action.label,
                            iconRes = action.iconRes,
                            onClick = {
                                action.onClick()
                                isExpanded = false
                            }
                        )
                    }
                }
            }

            // Przycisk główny
            val shapeAnimation by animateIntAsState(
                targetValue = if (isExpanded) 50 else 28,
                label = "ShapeRadius"
            )

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(percent = shapeAnimation)
                    )
                    .clickable { isExpanded = !isExpanded },
                contentAlignment = Alignment.Center
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (isExpanded) 45f else 0f,
                    label = "FabRotation"
                )

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    tint = if (isExpanded) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun FabMenuItem(
    label: String, 
    iconRes: Int, 
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .height(56.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.End,
            )
        }
    }
}
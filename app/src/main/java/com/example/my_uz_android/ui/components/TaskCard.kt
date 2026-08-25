package com.example.my_uz_android.ui.components

/**
 * Komponent karty zadania wykorzystywany na ekranach dashboardu i list zadań.
 * Udostępnia spójny sposób prezentacji tytułu, przedmiotu i stanu realizacji
 * z zachowaniem zgodności wizualnej z pozostałymi kartami aplikacji.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_uz_android.data.models.TaskEntity
import com.example.my_uz_android.ui.theme.InterFontFamily
import com.example.my_uz_android.ui.theme.extendedColors
import com.example.my_uz_android.ui.theme.taskPriorityDotColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Karta zadania z kropką priorytetu i datą terminu
@Composable
fun TaskCard(
    task: TaskEntity,
    onTaskClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null
) {
    val isCompleted = task.isCompleted
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val priorityDefaultBg = when (task.priority) {
        2 -> com.example.my_uz_android.ui.theme.getAppBackgroundColor(5, isDark) // Wysoki: pastelowy róż
        1 -> com.example.my_uz_android.ui.theme.getAppBackgroundColor(3, isDark) // Średni: pastelowy żółty
        else -> extendedColors.taskCardBackground                                // Niski: błękitny
    }

    val cardBackgroundColor = if (isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        backgroundColor ?: priorityDefaultBg
    }

    val titleColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textDecoration = if (isCompleted) TextDecoration.LineThrough else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBackgroundColor)
            .clickable { onTaskClick() }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = task.title,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight(500),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = titleColor,
                    textDecoration = textDecoration
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            val priorityColor = taskPriorityDotColor(task.priority)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
                Text(
                    text = taskPriorityLabelTaskCard(task.priority),
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = priorityColor
                    )
                )
            }
        }

        val subjectText = task.subjectName?.takeIf { it.isNotBlank() }
        if (subjectText != null) {
            Text(
                text = subjectText,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight(400),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = secondaryColor
                ),
                maxLines = 2, // Pozwalamy na 2 linie, by wysokość była spójna z EventCard (który ma 2 linie opisu)
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatTaskDateTaskCard(task.dueDate),
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight(400),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = secondaryColor
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun taskPriorityLabelTaskCard(priority: Int): String {
    return when (priority) {
        0 -> "Niski"
        2 -> "Wysoki"
        else -> "Średni"
    }
}

private fun formatTaskDateTaskCard(timestamp: Long): String {
    return SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
}

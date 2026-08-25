package com.example.my_uz_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.ClassEntity
import com.example.my_uz_android.ui.theme.InterFontFamily
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Definiuje warianty wizualne komponentu karty zajęć.
 * * Pozwala na ponowne wykorzystanie tego samego komponentu bazowego w różnych
 * kontekstach architektonicznych interfejsu (np. pełny widok na Ekranie Głównym
 * vs. minimalistyczny znacznik w Kalendarzu).
 */
enum class ClassCardType {
    HOME,
    CALENDAR
}

/**
 * Kompozywalny (Composable) element interfejsu reprezentujący kartę pojedynczych zajęć.
 *
 * Realizuje założenia zasady DRY (Don't Repeat Yourself), dostarczając zunifikowany
 * i reużywalny komponent wizualny dla całej aplikacji. Automatycznie zarządza swoją
 * prezentacją na podstawie przekazanego stanu (np. wygaszanie zajęć, które już się odbyły),
 * jednak pozostaje komponentem w pełni bezstanowym (stateless) z perspektywy logiki biznesowej.
 */
@Composable
fun ClassCard(
    classItem: ClassEntity,
    modifier: Modifier = Modifier,
    type: ClassCardType = ClassCardType.HOME,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    showBadge: Boolean = true,
    hasDeadlines: Boolean = false,
    isTeacherPlan: Boolean = false,
    onClick: () -> Unit = {}
) {
    val titleColor = MaterialTheme.colorScheme.onSurface
    val detailsColor = MaterialTheme.colorScheme.onSurfaceVariant

    val avatarTextColor = if (accentColor.luminance() > 0.5f) Color(0xFF1D192B) else Color.White

    val isPast = remember(classItem) {
        try {
            val datePart = LocalDate.parse(classItem.date)
            val timePart = LocalTime.parse(classItem.endTime)
            val classEndDateTime = LocalDateTime.of(datePart, timePart)
            classEndDateTime.isBefore(LocalDateTime.now())
        } catch (e: Exception) {
            false
        }
    }

    val contentAlpha = if (isPast && type == ClassCardType.CALENDAR) 0.6f else 1f

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = classItem.subjectName,
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = titleColor
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clock),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = detailsColor
                        )
                        Text(
                            text = "${classItem.startTime} - ${classItem.endTime}",
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = detailsColor
                            ),
                            maxLines = 1
                        )
                    }

                    if (!classItem.room.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = classItem.room,
                                style = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = detailsColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (showBadge) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when (type) {
                        ClassCardType.HOME -> {
                            val letter = classItem.classType.firstOrNull()?.uppercase() ?: "A"
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            ) {
                                Text(
                                    text = letter,
                                    style = TextStyle(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp,
                                        color = avatarTextColor
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        ClassCardType.CALENDAR -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }

                    if (hasDeadlines) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "ClassCard • HOME")
@Composable
private fun ClassCardHomePreview() {
    MaterialTheme {
        ClassCard(
            classItem = sampleClassEntity(
                subjectName = "Matematyka",
                classType = "W",
                date = LocalDate.now().toString(),
                startTime = "08:00",
                endTime = "09:30",
                room = "A-101"
            ),
            type = ClassCardType.HOME,
            showBadge = true
        )
    }
}

private fun sampleClassEntity(
    subjectName: String,
    classType: String,
    date: String,
    startTime: String,
    endTime: String,
    room: String?
): ClassEntity = ClassEntity(
    subjectName = subjectName,
    classType = classType,
    date = date,
    startTime = startTime,
    endTime = endTime,
    room = room,
    dayOfWeek = runCatching { LocalDate.parse(date).dayOfWeek.value }.getOrDefault(0),
    groupCode = "",
    subgroup = null
)
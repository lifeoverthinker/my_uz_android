package com.example.my_uz_android.ui.components

/**
 * Zestaw komponentów Empty State używanych w dashboardzie i ekranach list.
 * Plik zawiera zarówno pełnoekranowe warianty informacyjne, jak i kompaktowe
 * karty dashboardowe dla sekcji bez danych.
 */

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_uz_android.ui.theme.InterFontFamily
import androidx.compose.ui.res.stringResource
import com.example.my_uz_android.R

/**
 * Bazowy full-screen Empty State zgodny z układem z Figmy:
 * - kontener szerokości 328.dp
 * - ilustracja ~221.8dp
 * - stack tekstów 312.dp
 * - spacing 10.dp między ilustracją i tekstem, 8.dp między liniami tekstu
 */
@Composable
fun EmptyStateMessage(
    title: String,
    message: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    EmptyStateFigma(
        title = title,
        subtitle = subtitle,
        message = message,
        iconRes = iconRes,
        modifier = modifier,
        actionText = actionText,
        onActionClick = onActionClick
    )
}

/**
 * Wariant "figmowy" do precyzyjnego odwzorowania:
 * - title: headlineMedium (28/36)
 * - subtitle: titleMedium (16/24), kolor primary-like (68548E)
 * - message: bodySmall (12/16), onSurfaceVariant
 */
@Composable
fun EmptyStateFigma(
    title: String,
    subtitle: String?,
    message: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    illustrationSize: Dp = 221.7868.dp,
    containerHeight: Dp = 519.dp
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .width(328.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(illustrationSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(illustrationSize),
                    contentScale = ContentScale.Fit
                )
            }

            Box(modifier = Modifier.width(312.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 28.sp,
                            lineHeight = 36.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(text = actionText)
                }
            }
        }
    }
}

/**
 * Gotowe preset-y pod ekrany, które opisałeś.
 */
@Composable
fun CalendarEmptyState(
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    EmptyStateFigma(
        title = stringResource(R.string.empty_calendar_title),
        subtitle = stringResource(R.string.empty_calendar_subtitle),
        message = stringResource(R.string.empty_calendar_message),
        iconRes = iconRes,
        modifier = modifier,
        illustrationSize = 221.7868.dp
    )
}

// Pusty stan dla listy zadań
@Composable
fun TasksEmptyState(
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    EmptyStateFigma(
        title = stringResource(R.string.empty_tasks_title),
        subtitle = stringResource(R.string.empty_tasks_subtitle),
        message = stringResource(R.string.empty_tasks_message),
        iconRes = iconRes,
        modifier = modifier,
        illustrationSize = 221.7868.dp,
        containerHeight = 519.dp
    )
}

// Pusty stan dla listy nieobecności
@Composable
fun AbsencesEmptyState(
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    EmptyStateFigma(
        title = stringResource(R.string.empty_absences_title),
        subtitle = stringResource(R.string.empty_absences_subtitle),
        message = stringResource(R.string.empty_absences_message),
        iconRes = iconRes,
        modifier = modifier,
        illustrationSize = 221.7868.dp,
        containerHeight = 519.dp
    )
}

// Mały kafelek na dashboardzie informujący o braku danych
@Composable
fun DashboardEmptyCard(
    title: String,
    message: String,
    iconRes: Int,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = containerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                ),
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Kafelek pustego stanu na dashboardzie z przyciskiem akcji
@Composable
fun DashboardActionEmptyCard(
    title: String,
    message: String,
    iconRes: Int,
    actionText: String,
    onActionClick: () -> Unit,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 26.sp
                ),
                color = accentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(text = actionText, color = MaterialTheme.colorScheme.surfaceContainerLowest)
            }
        }
    }
}
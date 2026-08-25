package com.example.my_uz_android.ui.screens.notifications

/**
 * Ekran powiadomień aplikacji z listą wpisów oraz stanem pustym.
 * Plik zawiera także komponenty prezentacji pojedynczego powiadomienia
 * i obsługę gestu usuwania przez przesunięcie.
 */

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.NotificationEntity
import com.example.my_uz_android.ui.theme.InterFontFamily
import com.example.my_uz_android.ui.theme.getAppBackgroundColor
import com.example.my_uz_android.ui.theme.getAppAccentColor
import com.example.my_uz_android.ui.components.TopAppBar
import com.example.my_uz_android.ui.components.TopBarActionIcon
import com.example.my_uz_android.ui.components.EmptyStateFigma
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Główny widok powiadomień
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(Unit) {
        viewModel.markAllAsRead()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(R.string.notifications_title),
                navigationIcon = R.drawable.ic_chevron_left,
                onNavigationClick = onNavigateBack,
                isNavigationIconFilled = true,
                actions = {
                    if (notifications.isNotEmpty()) {
                        TopBarActionIcon(
                            icon = R.drawable.ic_trash,
                            onClick = { viewModel.clearAll() },
                            isFilled = true,
                            iconTint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (notifications.isEmpty()) {
                EmptyNotificationsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = notifications, key = { it.id }) { notification ->
                        SwipeToDeleteNotification(
                            notification = notification,
                            onDelete = { viewModel.deleteNotification(notification) }
                        )
                    }
                }
            }
        }
    }
}

// Pojedyncze powiadomienie z możliwością usunięcia swipem w lewo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteNotification(
    notification: NotificationEntity,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.error
                else Color.Transparent,
                label = "color_animation"
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, shape = RoundedCornerShape(12.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        },
        content = {
            NotificationCardItem(notification)
        }
    )
}

// Karta powiadomienia z ikonką, datą i treścią
@Composable
fun NotificationCardItem(
    notification: NotificationEntity,
    backgroundColor: Color? = null,
    isDarkMode: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
) {
    val formattedDate = remember(notification.timestamp) {
        SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault()).format(Date(notification.timestamp))
    }

    // Indeks 0 dla powiadomień (fioletowy zestaw spójny z resztą apki)
    val bgColor = backgroundColor ?: getAppBackgroundColor(0, isDarkMode)
    val accentColor = getAppAccentColor(0, isDarkMode)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { /* Opcjonalne: obsługa kliknięcia */ }
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Ikonka w kółku - spójna z ClassCard HOME
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (notification.type == "schedule_change") R.drawable.ic_marker_pin else R.drawable.ic_bell),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = notification.title,
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = formattedDate,
                style = TextStyle(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }

        Text(
            text = notification.message,
            style = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EmptyNotificationsState() {
    EmptyStateFigma(
        title = stringResource(R.string.notifications_empty_title),
        subtitle = stringResource(R.string.notifications_empty_accent),
        message = stringResource(R.string.notifications_empty_desc),
        iconRes = R.drawable.push_notifications_rafiki,
        modifier = Modifier.fillMaxSize(),
        illustrationSize = 221.dp,
        containerHeight = 580.dp
    )
}
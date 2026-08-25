package com.example.my_uz_android.ui.components

/**
 * Zestaw współdzielonych komponentów górnych pasków aplikacji opartych o Material 3.
 * Plik agreguje warianty TopAppBar używane w różnych modułach, tak aby utrzymać
 * spójny styl nagłówków i akcji nawigacyjnych.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.my_uz_android.R
import com.example.my_uz_android.ui.theme.InterFontFamily
import androidx.compose.ui.res.stringResource

// Okrągły przycisk akcji w górnym pasku (np. powrót, serduszko, odśwież)
@Composable
fun TopBarActionIcon(
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    isLoading: Boolean = false,
    isFilled: Boolean = true
) {
    val finalBackgroundColor = if (isFilled) backgroundColor else Color.Transparent

    Box(
        modifier = modifier
            .size(48.dp) // touch target
            .clip(CircleShape)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color = finalBackgroundColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = iconTint
                )
            } else {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = iconTint
                )
            }
        }
    }
}

@Composable
private fun BaseTopBarContainer(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

// Główny pasek górny aplikacji z tytułem i przyciskami akcji
@Composable
fun TopAppBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: Int? = null,
    onNavigationClick: () -> Unit = {},
    isNavigationIconFilled: Boolean = true,
    isCenterAligned: Boolean = false,
    isFilled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    bottomContent: @Composable (() -> Unit)? = null,
    titleClickable: Boolean = false,
    onTitleClick: () -> Unit = {},
    titleIcon: Int? = null,
    titleColor: Color? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier = Modifier.background(containerColor)) {
        BaseTopBarContainer(backgroundColor = containerColor) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isCenterAligned) Arrangement.Center else Arrangement.spacedBy(12.dp)
            ) {
                if (navigationIcon != null) {
                    TopBarActionIcon(
                        icon = navigationIcon,
                        onClick = onNavigationClick,
                        isFilled = isNavigationIconFilled,
                        backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                }

                Column(
                    modifier = Modifier
                        .clickable(enabled = titleClickable, onClick = onTitleClick)
                        .padding(vertical = 2.dp),
                    horizontalAlignment = if (isCenterAligned) Alignment.CenterHorizontally else Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (titleColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(color = titleColor, shape = RoundedCornerShape(4.dp))
                            )
                        }
                        Text(
                            text = title,
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = if (subtitle != null) FontWeight.Medium else FontWeight.SemiBold,
                                fontSize = if (subtitle != null) 16.sp else 24.sp,
                                lineHeight = if (subtitle != null) 24.sp else 32.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (titleIcon != null) {
                            Icon(
                                painter = painterResource(id = titleIcon),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            text = subtitle,
                            style = TextStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
        }

        bottomContent?.invoke()
    }
}

// Górny pasek dla formularzy dodawania/edycji z przyciskiem "Zapisz"
@Composable
fun AddEditTopAppBar(
    title: String,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    saveButtonEnabled: Boolean = true,
    titleColor: Color? = null
) {
    TopAppBar(
        title = title,
        navigationIcon = R.drawable.ic_chevron_left,
        onNavigationClick = onBackClick,
        isNavigationIconFilled = true,
        titleColor = titleColor,
        actions = {
            Button(
                onClick = onSaveClick,
                enabled = saveButtonEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.heightIn(min = 40.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    stringResource(R.string.btn_save),
                    style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
                )
            }
        }
    )
}

// Górny pasek kalendarza z możliwością rozwinięcia wyboru planu
@Composable
fun CalendarTopAppBar(
    title: String,
    isExpanded: Boolean,
    onNavigationClick: () -> Unit,
    onTitleClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = title,
        navigationIcon = R.drawable.ic_menu,
        onNavigationClick = onNavigationClick,
        titleClickable = true,
        onTitleClick = onTitleClick,
        titleIcon = if (isExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down,
        isNavigationIconFilled = true,
        actions = actions
    )
}

// Pasek podglądu planu z przyciskiem dodawania do ulubionych
@Composable
fun PreviewTopAppBar(
    title: String,
    subtitle: String? = null,
    isFavorite: Boolean = false,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    actionIcon: Int? = null,
    onActionClick: () -> Unit = {}
) {
    TopAppBar(
        title = title,
        subtitle = subtitle,
        navigationIcon = R.drawable.ic_chevron_left,
        onNavigationClick = onBackClick,
        isNavigationIconFilled = true,
        actions = {
            if (actionIcon != null) {
                TopBarActionIcon(icon = actionIcon, onClick = onActionClick, isFilled = true)
            }
            TopBarActionIcon(
                icon = if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
                onClick = onFavoriteClick,
                isFilled = true,
                iconTint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

// Górny pasek z polem tekstowym do wyszukiwania
@Composable
fun SearchTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .statusBarsPadding()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TopBarActionIcon(
                    icon = R.drawable.ic_chevron_left,
                    onClick = onBackClick,
                    isFilled = true,
                    backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Spacer(modifier = Modifier.width(12.dp))
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                thickness = 1.dp
            )
        }
    }
}
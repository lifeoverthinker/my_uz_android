package com.example.my_uz_android.ui.screens.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.my_uz_android.R
import com.example.my_uz_android.data.models.ThemeMode

private val ThemeTileShape = RoundedCornerShape(12.dp)

@Immutable
private data class ThemeTileColors(
    val background: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color
)

@Composable
private fun resolveThemeTileColors(isSelected: Boolean): ThemeTileColors {
    val scheme = MaterialTheme.colorScheme
    return if (isSelected) {
        ThemeTileColors(
            background = scheme.primaryContainer,
            content = scheme.onPrimaryContainer,
            border = scheme.primary
        )
    } else {
        ThemeTileColors(
            background = scheme.surfaceVariant.copy(alpha = 0.4f),
            content = scheme.onSurfaceVariant,
            border = scheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun ThemeSelector(
    selectedTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = selectedTheme == mode
            val colors = resolveThemeTileColors(isSelected)
            val themeName = when (mode) {
                ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(ThemeTileShape)
                    .background(colors.background)
                    .border(
                        width = 1.dp,
                        color = colors.border,
                        shape = ThemeTileShape
                    )
                    .clickable { onThemeSelected(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = themeName,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.content
                )
            }
        }
    }
}
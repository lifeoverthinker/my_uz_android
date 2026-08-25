package com.example.my_uz_android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Podstawowe kolory
val MyUZ_Black = Color(0xFF1D192B)
val MyUZ_White = Color(0xFFFFFFFF)
val MyUZ_Icon_Bg_Light = Color(0xFFF7F2F9)
val MyUZ_Badge_Red = Color(0xFFB3261E)

// Material 3 - Motyw Jasny
val md_theme_light_primary = Color(0xFF6750A4)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFEADDFF)
val md_theme_light_onPrimaryContainer = Color(0xFF21005D)
val md_theme_light_secondary = Color(0xFF625B71)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFE8DEF8)
val md_theme_light_onSecondaryContainer = Color(0xFF1D192B)
val md_theme_light_tertiary = Color(0xFF7D5260)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFFFD8E4)
val md_theme_light_onTertiaryContainer = Color(0xFF31111D)
val md_theme_light_error = MyUZ_Badge_Red
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onErrorContainer = Color(0xFF410E0B)
val md_theme_light_outline = Color(0xFF79747E)
val md_theme_light_background = MyUZ_White
val md_theme_light_onBackground = Color(0xFF1D1B20)
val md_theme_light_surface = MyUZ_White
val md_theme_light_onSurface = MyUZ_Black
val md_theme_light_surfaceVariant = Color(0xFFE7E0EB)
val md_theme_light_onSurfaceVariant = Color(0xFF49454F)
val md_theme_light_inverseSurface = Color(0xFF313033)
val md_theme_light_inverseOnSurface = Color(0xFFF4EFF4)
val md_theme_light_inversePrimary = Color(0xFFD0BCFF)
val md_theme_light_outlineVariant = Color(0xFFCAC4D0)
val md_theme_light_scrim = Color(0xFF000000)

val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF8F1FA)
val md_theme_light_surfaceContainer = Color(0xFFF3EDF7)
val md_theme_light_surfaceContainerHigh = Color(0xFFECE6F0)
val md_theme_light_surfaceContainerHighest = Color(0xFFE6E0E9)

// Material 3 - Motyw Ciemny
val md_theme_dark_primary = Color(0xFFD0BCFF)
val md_theme_dark_onPrimary = Color(0xFF381E72)
val md_theme_dark_primaryContainer = Color(0xFF4F378B)
val md_theme_dark_onPrimaryContainer = Color(0xFFEADDFF)
val md_theme_dark_secondary = Color(0xFFCCC2DC)
val md_theme_dark_onSecondary = Color(0xFF332D41)
val md_theme_dark_secondaryContainer = Color(0xFF4A4458)
val md_theme_dark_onSecondaryContainer = Color(0xFFE8DEF8)
val md_theme_dark_tertiary = Color(0xFFEFB8C8)
val md_theme_dark_onTertiary = Color(0xFF492532)
val md_theme_dark_tertiaryContainer = Color(0xFF633B48)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFD8E4)
val md_theme_dark_error = Color(0xFFF2B8B5)
val md_theme_dark_onError = Color(0xFF601410)
val md_theme_dark_errorContainer = Color(0xFF8C1D18)
val md_theme_dark_onErrorContainer = Color(0xFFF9DEDC)
val md_theme_dark_outline = Color(0xFF938F99)
val md_theme_dark_background = Color(0xFF141218)
val md_theme_dark_onBackground = Color(0xFFE6E1E5)
val md_theme_dark_surface = Color(0xFF141218)
val md_theme_dark_onSurface = Color(0xFFE6E1E5)
val md_theme_dark_surfaceVariant = Color(0xFF49454F)
val md_theme_dark_onSurfaceVariant = Color(0xFFCAC4D0)
val md_theme_dark_inverseSurface = Color(0xFFE6E1E5)
val md_theme_dark_inverseOnSurface = Color(0xFF313033)
val md_theme_dark_inversePrimary = Color(0xFF6750A4)
val md_theme_dark_outlineVariant = Color(0xFF49454F)
val md_theme_dark_scrim = Color(0xFF000000)

val md_theme_dark_surfaceContainerLowest = Color(0xFF0F0D13)
val md_theme_dark_surfaceContainerLow = Color(0xFF1D1B20)
val md_theme_dark_surfaceContainer = Color(0xFF211F26)
val md_theme_dark_surfaceContainerHigh = Color(0xFF2B2930)
val md_theme_dark_surfaceContainerHighest = Color(0xFF36343B)

// Kolory dolnego paska nawigacji
val nav_light_background = MyUZ_White
val nav_light_border = Color(0xFFEDE6F3)
val nav_light_active = Color(0xFF381E72)
val nav_light_inactive = Color(0xFF787579)

val nav_dark_background = Color(0xFF1D1B20)
val nav_dark_border = Color(0xFF49454e)
val nav_dark_active = Color(0xFFd3bcfd)
val nav_dark_inactive = Color(0xFFcac4d0)

// Tło górnej części ekranu głównego
val home_top_background_light = MyUZ_Icon_Bg_Light
val home_top_background_dark = Color(0xFF25232A)

// Kolory kart zajęć, wydarzeń i zadań
val card_class_light = Color(0xFFE8DEF8)
val card_class_dark = Color(0xFF2B2733)
val card_event_light = Color(0xFFDAF5D7)
val card_event_dark = Color(0xFF233121)
val card_task_light = Color(0xFFD8FCFF)
val card_task_dark = Color(0xFF222F36)

// Zestaw kolorów dla kafelków
data class AppColorSet(
    val lightBg: Color,
    val darkBg: Color,
    val lightAccent: Color,
    val darkAccent: Color
)

val ColorSetPurple = AppColorSet(
    lightBg = Color(0xFFE9DEF8),
    darkBg = Color(0xFF2B2733),
    lightAccent = Color(0xFF6750A4),
    darkAccent = Color(0xFFD0BCFF)
)

val ColorSetBlue = AppColorSet(
    lightBg = Color(0xFFDDF4FA),
    darkBg = Color(0xFF27343D),
    lightAccent = Color(0xFF567985),
    darkAccent = Color(0xFFA8CFDC)
)

val ColorSetGreen = AppColorSet(
    lightBg = Color(0xFFDAF5D7),
    darkBg = Color(0xFF233121),
    lightAccent = Color(0xFF5B7D52),
    darkAccent = Color(0xFFA2D399)
)

val ColorSetYellow = AppColorSet(
    lightBg = Color(0xFFFAF7D9),
    darkBg = Color(0xFF353225),
    lightAccent = Color(0xFF7B7756),
    darkAccent = Color(0xFFD7D09B)
)

val ColorSetOrange = AppColorSet(
    lightBg = Color(0xFFFFE2D8),
    darkBg = Color(0xFF362821),
    lightAccent = Color(0xFF7D7352),
    darkAccent = Color(0xFFFFB599)
)

val ColorSetPink = AppColorSet(
    lightBg = Color(0xFFFFD8E4),
    darkBg = Color(0xFF3D272C),
    lightAccent = Color(0xFF7D5260),
    darkAccent = Color(0xFFFFB1C8)
)

val AppColorPalette = listOf(
    ColorSetPurple,
    ColorSetBlue,
    ColorSetGreen,
    ColorSetYellow,
    ColorSetOrange,
    ColorSetPink
)

val ClassColorPalette = AppColorPalette

// Zwraca tło kafelka (bezpieczny index bez crasha przy ujemnych liczbach)
fun getAppBackgroundColor(index: Int, isDark: Boolean): Color {
    val safeIndex = (index and 0x7FFFFFFF) % AppColorPalette.size
    val set = AppColorPalette[safeIndex]
    return if (isDark) set.darkBg else set.lightBg
}

// Zwraca kolor akcentu/ikonki kafelka
fun getAppAccentColor(index: Int, isDark: Boolean): Color {
    val safeIndex = (index and 0x7FFFFFFF) % AppColorPalette.size
    val set = AppColorPalette[safeIndex]
    return if (isDark) set.darkAccent else set.lightAccent
}

// Dobiera indeks koloru dla przedmiotu (z mapy użytkownika lub z hasha nazwy)
fun getClassColorIndex(subjectName: String?, userColorMap: Map<String, Int> = emptyMap()): Int {
    if (subjectName == null) return 0
    return userColorMap[subjectName] ?: ((subjectName.hashCode() and 0x7FFFFFFF) % AppColorPalette.size)
}

// Kolor kropki priorytetu zadania (2 = wysoki/czerwony, 1 = średni/pomarańczowy, 0 = niski/zielony)
@Composable
fun taskPriorityDotColor(priority: Int): Color {
    return when (priority) {
        2 -> MaterialTheme.colorScheme.error
        1 -> Color(0xFFF57C00)
        else -> Color(0xFF388E3C)
    }
}
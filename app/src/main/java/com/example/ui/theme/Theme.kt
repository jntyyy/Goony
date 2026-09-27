package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalVaultPalette = staticCompositionLocalOf<VaultThemePalette> { VaultThemePalette.Dark }
val LocalAccentColor = staticCompositionLocalOf { Color(0xFF7C4DFF) }

fun parseHexColor(hex: String, fallback: Color = Color(0xFF7C4DFF)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (clean.length == 8) {
            Color(colorInt)
        } else {
            fallback
        }
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun GVJVaultTheme(
    paletteName: String = "Dark",
    accentColorHex: String = MaterialYouColorPresets.SYSTEM_DYNAMIC_ID,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val palette = VaultThemePalette.fromName(paletteName)
    val isLight = palette is VaultThemePalette.Light
    val isSystemDynamic = accentColorHex.equals(MaterialYouColorPresets.SYSTEM_DYNAMIC_ID, ignoreCase = true)

    // Build Material 3 Color Scheme
    val colorScheme = if (isSystemDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // System Material You Dynamic Color (Monet) from user's wallpaper
        if (isLight) {
            dynamicLightColorScheme(context).copy(
                background = palette.bg,
                surface = palette.surface,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.cardBg,
                outline = palette.border
            )
        } else {
            dynamicDarkColorScheme(context).copy(
                background = palette.bg,
                surface = palette.surface,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.cardBg,
                outline = palette.border
            )
        }
    } else {
        // Custom Material You Tonal Preset
        val preset = MaterialYouColorPresets.getPreset(accentColorHex)
        if (isLight) {
            lightColorScheme(
                primary = preset.lightPrimary,
                secondary = preset.lightSecondary,
                tertiary = preset.lightTertiary,
                primaryContainer = preset.lightContainer,
                background = palette.bg,
                surface = palette.surface,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.cardBg,
                outline = palette.border
            )
        } else {
            darkColorScheme(
                primary = preset.darkPrimary,
                secondary = preset.darkSecondary,
                tertiary = preset.darkTertiary,
                primaryContainer = preset.darkContainer,
                background = palette.bg,
                surface = palette.surface,
                onBackground = palette.textPrimary,
                onSurface = palette.textPrimary,
                surfaceVariant = palette.cardBg,
                outline = palette.border
            )
        }
    }

    val activeAccent = colorScheme.primary

    CompositionLocalProvider(
        LocalVaultPalette provides palette,
        LocalAccentColor provides activeAccent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

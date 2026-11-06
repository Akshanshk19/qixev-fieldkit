package com.qixev.qfieldkit.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

val DarkColorScheme = darkColorScheme(
    primary = NeutralPrimaryDark,
    onPrimary = NeutralOnPrimaryDark,
    primaryContainer = NeutralPrimaryContainerDark,
    onPrimaryContainer = NeutralOnPrimaryContainerDark,
    secondary = NeutralSecondaryDark,
    onSecondary = NeutralOnSecondaryDark,
    secondaryContainer = NeutralSecondaryContainerDark,
    onSecondaryContainer = NeutralOnSecondaryContainerDark,
    tertiary = NeutralTertiaryDark,
    onTertiary = NeutralOnTertiaryDark,
    tertiaryContainer = NeutralTertiaryContainerDark,
    onTertiaryContainer = NeutralOnTertiaryContainerDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralOnBackgroundDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralOnSurfaceDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralOnSurfaceVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark
)

val LightColorScheme = lightColorScheme(
    primary = NeutralPrimaryLight,
    onPrimary = NeutralOnPrimaryLight,
    primaryContainer = NeutralPrimaryContainerLight,
    onPrimaryContainer = NeutralOnPrimaryContainerLight,
    secondary = NeutralSecondaryLight,
    onSecondary = NeutralOnSecondaryLight,
    secondaryContainer = NeutralSecondaryContainerLight,
    onSecondaryContainer = NeutralOnSecondaryContainerLight,
    tertiary = NeutralTertiaryLight,
    onTertiary = NeutralOnTertiaryLight,
    tertiaryContainer = NeutralTertiaryContainerLight,
    onTertiaryContainer = NeutralOnTertiaryContainerLight,
    background = NeutralBackgroundLight,
    onBackground = NeutralOnBackgroundLight,
    surface = NeutralSurfaceLight,
    onSurface = NeutralOnSurfaceLight,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = NeutralOnSurfaceVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = NeutralOutlineLight,
    outlineVariant = NeutralOutlineVariantLight
)

@Composable
fun QixevFieldKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Primary Material You experience on Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Backward-compatible alias for existing call sites and UI tests.
 */
@Composable
fun QFieldKitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = QixevFieldKitTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

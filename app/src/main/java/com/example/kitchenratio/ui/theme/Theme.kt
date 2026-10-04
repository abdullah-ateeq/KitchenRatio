package com.example.kitchenratio.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.kitchenratio.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageContainer,
    onPrimaryContainer = OnSageContainer,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = Color(0xFF7A6448),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1E1C9),
    onTertiaryContainer = Color(0xFF2B1E0B),
    background = Cream,
    onBackground = Charcoal,
    surface = Cream,
    onSurface = Charcoal,
    surfaceVariant = Beige,
    onSurfaceVariant = CharcoalMuted,
    surfaceTint = Sage,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceBright = CreamSurface,
    surfaceDim = Color(0xFFE6DFD4),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F2EA),
    surfaceContainer = Color(0xFFF3EDE3),
    surfaceContainerHigh = Color(0xFFEEE7DC),
    surfaceContainerHighest = Color(0xFFE8E1D5),
    inverseSurface = Charcoal,
    inverseOnSurface = WarmOffWhite,
    inversePrimary = SageDark
)

private val DarkColors = darkColorScheme(
    primary = SageDark,
    onPrimary = OnSageDark,
    primaryContainer = SageContainerDark,
    onPrimaryContainer = OnSageContainerDark,
    secondary = TerracottaDark,
    onSecondary = OnTerracottaDark,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = OnTerracottaContainerDark,
    tertiary = Color(0xFFDCC5A5),
    onTertiary = Color(0xFF3D2E17),
    tertiaryContainer = Color(0xFF55452E),
    onTertiaryContainer = Color(0xFFF7E3C6),
    background = DeepCharcoal,
    onBackground = WarmOffWhite,
    surface = DeepCharcoal,
    onSurface = WarmOffWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = WarmOffWhiteMuted,
    surfaceTint = SageDark,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceBright = Color(0xFF3B3835),
    surfaceDim = DeepCharcoal,
    surfaceContainerLowest = Color(0xFF141311),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = Color(0xFF232120),
    surfaceContainerHigh = Color(0xFF2C2A28),
    surfaceContainerHighest = Color(0xFF373432),
    inverseSurface = WarmOffWhite,
    inverseOnSurface = Charcoal,
    inversePrimary = Sage
)

@Composable
fun KitchenRatioTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

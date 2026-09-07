package com.ryanshelby.linea.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LineaDarkColorScheme = darkColorScheme(
    primary = LineaColors.TitaniumBlue,
    secondary = LineaColors.DesaturatedTeal,
    tertiary = LineaColors.MutedSageGreen,
    background = LineaColors.BackgroundTop,
    surface = LineaColors.GlassFallbackFill,
    error = LineaColors.MutedBrickRed,
    onPrimary = LineaColors.TextPrimary,
    onSecondary = LineaColors.TextPrimary,
    onBackground = LineaColors.TextPrimary,
    onSurface = LineaColors.TextPrimary
)

@Composable
fun LineaTheme(
    reduceAnimations: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = LineaColors.BackgroundTop.toArgb()
                window.navigationBarColor = LineaColors.BackgroundBottom.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalReduceAnimations provides reduceAnimations
    ) {
        MaterialTheme(
            colorScheme = LineaDarkColorScheme,
            typography = LineaTypography,
            shapes = LineaShapes
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LineaColors.BackgroundGradient)
            ) {
                content()
            }
        }
    }
}

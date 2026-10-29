package com.ryanshelby.linea.ui.theme

import android.app.Activity
import androidx.compose.runtime.remember
import com.ryanshelby.linea.ui.components.rememberCrystalBackdropSource
import com.ryanshelby.linea.ui.components.captureCrystalBackdrop
import com.ryanshelby.linea.ui.components.LocalCrystalBackdrop
import com.ryanshelby.linea.data.preferences.DesignStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun buildDarkColorScheme(palette: LineaColorPalette, onAccent: androidx.compose.ui.graphics.Color) = darkColorScheme(
    primary = palette.TitaniumBlue,
    secondary = palette.DesaturatedTeal,
    tertiary = palette.MutedSageGreen,
    background = palette.BackgroundTop,
    surface = palette.GlassFallbackFill,
    error = palette.MutedBrickRed,
    onPrimary = onAccent,
    onSecondary = onAccent,
    onBackground = palette.TextPrimary,
    onSurface = palette.TextPrimary
)

private fun buildLightColorScheme(palette: LineaColorPalette, onAccent: androidx.compose.ui.graphics.Color) = lightColorScheme(
    primary = palette.TitaniumBlue,
    secondary = palette.DesaturatedTeal,
    tertiary = palette.MutedSageGreen,
    background = palette.BackgroundTop,
    surface = palette.GlassFallbackFill,
    error = palette.MutedBrickRed,
    onPrimary = onAccent,
    onSecondary = onAccent,
    onBackground = palette.TextPrimary,
    onSurface = palette.TextPrimary
)

@Composable
fun LineaTheme(
    theme: String = "DARK",
    reduceAnimations: Boolean = false,
    designStyle: DesignStyle = DesignStyle.NEUMORPHISM,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()

    val activePalette = remember(theme, designStyle, systemInDark) {
        resolveThemePalette(designStyle, theme, systemInDark)
    }
    val backdrop = rememberCrystalBackdropSource()
    val themeMotion = remember(designStyle, reduceAnimations) {
        resolveThemeMotion(designStyle, reduceAnimations)
    }

    LineaColors.currentDesignStyle = designStyle
    LineaColors.isCrystal = (designStyle == DesignStyle.CRYSTAL)
    // Reactively update LineaColors snapshot state so all composables recompose instantly
    LineaColors.updatePalette(activePalette)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = activePalette.BackgroundTop.toArgb()
                window.navigationBarColor = activePalette.BackgroundBottom.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = activePalette.isLight
                    isAppearanceLightNavigationBars = activePalette.isLight
                }
            }
        }
    }

    val onAccent = if (designStyle == DesignStyle.CRYSTAL) LineaColors.OnAccent
        else if (designStyle == DesignStyle.NEO_BRUTALISM && activePalette.isLight) androidx.compose.ui.graphics.Color.Black
        else if (designStyle == DesignStyle.FINTECH || designStyle == DesignStyle.MATERIAL3) androidx.compose.ui.graphics.Color.White
        else activePalette.TextPrimary

    val colorScheme = if (activePalette.isLight) {
        buildLightColorScheme(activePalette, onAccent)
    } else {
        buildDarkColorScheme(activePalette, onAccent)
    }

    val currentTypography = LineaTypography
    val currentShapes = LineaShapes

    CompositionLocalProvider(
        LocalReduceAnimations provides reduceAnimations,
        LocalDesignStyle provides designStyle,
        LocalThemeMotion provides themeMotion,
        LocalCrystalBackdrop provides backdrop
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = currentTypography,
            shapes = currentShapes
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activePalette.BackgroundGradient)
            ) {
                if (designStyle == DesignStyle.CRYSTAL) {
                    Box(Modifier.fillMaxSize().captureCrystalBackdrop(backdrop)
                        .background(activePalette.BackgroundGradient)) { CrystalBackdrop() }
                }
                content()
            }
        }
    }
}

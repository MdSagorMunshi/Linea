package com.ryanshelby.linea.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.data.preferences.DesignStyle

/**
 * Motion specifications and interactive physics tokens for each theme.
 */
data class ThemeMotionConfig(
    val pressScale: Float,
    val pressOffsetX: Dp,
    val pressOffsetY: Dp,
    val pressRotationZ: Float,
    val pressAnimationSpec: AnimationSpec<Float>,
    val pressOffsetAnimationSpec: AnimationSpec<Dp>,
    val transitionSpec: AnimatedContentTransitionScope<com.ryanshelby.linea.ui.navigation.LineaDestination>.() -> ContentTransform
)

// =============================================================================
// Theme Motion Configurations
// =============================================================================

val NeumorphicMotionConfig = ThemeMotionConfig(
    pressScale = 0.95f,
    pressOffsetX = 0.dp,
    pressOffsetY = 0.dp,
    pressRotationZ = 0f,
    pressAnimationSpec = spring(
        dampingRatio = 0.65f,
        stiffness = 400f
    ),
    pressOffsetAnimationSpec = spring(
        dampingRatio = 0.65f,
        stiffness = 400f
    ),
    transitionSpec = {
        (scaleIn(tween(220), initialScale = 0.985f) + fadeIn(tween(200))) togetherWith
            (scaleOut(tween(180), targetScale = 0.99f) + fadeOut(tween(160)))
    }
)

val OrganicMotionConfig = ThemeMotionConfig(
    pressScale = 0.96f,
    pressOffsetX = 0.dp,
    pressOffsetY = 0.dp,
    pressRotationZ = 0f,
    pressAnimationSpec = spring(
        dampingRatio = 0.80f,
        stiffness = 220f
    ),
    pressOffsetAnimationSpec = spring(
        dampingRatio = 0.80f,
        stiffness = 220f
    ),
    transitionSpec = {
        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
        (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 8 * direction } + fadeIn(tween(280))) togetherWith
            (slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { -it / 10 * direction } + fadeOut(tween(200)))
    }
)

val FintechMotionConfig = ThemeMotionConfig(
    pressScale = 0.98f,
    pressOffsetX = 0.dp,
    pressOffsetY = 0.dp,
    pressRotationZ = 0f,
    pressAnimationSpec = tween(
        durationMillis = 110,
        easing = LinearOutSlowInEasing
    ),
    pressOffsetAnimationSpec = tween(
        durationMillis = 110,
        easing = LinearOutSlowInEasing
    ),
    transitionSpec = {
        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
        (slideInHorizontally(tween(160, easing = LinearOutSlowInEasing)) { it / 6 * direction } + fadeIn(tween(140))) togetherWith
            (slideOutHorizontally(tween(140, easing = FastOutLinearInEasing)) { -it / 6 * direction } + fadeOut(tween(120)))
    }
)

val Material3MotionConfig = ThemeMotionConfig(
    pressScale = 0.92f,
    pressOffsetX = 0.dp,
    pressOffsetY = 0.dp,
    pressRotationZ = 0f,
    pressAnimationSpec = spring(
        dampingRatio = 0.52f,
        stiffness = 320f
    ),
    pressOffsetAnimationSpec = spring(
        dampingRatio = 0.52f,
        stiffness = 320f
    ),
    transitionSpec = {
        (scaleIn(spring(dampingRatio = 0.65f, stiffness = 350f), initialScale = 0.91f) + fadeIn(tween(250))) togetherWith
            (scaleOut(spring(dampingRatio = 0.75f, stiffness = 400f), targetScale = 0.96f) + fadeOut(tween(180)))
    }
)

val NeoBrutalismMotionConfig = ThemeMotionConfig(
    pressScale = 1.0f, // No scale compression; physical translate into shadow!
    pressOffsetX = 3.dp,
    pressOffsetY = 3.dp,
    pressRotationZ = 0f,
    pressAnimationSpec = tween(
        durationMillis = 80,
        easing = LinearEasing
    ),
    pressOffsetAnimationSpec = tween(
        durationMillis = 80,
        easing = LinearEasing
    ),
    transitionSpec = {
        (scaleIn(spring(dampingRatio = 0.55f, stiffness = 550f), initialScale = 0.94f) + fadeIn(tween(120))) togetherWith
            (scaleOut(spring(dampingRatio = 0.55f, stiffness = 550f), targetScale = 0.94f) + fadeOut(tween(100)))
    }
)

val HandwrittenMotionConfig = ThemeMotionConfig(
    pressScale = 0.95f,
    pressOffsetX = 0.dp,
    pressOffsetY = 0.dp,
    pressRotationZ = -1.5f, // Playful sketch tilt
    pressAnimationSpec = spring(
        dampingRatio = 0.70f,
        stiffness = 280f
    ),
    pressOffsetAnimationSpec = spring(
        dampingRatio = 0.70f,
        stiffness = 280f
    ),
    transitionSpec = {
        (scaleIn(tween(240), initialScale = 0.96f) + fadeIn(tween(220))) togetherWith
            (scaleOut(tween(180), targetScale = 0.98f) + fadeOut(tween(160)))
    }
)

val CrystalMotionConfig = ThemeMotionConfig(
    pressScale = 0.94f,
    pressOffsetX = 0.dp,
    pressOffsetY = 0.dp,
    pressRotationZ = 0f,
    pressAnimationSpec = spring(
        dampingRatio = 0.85f,
        stiffness = 350f
    ),
    pressOffsetAnimationSpec = spring(
        dampingRatio = 0.85f,
        stiffness = 350f
    ),
    transitionSpec = {
        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
        (fadeIn(tween(260)) + slideInHorizontally(tween(300)) { it / 22 * direction } + scaleIn(tween(300), initialScale = .985f)) togetherWith
            (fadeOut(tween(160)) + slideOutHorizontally(tween(240)) { -it / 28 * direction } + scaleOut(tween(220), targetScale = .99f))
    }
)

val ReducedMotionConfig = ThemeMotionConfig(
    pressScale = 1.0f,
    pressOffsetX = 0.dp,
    pressOffsetY = 0.dp,
    pressRotationZ = 0f,
    pressAnimationSpec = tween(durationMillis = 80),
    pressOffsetAnimationSpec = tween(durationMillis = 80),
    transitionSpec = {
        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
    }
)

val LocalThemeMotion = compositionLocalOf { NeumorphicMotionConfig }

fun resolveThemeMotion(style: DesignStyle, reduceAnimations: Boolean): ThemeMotionConfig {
    if (reduceAnimations) return ReducedMotionConfig
    return when (style) {
        DesignStyle.NEUMORPHISM, DesignStyle.CLASSIC -> NeumorphicMotionConfig
        DesignStyle.ORGANIC -> OrganicMotionConfig
        DesignStyle.FINTECH -> FintechMotionConfig
        DesignStyle.MATERIAL3 -> Material3MotionConfig
        DesignStyle.NEO_BRUTALISM -> NeoBrutalismMotionConfig
        DesignStyle.HANDWRITTEN -> HandwrittenMotionConfig
        DesignStyle.CRYSTAL -> CrystalMotionConfig
    }
}

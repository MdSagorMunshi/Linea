package com.ryanshelby.linea.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf

val LocalReduceAnimations = compositionLocalOf { false }

object LineaMotion {
    // Spring physics presets
    val ResponsiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SubtleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val ScaleCompressSpring = spring<Float>(
        dampingRatio = 0.65f,
        stiffness = 400f
    )

    // Reduced animation fallback (fast crossfades)
    val FastFade = tween<Float>(
        durationMillis = 150,
        easing = FastOutSlowInEasing
    )

    // 250ms materialization animation for frosted blur panels
    val PanelMaterializeDuration = 250

    fun <T> springOrFade(reduceAnimations: Boolean, springSpec: AnimationSpec<T>): AnimationSpec<T> {
        return if (reduceAnimations) {
            tween(durationMillis = 120)
        } else {
            springSpec
        }
    }
}

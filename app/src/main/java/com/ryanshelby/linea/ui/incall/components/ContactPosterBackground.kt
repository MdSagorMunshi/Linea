package com.ryanshelby.linea.ui.incall.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations
import com.ryanshelby.linea.util.ContactPhotoHelper

@Composable
fun ContactPosterBackground(
    photoUri: String?,
    displayName: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val reduceAnimations = LocalReduceAnimations.current

    val bitmapState = produceState<Bitmap?>(initialValue = null, photoUri) {
        value = if (!photoUri.isNullOrBlank()) {
            ContactPhotoHelper.loadBitmap(context, photoUri)
        } else {
            null
        }
    }
    val bitmap = bitmapState.value

    val infiniteTransition = rememberInfiniteTransition(label = "PosterBreathing")
    val posterScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (reduceAnimations) 1.0f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PosterScale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LineaColors.BackgroundBottom)
    ) {
        if (bitmap != null) {
            // Real Contact Photo Poster
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(posterScale)
                    .blur(20.dp)
            )
        } else {
            // Generative Titanium Mesh Poster
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LineaColors.TitaniumBlue.copy(alpha = 0.16f),
                                LineaColors.BackgroundTop.copy(alpha = 0.4f),
                                LineaColors.BackgroundBottom
                            )
                        )
                    )
            ) {
                // Architectural Neumorphic Monogram Watermark
                val initial = displayName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "L"
                Text(
                    text = initial,
                    fontSize = 280.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.035f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .scale(posterScale)
                )
            }
        }

        // Layered Vignette Gradient Scrim: ensures flawless contrast for status bar, typography, and action pills
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.72f),
                        0.25f to Color.Black.copy(alpha = 0.35f),
                        0.55f to Color.Black.copy(alpha = 0.30f),
                        0.80f to Color.Black.copy(alpha = 0.70f),
                        1.0f to Color.Black.copy(alpha = 0.90f)
                    )
                )
        )
    }
}

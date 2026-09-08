package com.ryanshelby.linea.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import com.ryanshelby.linea.util.ContactPhotoHelper

@Composable
fun ContactAvatar(
    photoUri: String?,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    shape: Shape = CircleShape,
    borderWidth: Dp = LineaDimensions.HairlineBorder,
    borderColor: Color = LineaColors.GlassBorder,
    initialsTextSize: TextUnit = 16.sp,
    backgroundColor: Color = LineaColors.GlassFill
) {
    val context = LocalContext.current

    val bitmapState = produceState<Bitmap?>(initialValue = null, photoUri) {
        value = if (!photoUri.isNullOrBlank()) {
            ContactPhotoHelper.loadBitmap(context, photoUri)
        } else {
            null
        }
    }

    val bitmap = bitmapState.value

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString()
            if (!initial.isNullOrBlank() && initial.first().isLetterOrDigit()) {
                Text(
                    text = initial,
                    style = LineaTypography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = initialsTextSize
                    ),
                    color = LineaColors.TextPrimary
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = displayName,
                    tint = LineaColors.TextSecondary,
                    modifier = Modifier.size(size * 0.52f)
                )
            }
        }
    }
}

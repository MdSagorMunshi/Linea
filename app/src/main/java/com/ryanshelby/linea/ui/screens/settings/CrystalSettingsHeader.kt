package com.ryanshelby.linea.ui.screens.settings

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.crystalSurface
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun CrystalSettingsHeader(name: String, photo: Bitmap?, onProfile: () -> Unit) {
    Text("SETTINGS", style = LineaTypography.labelSmall.copy(letterSpacing = 2.sp), color = LineaColors.TextSecondary)
    Text("Make it yours.", style = LineaTypography.headlineLarge, color = LineaColors.TextPrimary)
    Spacer(Modifier.height(20.dp))
    Row(Modifier.fillMaxWidth().crystalSurface(RoundedCornerShape(28.dp))
        .clickable(onClick = onProfile).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (photo != null) Image(photo.asImageBitmap(), null,
            Modifier.size(52.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        else Box(Modifier.size(52.dp).crystalSurface(CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, null, tint = LineaColors.TitaniumBlue)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name.ifBlank { "My profile" }, style = LineaTypography.titleMedium, color = LineaColors.TextPrimary)
            Text("Your profile & contact card", style = LineaTypography.bodySmall, color = LineaColors.TextSecondary)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = LineaColors.TextSecondary)
    }
}

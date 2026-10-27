package com.ryanshelby.linea.ui.incall.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.ryanshelby.linea.ui.components.LocalCrystalBackdrop
import com.ryanshelby.linea.ui.components.captureCrystalBackdrop
import com.ryanshelby.linea.ui.components.rememberCrystalBackdropSource
import com.ryanshelby.linea.ui.theme.LineaColors

/** Call controls sample the actual contact poster, independently of the main activity. */
@Composable
fun CallScene(photoUri: String?, displayName: String?, modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit) {
    val source = rememberCrystalBackdropSource()
    CompositionLocalProvider(LocalCrystalBackdrop provides source) {
        Box(modifier.fillMaxSize()) {
            ContactPosterBackground(photoUri, displayName,
                modifier = if (LineaColors.isCrystal) Modifier.captureCrystalBackdrop(source) else Modifier)
            content()
        }
    }
}

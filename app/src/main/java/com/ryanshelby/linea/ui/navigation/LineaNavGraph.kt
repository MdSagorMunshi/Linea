package com.ryanshelby.linea.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.screens.contacts.ContactsScreen
import com.ryanshelby.linea.ui.screens.dialpad.DialpadScreen
import com.ryanshelby.linea.ui.screens.history.HistoryScreen
import com.ryanshelby.linea.ui.screens.settings.SettingsScreen
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

@Composable
fun LineaNavGraph(
    isDefaultDialer: Boolean,
    simAccounts: List<SimAccountInfo>,
    reduceAnimations: Boolean,
    onRequestDefaultDialer: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleReduceAnimations: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentDestination by remember { mutableStateOf(LineaDestination.DIALPAD) }
    val reduceAnim = LocalReduceAnimations.current

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = currentDestination,
            animationSpec = tween(durationMillis = if (reduceAnim) 120 else 220),
            label = "screen_crossfade"
        ) { destination ->
            when (destination) {
                LineaDestination.DIALPAD -> {
                    DialpadScreen(
                        isDefaultDialer = isDefaultDialer,
                        simAccounts = simAccounts,
                        onRequestDefaultDialer = onRequestDefaultDialer
                    )
                }
                LineaDestination.HISTORY -> {
                    HistoryScreen()
                }
                LineaDestination.CONTACTS -> {
                    ContactsScreen()
                }
                LineaDestination.SETTINGS -> {
                    SettingsScreen(
                        isDefaultDialer = isDefaultDialer,
                        reduceAnimations = reduceAnimations,
                        onToggleReduceAnimations = onToggleReduceAnimations,
                        onRequestDefaultDialer = onRequestDefaultDialer,
                        onRequestPermissions = onRequestPermissions
                    )
                }
            }
        }

        // Floating Glass Bottom Navigation Bar
        FloatingGlassNavBar(
            currentDestination = currentDestination,
            onNavigate = { destination -> currentDestination = destination },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

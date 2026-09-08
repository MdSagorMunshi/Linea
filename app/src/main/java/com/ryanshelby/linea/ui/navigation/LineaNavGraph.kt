package com.ryanshelby.linea.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.telecom.PhoneAccountManager
import com.ryanshelby.linea.telecom.SimAccountInfo
import com.ryanshelby.linea.ui.screens.contacts.ContactsScreen
import com.ryanshelby.linea.ui.screens.dialpad.DialpadScreen
import com.ryanshelby.linea.ui.screens.history.HistoryScreen
import com.ryanshelby.linea.ui.screens.settings.SettingsScreen
import com.ryanshelby.linea.ui.screens.settings.SettingsViewModel
import com.ryanshelby.linea.ui.screens.settings.blocking.BlockingScreen
import com.ryanshelby.linea.ui.screens.settings.blocking.BlockingViewModel
import com.ryanshelby.linea.ui.screens.settings.dualsim.DualSimScreen
import com.ryanshelby.linea.ui.screens.settings.permissions.PermissionsScreen
import com.ryanshelby.linea.ui.theme.LocalReduceAnimations

enum class SettingsSubScreen {
    BLOCKING,
    DUAL_SIM,
    PERMISSIONS,
    RECORDINGS,
    VOICEMAIL,
    CALL_FORWARDING,
    CALL_BARRING_FDN
}

@Composable
fun LineaNavGraph(
    isDefaultDialer: Boolean,
    simAccounts: List<SimAccountInfo>,
    reduceAnimations: Boolean,
    phoneAccountManager: PhoneAccountManager,
    lineaPreferences: LineaPreferences,
    onRequestDefaultDialer: () -> Unit,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    blockingViewModel: BlockingViewModel = hiltViewModel()
) {
    var currentDestination by remember { mutableStateOf(LineaDestination.DIALPAD) }
    var settingsSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }
    val reduceAnim = LocalReduceAnimations.current

    val settingsState by settingsViewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        if (settingsSubScreen != null) {
            when (settingsSubScreen) {
                SettingsSubScreen.BLOCKING -> {
                    BlockingScreen(
                        viewModel = blockingViewModel,
                        onNavigateBack = { settingsSubScreen = null }
                    )
                }
                SettingsSubScreen.DUAL_SIM -> {
                    DualSimScreen(
                        phoneAccountManager = phoneAccountManager,
                        preferences = lineaPreferences,
                        onNavigateBack = { settingsSubScreen = null }
                    )
                }
                SettingsSubScreen.PERMISSIONS -> {
                    PermissionsScreen(
                        isDefaultDialer = isDefaultDialer,
                        onRequestDefaultDialer = onRequestDefaultDialer,
                        onNavigateBack = { settingsSubScreen = null }
                    )
                }
                SettingsSubScreen.RECORDINGS -> {
                    com.ryanshelby.linea.ui.screens.recordings.RecordingsScreen(
                        onNavigateBack = { settingsSubScreen = null }
                    )
                }
                SettingsSubScreen.VOICEMAIL -> {
                    com.ryanshelby.linea.ui.screens.voicemail.VoicemailScreen(
                        onNavigateBack = { settingsSubScreen = null }
                    )
                }
                SettingsSubScreen.CALL_FORWARDING -> {
                    com.ryanshelby.linea.ui.screens.settings.telecom.CallForwardingScreen(
                        onNavigateBack = { settingsSubScreen = null }
                    )
                }
                SettingsSubScreen.CALL_BARRING_FDN -> {
                    com.ryanshelby.linea.ui.screens.settings.telecom.CallBarringFdnScreen(
                        onNavigateBack = { settingsSubScreen = null }
                    )
                }
                null -> Unit
            }
        } else {
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
                            viewModel = settingsViewModel,
                            isDefaultDialer = isDefaultDialer,
                            onRequestDefaultDialer = onRequestDefaultDialer,
                            onNavigateToBlocking = { settingsSubScreen = SettingsSubScreen.BLOCKING },
                            onNavigateToDualSim = { settingsSubScreen = SettingsSubScreen.DUAL_SIM },
                            onNavigateToPermissions = { settingsSubScreen = SettingsSubScreen.PERMISSIONS },
                            onNavigateToRecordings = { settingsSubScreen = SettingsSubScreen.RECORDINGS },
                            onNavigateToVoicemail = { settingsSubScreen = SettingsSubScreen.VOICEMAIL },
                            onNavigateToCallForwarding = { settingsSubScreen = SettingsSubScreen.CALL_FORWARDING },
                            onNavigateToCallBarring = { settingsSubScreen = SettingsSubScreen.CALL_BARRING_FDN }
                        )
                    }
                }
            }

            // Floating Glass Bottom Navigation Bar with unread missed calls badge
            FloatingGlassNavBar(
                currentDestination = currentDestination,
                onNavigate = { destination -> currentDestination = destination },
                unreadMissedCalls = settingsState.unreadMissedCalls,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

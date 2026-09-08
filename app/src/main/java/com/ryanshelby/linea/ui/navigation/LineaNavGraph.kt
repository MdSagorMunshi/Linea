package com.ryanshelby.linea.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import com.ryanshelby.linea.ui.screens.contacts.ContactsViewModel
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
    CALL_BARRING_FDN,
    CALL_RULES,
    DIAGNOSTICS,
    STATS,
    BACKUP
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
    callManager: com.ryanshelby.linea.telecom.CallManager? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    blockingViewModel: BlockingViewModel = hiltViewModel(),
    contactsViewModel: ContactsViewModel = hiltViewModel()
) {
    var currentDestination by remember { mutableStateOf(LineaDestination.DIALPAD) }
    var settingsSubScreen by remember { mutableStateOf<SettingsSubScreen?>(null) }
    var activeContactDashboardId by remember { mutableStateOf<Long?>(null) }
    var isPrivateSafeOpen by remember { mutableStateOf(false) }
    val reduceAnim = LocalReduceAnimations.current

    val settingsState by settingsViewModel.uiState.collectAsState()

    // Hoisted scroll states to preserve exact positions across sub-screen visits and tab switches
    // Resets cleanly only when the app process is closed (cold restart)
    val contactsListState = rememberLazyListState()
    val settingsScrollState = rememberScrollState()
    val historySessionsListState = rememberLazyListState()
    val historyFeedListState = rememberLazyListState()

    // Handle system back gestures seamlessly across all sub-screens
    BackHandler(enabled = activeContactDashboardId != null) {
        activeContactDashboardId = null
    }

    BackHandler(enabled = activeContactDashboardId == null && isPrivateSafeOpen) {
        isPrivateSafeOpen = false
    }

    BackHandler(enabled = activeContactDashboardId == null && !isPrivateSafeOpen && settingsSubScreen != null) {
        settingsSubScreen = null
    }

    BackHandler(enabled = activeContactDashboardId == null && !isPrivateSafeOpen && settingsSubScreen == null && currentDestination != LineaDestination.DIALPAD) {
        currentDestination = LineaDestination.DIALPAD
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Base screens (kept in composition so scroll position and UI states are preserved smoothly)
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
                    HistoryScreen(
                        sessionsListState = historySessionsListState,
                        feedListState = historyFeedListState
                    )
                }
                LineaDestination.CONTACTS -> {
                    ContactsScreen(
                        listState = contactsListState,
                        viewModel = contactsViewModel,
                        onContactClick = { contactId -> activeContactDashboardId = contactId },
                        onOpenPrivateSafe = {
                            activeContactDashboardId = null
                            isPrivateSafeOpen = true
                        }
                    )
                }
                LineaDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        isDefaultDialer = isDefaultDialer,
                        onRequestDefaultDialer = onRequestDefaultDialer,
                        scrollState = settingsScrollState,
                        onNavigateToBlocking = { settingsSubScreen = SettingsSubScreen.BLOCKING },
                        onNavigateToDualSim = { settingsSubScreen = SettingsSubScreen.DUAL_SIM },
                        onNavigateToPermissions = { settingsSubScreen = SettingsSubScreen.PERMISSIONS },
                        onNavigateToRecordings = { settingsSubScreen = SettingsSubScreen.RECORDINGS },
                        onNavigateToVoicemail = { settingsSubScreen = SettingsSubScreen.VOICEMAIL },
                        onNavigateToCallForwarding = { settingsSubScreen = SettingsSubScreen.CALL_FORWARDING },
                        onNavigateToCallBarring = { settingsSubScreen = SettingsSubScreen.CALL_BARRING_FDN },
                        onNavigateToCallRules = { settingsSubScreen = SettingsSubScreen.CALL_RULES },
                        onNavigateToDiagnostics = { settingsSubScreen = SettingsSubScreen.DIAGNOSTICS },
                        onNavigateToStats = { settingsSubScreen = SettingsSubScreen.STATS },
                        onNavigateToBackup = { settingsSubScreen = SettingsSubScreen.BACKUP }
                    )
                }
            }
        }

        // Floating Glass Bottom Navigation Bar with unread missed calls badge
        if (activeContactDashboardId == null && settingsSubScreen == null && !isPrivateSafeOpen) {
            FloatingGlassNavBar(
                currentDestination = currentDestination,
                onNavigate = { destination -> currentDestination = destination },
                unreadMissedCalls = settingsState.unreadMissedCalls,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Private Safe Full-screen Overlay
        if (isPrivateSafeOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                com.ryanshelby.linea.ui.screens.contacts.PrivateSafeScreen(
                    onNavigateBack = {
                        isPrivateSafeOpen = false
                        activeContactDashboardId = null
                    },
                    onContactClick = { contactId -> activeContactDashboardId = contactId },
                    viewModel = contactsViewModel,
                    vaultSecurityManager = contactsViewModel.vaultSecurityManager
                )
            }
        }

        // Contact Dashboard Full-screen Overlay (rendered last = always on top)
        if (activeContactDashboardId != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
                com.ryanshelby.linea.ui.screens.contacts.ContactDashboardScreen(
                    contactId = activeContactDashboardId!!,
                    onNavigateBack = { activeContactDashboardId = null }
                )
            }
        }

        // Settings Sub-Screen Full-screen Overlay (when viewing settings subpages)
        if (settingsSubScreen != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
            ) {
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
                            simAccounts = simAccounts,
                            onNavigateBack = { settingsSubScreen = null }
                        )
                    }
                    SettingsSubScreen.CALL_BARRING_FDN -> {
                        com.ryanshelby.linea.ui.screens.settings.telecom.CallBarringFdnScreen(
                            simAccounts = simAccounts,
                            onNavigateBack = { settingsSubScreen = null }
                        )
                    }
                    SettingsSubScreen.CALL_RULES -> {
                        com.ryanshelby.linea.ui.screens.rules.CallRulesScreen(
                            onNavigateBack = { settingsSubScreen = null }
                        )
                    }
                    SettingsSubScreen.DIAGNOSTICS -> {
                        com.ryanshelby.linea.ui.screens.diagnostics.CallDiagnosticsScreen(
                            onNavigateBack = { settingsSubScreen = null }
                        )
                    }
                    SettingsSubScreen.STATS -> {
                        com.ryanshelby.linea.ui.screens.stats.CallStatsScreen(
                            onNavigateBack = { settingsSubScreen = null }
                        )
                    }
                    SettingsSubScreen.BACKUP -> {
                        com.ryanshelby.linea.ui.screens.backup.BackupRestoreScreen(
                            onNavigateBack = { settingsSubScreen = null }
                        )
                    }
                    null -> Unit
                }
            }
        }

        // DIM Mode Floating Banner overlay (fallback inside app when system overlay permission is not granted)
        if (callManager != null) {
            val floatingCall by callManager.incomingFloatingCall.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            val hasOverlayPermission = android.provider.Settings.canDrawOverlays(context)

            if (!hasOverlayPermission && floatingCall != null) {
                com.ryanshelby.linea.ui.components.DontInterruptMeBanner(
                    callInfo = floatingCall,
                    onAnswer = {
                        callManager.answerCall()
                        callManager.dismissFloatingCall()
                        val inCallIntent = android.content.Intent(context, com.ryanshelby.linea.ui.incall.InCallActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        context.startActivity(inCallIntent)
                    },
                    onReject = {
                        callManager.rejectCall()
                        callManager.dismissFloatingCall()
                    },
                    onIgnore = {
                        callManager.silenceRinger()
                        callManager.dismissFloatingCall()
                    },
                    onMessage = {
                        callManager.dismissFloatingCall()
                    },
                    onExpand = {
                        callManager.dismissFloatingCall()
                        val inCallIntent = android.content.Intent(context, com.ryanshelby.linea.ui.incall.InCallActivity::class.java).apply {
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        context.startActivity(inCallIntent)
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // Return to active call chip when navigating dialer/contacts during an active/held call
            val activeCall by callManager.currentCall.collectAsState()
            val isCallInProgress = activeCall != null && (
                activeCall?.state == com.ryanshelby.linea.telecom.LineaCallState.ACTIVE ||
                activeCall?.state == com.ryanshelby.linea.telecom.LineaCallState.HOLDING ||
                activeCall?.state == com.ryanshelby.linea.telecom.LineaCallState.DIALING
            )

            AnimatedVisibility(
                visible = isCallInProgress,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
            ) {
                val current = activeCall
                if (current != null) {
                    val callerText = current.displayName ?: current.phoneNumber.ifBlank { "Active Call" }
                    val durationText = if (current.durationSeconds > 0) {
                        val m = current.durationSeconds / 60
                        val s = current.durationSeconds % 60
                        String.format("%02d:%02d", m, s)
                    } else if (current.state == com.ryanshelby.linea.telecom.LineaCallState.HOLDING) {
                        "On Hold"
                    } else {
                        "Dialing..."
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(LineaColors.GlassFill)
                            .border(1.dp, LineaColors.Success.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable {
                                val inCallIntent = android.content.Intent(context, com.ryanshelby.linea.ui.incall.InCallActivity::class.java).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                }
                                context.startActivity(inCallIntent)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LineaColors.Success)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Return to call: $callerText ($durationText)",
                            style = LineaTypography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = LineaColors.TextPrimary
                        )
                    }
                }
            }
        }
    }
}

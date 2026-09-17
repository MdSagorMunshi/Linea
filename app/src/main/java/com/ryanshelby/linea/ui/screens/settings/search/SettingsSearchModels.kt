package com.ryanshelby.linea.ui.screens.settings.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import com.ryanshelby.linea.ui.screens.settings.SettingsUiState

enum class SettingsCategory(val displayName: String, val icon: ImageVector) {
    GENERAL("Dual SIM & Profile", Icons.Filled.SimCard),
    CALLING("Calling Preferences", Icons.Filled.Phone),
    AUDIO("Sound & Haptics", Icons.AutoMirrored.Filled.VolumeUp),
    GESTURES("Motion & Gestures", Icons.Filled.ScreenRotation),
    SECURITY("Screening & Security", Icons.Filled.Block),
    APPEARANCE("Appearance & Theme", Icons.Filled.Palette),
    HISTORY("Call History & Data", Icons.Filled.Delete),
    TELECOM("Cellular & Telecom", Icons.Filled.Speed),
    PERMISSIONS("Permissions & Access", Icons.Filled.Security),
    ABOUT("About & Info", Icons.Filled.Info)
}

sealed class SettingsSearchAction {
    /**
     * Direct toggle switch displayed in search results.
     */
    data class Toggle(
        val isChecked: (SettingsUiState) -> Boolean,
        val onToggle: (Boolean) -> Unit
    ) : SettingsSearchAction()

    /**
     * Navigation to a settings sub-screen.
     */
    data class Navigate(
        val onNavigate: () -> Unit
    ) : SettingsSearchAction()

    /**
     * Option selection with current value pill and direct action.
     */
    data class Select(
        val currentValue: (SettingsUiState) -> String,
        val onAction: () -> Unit
    ) : SettingsSearchAction()

    /**
     * Explicit button action (e.g. Export, Clear, Cleanup).
     */
    data class Action(
        val buttonLabel: String,
        val onAction: () -> Unit
    ) : SettingsSearchAction()
}

data class SettingsSearchItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: SettingsCategory,
    val icon: ImageVector,
    val keywords: List<String>,
    val action: SettingsSearchAction,
    val score: Int = 0
)

data class SettingsSearchCallbacks(
    val onRequestDefaultDialer: () -> Unit = {},
    val onNavigateToBlocking: () -> Unit = {},
    val onNavigateToDualSim: () -> Unit = {},
    val onNavigateToPermissions: () -> Unit = {},
    val onNavigateToVoicemail: () -> Unit = {},
    val onNavigateToCallForwarding: () -> Unit = {},
    val onNavigateToCallBarring: () -> Unit = {},
    val onNavigateToCallRules: () -> Unit = {},
    val onNavigateToDiagnostics: () -> Unit = {},
    val onNavigateToStats: () -> Unit = {},
    val onNavigateToBackup: () -> Unit = {},
    val onNavigateToAbout: () -> Unit = {},
    val onNavigateToRingtone: () -> Unit = {},
    val onExportCsv: () -> Unit = {},
    val onCleanHistoryNow: () -> Unit = {},
    val onShowClearHistoryDialog: () -> Unit = {},
    val onScrollToSection: (String) -> Unit = {},
    val onOpenEscapeCall: () -> Unit = {},
    val onOpenEscapeSettings: () -> Unit = {},
    val onOpenEscapeHelp: () -> Unit = {}
)

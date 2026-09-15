package com.ryanshelby.linea.ui.screens.settings.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Voicemail
import com.ryanshelby.linea.data.preferences.LineaPreferences
import com.ryanshelby.linea.ui.screens.settings.SettingsUiState
import com.ryanshelby.linea.ui.screens.settings.SettingsViewModel

object SettingsSearchEngine {

    /**
     * Builds the complete catalog of searchable settings items.
     */
    fun buildCatalog(
        viewModel: SettingsViewModel,
        callbacks: SettingsSearchCallbacks
    ): List<SettingsSearchItem> {
        val list = mutableListOf<SettingsSearchItem>()

        // 1. Dual SIM Management
        list.add(
            SettingsSearchItem(
                id = "dual_sim_mgmt",
                title = "Dual SIM Management",
                subtitle = "Configure multi-SIM accounts, slot affinities, and prompt behaviors",
                category = SettingsCategory.GENERAL,
                icon = Icons.Filled.SimCard,
                keywords = listOf(
                    "sim", "dual sim", "sim 1", "sim 2", "carrier", "network", "slot", "cellular",
                    "esim", "multi sim", "subscription", "affinity", "prefer sim", "phone account"
                ),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToDualSim)
            )
        )

        // 2. Default Calling SIM
        list.add(
            SettingsSearchItem(
                id = "default_calling_sim",
                title = "Default Calling SIM",
                subtitle = "Choose SIM 1, SIM 2, or prompt before every call",
                category = SettingsCategory.GENERAL,
                icon = Icons.Filled.SimCard,
                keywords = listOf("default sim", "sim 1", "sim 2", "always ask", "prompt", "primary sim", "preferred sim"),
                action = SettingsSearchAction.Select(
                    currentValue = { state ->
                        when {
                            state.askSimBeforeDial -> "Always Ask"
                            state.defaultSim == 0 -> "SIM 1"
                            else -> "SIM 2"
                        }
                    },
                    onAction = callbacks.onNavigateToDualSim
                )
            )
        )

        // 3. SIM Selector Position
        list.add(
            SettingsSearchItem(
                id = "sim_selector_position",
                title = "SIM Selector Position",
                subtitle = "Switch between centered floating dialog and bottom sheet",
                category = SettingsCategory.GENERAL,
                icon = Icons.Filled.SimCard,
                keywords = listOf("sim position", "popup", "middle", "bottom", "sheet", "dialog", "center", "selector"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToDualSim)
            )
        )

        // 4. Call Screening & Blocking
        list.add(
            SettingsSearchItem(
                id = "blocking_and_screening",
                title = "Call Screening & Blocking",
                subtitle = "Block private, unknown, international, and non-contact numbers",
                category = SettingsCategory.SECURITY,
                icon = Icons.Filled.Block,
                keywords = listOf(
                    "block", "spam", "blacklist", "screen", "reject", "private", "unknown",
                    "international", "non-contacts", "vip", "whitelist", "allowlist", "filter"
                ),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToBlocking)
            )
        )

        // 5. Smart Rules & Schedules
        list.add(
            SettingsSearchItem(
                id = "smart_rules_schedules",
                title = "Smart Rules & Schedules",
                subtitle = "Automated nighttime quiet hours and workday focus rules",
                category = SettingsCategory.SECURITY,
                icon = Icons.Filled.Schedule,
                keywords = listOf(
                    "rules", "schedules", "quiet hours", "dnd", "do not disturb", "night",
                    "workday", "focus", "time based", "auto silence", "sleep"
                ),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToCallRules)
            )
        )

        // 6. Visual Voicemail
        list.add(
            SettingsSearchItem(
                id = "visual_voicemail",
                title = "Visual Voicemail",
                subtitle = "On-device audio inbox with carrier sync and playback",
                category = SettingsCategory.TELECOM,
                icon = Icons.Filled.Voicemail,
                keywords = listOf("voicemail", "vvm", "inbox", "audio message", "carrier mailbox", "messages", "playback"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToVoicemail)
            )
        )

        // 7. Call Forwarding
        list.add(
            SettingsSearchItem(
                id = "call_forwarding",
                title = "Call Forwarding",
                subtitle = "Unconditional and conditional carrier divert configurations",
                category = SettingsCategory.TELECOM,
                icon = Icons.AutoMirrored.Filled.PhoneForwarded,
                keywords = listOf("forward", "divert", "call divert", "unconditional", "busy", "unanswered", "unreachable", "carrier"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToCallForwarding)
            )
        )

        // 8. Call Barring & FDN
        list.add(
            SettingsSearchItem(
                id = "call_barring_fdn",
                title = "Call Barring & Fixed Dialing (FDN)",
                subtitle = "Network restriction locks and authorized whitelist numbers",
                category = SettingsCategory.TELECOM,
                icon = Icons.Filled.Security,
                keywords = listOf("barring", "fdn", "fixed dialing", "restrict", "pin2", "international barring", "outgoing lock"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToCallBarring)
            )
        )

        // 9. Dialer Profile (Personal / Work)
        list.add(
            SettingsSearchItem(
                id = "dialer_profile",
                title = "Dialer Profile (Personal / Work)",
                subtitle = "Switch smart routing rules, default SIM affinity, and focus filters",
                category = SettingsCategory.GENERAL,
                icon = Icons.Filled.Person,
                keywords = listOf("profile", "personal", "work", "environment", "workspace", "focus", "mode"),
                action = SettingsSearchAction.Select(
                    currentValue = { _ -> "Profile" },
                    onAction = { callbacks.onScrollToSection("profile") }
                )
            )
        )

        // 10. Cellular Diagnostics
        list.add(
            SettingsSearchItem(
                id = "cellular_diagnostics",
                title = "Cellular Diagnostics",
                subtitle = "Real-time signal dBm, 5G/LTE band, VoLTE & VoWiFi telemetry",
                category = SettingsCategory.TELECOM,
                icon = Icons.Filled.Speed,
                keywords = listOf("diagnostics", "signal", "dbm", "5g", "lte", "radio", "volte", "vowifi", "telemetry", "asu", "tower"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToDiagnostics)
            )
        )

        // 11. Call Analytics & Stats
        list.add(
            SettingsSearchItem(
                id = "call_analytics_stats",
                title = "Call Analytics & Stats",
                subtitle = "Total talk time, incoming/outgoing metrics, top frequent contacts",
                category = SettingsCategory.HISTORY,
                icon = Icons.Filled.BarChart,
                keywords = listOf("stats", "analytics", "metrics", "talk time", "duration", "frequency", "charts", "summary"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToStats)
            )
        )

        // 12. Backup & Data Migration
        list.add(
            SettingsSearchItem(
                id = "backup_migration",
                title = "Backup & Data Migration",
                subtitle = "Export and restore encrypted JSON archives for logs and rules",
                category = SettingsCategory.HISTORY,
                icon = Icons.Filled.Backup,
                keywords = listOf("backup", "restore", "migration", "export", "import", "archive", "json", "transfer"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToBackup)
            )
        )

        // 13. Call Duration Warning
        list.add(
            SettingsSearchItem(
                id = "call_duration_warning",
                title = "Call Duration Warning",
                subtitle = "Vibrates and alerts when active call exceeds threshold (5, 10, 15 min)",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Schedule,
                keywords = listOf("duration warning", "timer", "limit", "alert", "talk time limit", "call length", "reminder"),
                action = SettingsSearchAction.Select(
                    currentValue = { state ->
                        if (state.callDurationWarningMinutes == 0) "Off" else "${state.callDurationWarningMinutes} Min"
                    },
                    onAction = { callbacks.onScrollToSection("duration_warning") }
                )
            )
        )

        // 14. Confirm Before Calling
        list.add(
            SettingsSearchItem(
                id = "call_confirmation",
                title = "Confirm Before Calling",
                subtitle = "Prevents accidental dials with a countdown prompt",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Phone,
                keywords = listOf("confirm", "accidental", "prevent dial", "safety prompt", "countdown", "before call", "pocket dial"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.callConfirmation },
                    onToggle = { viewModel.setCallConfirmation(it) }
                )
            )
        )

        // 15. Proximity Sensor (Ear detection)
        list.add(
            SettingsSearchItem(
                id = "proximity_sensor",
                title = "Proximity Sensor",
                subtitle = "Turns off screen when held to ear during active calls",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Phone,
                keywords = listOf("proximity", "ear", "screen off", "face detection", "black screen", "sensor", "held to ear"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.proximitySensorEnabled },
                    onToggle = { viewModel.setProximitySensor(it) }
                )
            )
        )

        // 16. Don't Interrupt Me Mode (Floating mini banner)
        list.add(
            SettingsSearchItem(
                id = "dont_interrupt_me",
                title = "Don't Interrupt Me Mode",
                subtitle = "Incoming calls float as unobtrusive top banner instead of full screen",
                category = SettingsCategory.CALLING,
                icon = Icons.Filled.Phone,
                keywords = listOf("dont interrupt", "floating", "banner", "mini call", "heads up", "compact", "gaming", "overlay"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.dontInterruptMe },
                    onToggle = { viewModel.setDontInterruptMe(it) }
                )
            )
        )

        // 17. Repeated Call Emergency Override
        list.add(
            SettingsSearchItem(
                id = "repeat_call_override",
                title = "Repeated Call Emergency Override",
                subtitle = "3 calls within 5 minutes bypass quiet hours and block rules",
                category = SettingsCategory.SECURITY,
                icon = Icons.Filled.Security,
                keywords = listOf("emergency", "repeat call", "urgent", "bypass quiet hours", "override block", "breakthrough"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.repeatCallOverride },
                    onToggle = { viewModel.setRepeatCallOverride(it) }
                )
            )
        )

        // 18. Phone Ringtone
        list.add(
            SettingsSearchItem(
                id = "phone_ringtone",
                title = "Phone Ringtone",
                subtitle = "Select Linea Signature chime or Android system default ringtone",
                category = SettingsCategory.AUDIO,
                icon = Icons.Filled.MusicNote,
                keywords = listOf("ringtone", "melody", "sound", "ring", "incoming sound", "audio", "chime", "tune", "bell"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToRingtone)
            )
        )

        // 19. Dial Pad DTMF Tones
        list.add(
            SettingsSearchItem(
                id = "dialpad_sound",
                title = "Dial Pad DTMF Tones",
                subtitle = "Audible dual-tone multi-frequency key feedback while typing numbers",
                category = SettingsCategory.AUDIO,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                keywords = listOf("dtmf", "tones", "dialpad sound", "keypad sound", "beeps", "audio feedback", "dial sound"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.dialpadSound },
                    onToggle = { viewModel.setDialpadSound(it) }
                )
            )
        )

        // 20. Dial Pad Haptic Click
        list.add(
            SettingsSearchItem(
                id = "dialpad_vibration",
                title = "Dial Pad Haptic Click",
                subtitle = "Tactile vibration when tapping keypad digits",
                category = SettingsCategory.AUDIO,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                keywords = listOf("vibrate", "haptic", "click", "keypad vibration", "touch feedback", "tactile click"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.dialpadVibration },
                    onToggle = { viewModel.setDialpadVibration(it) }
                )
            )
        )

        // 21. Dialpad Haptic & Audio Profile
        list.add(
            SettingsSearchItem(
                id = "dialpad_haptic_profile",
                title = "Dialpad Haptic & Audio Profile",
                subtitle = "Choose Tactile Neumorphic, Mechanical Relay, Stealth, or Classic",
                category = SettingsCategory.AUDIO,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                keywords = listOf("profile", "tactile", "mechanical relay", "stealth", "classic", "haptics", "sound profile"),
                action = SettingsSearchAction.Select(
                    currentValue = { state ->
                        when (state.dialpadHapticProfile) {
                            LineaPreferences.DialpadHapticProfile.TITANIUM_GLASS -> "Tactile"
                            LineaPreferences.DialpadHapticProfile.MECHANICAL_RELAY -> "Mechanical"
                            LineaPreferences.DialpadHapticProfile.STEALTH -> "Stealth"
                            else -> "Classic"
                        }
                    },
                    onAction = { callbacks.onScrollToSection("haptic_profile") }
                )
            )
        )

        // 22. Call State Vibrations
        list.add(
            SettingsSearchItem(
                id = "call_vibration",
                title = "Call State Vibrations",
                subtitle = "Haptic pulse when calls connect and disconnect",
                category = SettingsCategory.AUDIO,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                keywords = listOf("call connect vibration", "disconnect vibrate", "haptic pulse", "call answer vibrate", "haptics"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.callVibration },
                    onToggle = { viewModel.setCallVibration(it) }
                )
            )
        )

        // 23. Flip to Silence
        list.add(
            SettingsSearchItem(
                id = "flip_to_silence",
                title = "Flip to Silence",
                subtitle = "Turn phone face-down during incoming call to mute ringer",
                category = SettingsCategory.GESTURES,
                icon = Icons.Filled.ScreenRotation,
                keywords = listOf("flip", "silence", "mute", "face down", "turn over", "gesture", "incoming mute", "quiet", "sensor"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.flipToSilence },
                    onToggle = { viewModel.setFlipToSilence(it) }
                )
            )
        )

        // 24. Proximity Wave to Silence
        list.add(
            SettingsSearchItem(
                id = "proximity_wave_to_silence",
                title = "Proximity Wave to Silence",
                subtitle = "Wave hand over top sensor during incoming call to mute ringer",
                category = SettingsCategory.GESTURES,
                icon = Icons.Filled.ScreenRotation,
                keywords = listOf("wave", "proximity wave", "hand wave", "hover", "gesture", "mute ringer", "silence", "sensor"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.proximityWaveToSilence },
                    onToggle = { viewModel.setProximityWaveToSilence(it) }
                )
            )
        )

        // 25. Color Theme
        list.add(
            SettingsSearchItem(
                id = "color_theme",
                title = "Color Theme",
                subtitle = "Choose Light Neumorphic, Dark Titanium, AMOLED OLED, or System",
                category = SettingsCategory.APPEARANCE,
                icon = Icons.Filled.Palette,
                keywords = listOf("theme", "dark mode", "light mode", "oled", "amoled", "black", "system theme", "appearance", "colors", "style"),
                action = SettingsSearchAction.Select(
                    currentValue = { it.theme },
                    onAction = { callbacks.onScrollToSection("theme") }
                )
            )
        )

        // 26. Reduce Animations
        list.add(
            SettingsSearchItem(
                id = "reduce_animations",
                title = "Reduce Animations",
                subtitle = "Swaps spring physics for fast cross-fades for maximum speed",
                category = SettingsCategory.APPEARANCE,
                icon = Icons.Filled.Speed,
                keywords = listOf("animation", "motion", "speed", "fast", "battery", "spring physics", "transitions", "performance"),
                action = SettingsSearchAction.Toggle(
                    isChecked = { it.reduceAnimations },
                    onToggle = { viewModel.setReduceAnimations(it) }
                )
            )
        )

        // 27. Auto-Cleanup Retention
        list.add(
            SettingsSearchItem(
                id = "history_retention",
                title = "Auto-Cleanup Retention",
                subtitle = "Automatically delete logs older than 30 days, 90 days, or keep Forever",
                category = SettingsCategory.HISTORY,
                icon = Icons.Filled.Delete,
                keywords = listOf("retention", "cleanup", "prune", "auto delete", "30 days", "90 days", "forever", "history cleanup"),
                action = SettingsSearchAction.Select(
                    currentValue = { state ->
                        when (state.cleanupDays) {
                            30 -> "30 Days"
                            90 -> "90 Days"
                            else -> "Forever"
                        }
                    },
                    onAction = { callbacks.onScrollToSection("retention") }
                )
            )
        )

        // 28. Run History Cleanup Now
        list.add(
            SettingsSearchItem(
                id = "run_cleanup_now",
                title = "Run History Cleanup Now",
                subtitle = "Immediately prune call logs exceeding the configured retention policy",
                category = SettingsCategory.HISTORY,
                icon = Icons.Filled.CleaningServices,
                keywords = listOf("clean now", "prune history", "delete old records", "storage cleanup"),
                action = SettingsSearchAction.Action(
                    buttonLabel = "Run Cleanup",
                    onAction = callbacks.onCleanHistoryNow
                )
            )
        )

        // 29. Export Call History (.csv)
        list.add(
            SettingsSearchItem(
                id = "export_csv",
                title = "Export Call History (.csv)",
                subtitle = "Generate a spreadsheet export of all logged incoming, outgoing, and missed calls",
                category = SettingsCategory.HISTORY,
                icon = Icons.Filled.FileDownload,
                keywords = listOf("export", "csv", "download logs", "spreadsheet", "excel", "backup csv", "call records"),
                action = SettingsSearchAction.Action(
                    buttonLabel = "Export CSV",
                    onAction = callbacks.onExportCsv
                )
            )
        )

        // 30. Clear Entire Call History
        list.add(
            SettingsSearchItem(
                id = "clear_all_history",
                title = "Clear Entire Call History",
                subtitle = "Permanently remove all call records from Linea's local database",
                category = SettingsCategory.HISTORY,
                icon = Icons.Filled.Delete,
                keywords = listOf("clear all", "wipe", "delete history", "erase logs", "reset call records"),
                action = SettingsSearchAction.Action(
                    buttonLabel = "Clear All",
                    onAction = callbacks.onShowClearHistoryDialog
                )
            )
        )

        // 31. Permissions Architecture
        list.add(
            SettingsSearchItem(
                id = "permissions_architecture",
                title = "Permissions Architecture",
                subtitle = "Inspect Telecom, contacts, audio, notifications, and lockscreen privileges",
                category = SettingsCategory.PERMISSIONS,
                icon = Icons.Filled.Security,
                keywords = listOf("permissions", "privileges", "access", "telecom", "contacts", "audio", "lockscreen", "security"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToPermissions)
            )
        )

        // 32. Default Phone App Role
        list.add(
            SettingsSearchItem(
                id = "default_dialer_role",
                title = "Default Phone App Role",
                subtitle = "Manage Android Telecom default dialer designation",
                category = SettingsCategory.PERMISSIONS,
                icon = Icons.Filled.Phone,
                keywords = listOf("default dialer", "default phone", "telecom role", "system dialer", "set as default"),
                action = SettingsSearchAction.Action(
                    buttonLabel = "Set Default",
                    onAction = callbacks.onRequestDefaultDialer
                )
            )
        )

        // 33. About Linea & FOSS
        list.add(
            SettingsSearchItem(
                id = "about_linea",
                title = "About Linea & FOSS",
                subtitle = "Developer info, architecture specifications, zero-telemetry policy, and source repository",
                category = SettingsCategory.ABOUT,
                icon = Icons.Filled.Info,
                keywords = listOf("about", "developer", "ryan shelby", "foss", "open source", "github", "version", "license", "privacy", "zero telemetry"),
                action = SettingsSearchAction.Navigate(callbacks.onNavigateToAbout)
            )
        )

        return list
    }

    /**
     * Executes intelligent keyword, title, synonym, and fuzzy token search.
     */
    fun search(
        query: String,
        catalog: List<SettingsSearchItem>
    ): List<SettingsSearchItem> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }

        val scoredItems = mutableListOf<SettingsSearchItem>()

        for (item in catalog) {
            val titleLower = item.title.lowercase()
            val subtitleLower = item.subtitle.lowercase()
            val categoryLower = item.category.displayName.lowercase()

            var score = 0

            // 1. Exact title match
            if (titleLower == trimmed) {
                score += 150
            } else if (titleLower.startsWith(trimmed)) {
                score += 100
            } else if (titleLower.contains(trimmed)) {
                score += 70
            }

            // 2. Keyword exact match
            for (kw in item.keywords) {
                val kwLower = kw.lowercase()
                if (kwLower == trimmed) {
                    score += 90
                } else if (kwLower.startsWith(trimmed)) {
                    score += 60
                } else if (kwLower.contains(trimmed)) {
                    score += 40
                }
            }

            // 3. Subtitle match
            if (subtitleLower.contains(trimmed)) {
                score += 35
            }

            // 4. Category match
            if (categoryLower.contains(trimmed)) {
                score += 25
            }

            // 5. Multi-token evaluation (if multiple words are typed)
            if (tokens.size > 1) {
                var allTokensFound = true
                for (token in tokens) {
                    val inTitle = titleLower.contains(token)
                    val inSubtitle = subtitleLower.contains(token)
                    val inKeywords = item.keywords.any { it.lowercase().contains(token) }
                    val inCat = categoryLower.contains(token)

                    if (inTitle || inSubtitle || inKeywords || inCat) {
                        score += 15
                    } else {
                        allTokensFound = false
                    }
                }
                if (allTokensFound) {
                    score += 40
                }
            }

            if (score > 0) {
                scoredItems.add(item.copy(score = score))
            }
        }

        return scoredItems.sortedByDescending { it.score }
    }
}

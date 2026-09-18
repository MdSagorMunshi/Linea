package com.ryanshelby.linea

import com.ryanshelby.linea.telecom.PostCallSummary
import com.ryanshelby.linea.telecom.screening.OfflineCallerIdEngine
import com.ryanshelby.linea.ui.screens.settings.search.SettingsCategory
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchAction
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchEngine
import com.ryanshelby.linea.ui.screens.settings.search.SettingsSearchItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PostCallHudTest {

    @Test
    fun postCallSummary_durationFormatting_handlesVariousLengths() {
        val summaryMissed = PostCallSummary(
            phoneNumber = "+14165550199",
            durationSeconds = 0,
            isIncoming = true,
            wasConnected = false
        )
        assertEquals("Missed", summaryMissed.formattedDuration)

        val summaryUnanswered = PostCallSummary(
            phoneNumber = "+14165550199",
            durationSeconds = 0,
            isIncoming = false,
            wasConnected = false
        )
        assertEquals("Unanswered", summaryUnanswered.formattedDuration)

        val summaryShort = PostCallSummary(
            phoneNumber = "+14165550199",
            durationSeconds = 45,
            isIncoming = true,
            wasConnected = true
        )
        assertEquals("0:45", summaryShort.formattedDuration)

        val summaryMinutes = PostCallSummary(
            phoneNumber = "+14165550199",
            durationSeconds = 125,
            isIncoming = false,
            wasConnected = true
        )
        assertEquals("2:05", summaryMinutes.formattedDuration)

        val summaryHours = PostCallSummary(
            phoneNumber = "+14165550199",
            durationSeconds = 3661,
            isIncoming = true,
            wasConnected = true
        )
        assertEquals("1:01:01", summaryHours.formattedDuration)
    }

    @Test
    fun postCallSummary_displayTitle_prefersCallerNameOverPhoneNumber() {
        val namedSummary = PostCallSummary(
            phoneNumber = "+8801712345678",
            callerName = "Ahmad Rahman",
            durationSeconds = 60,
            isIncoming = true,
            wasConnected = true
        )
        assertEquals("Ahmad Rahman", namedSummary.displayTitle)

        val unnamedSummary = PostCallSummary(
            phoneNumber = "+8801712345678",
            callerName = null,
            durationSeconds = 60,
            isIncoming = true,
            wasConnected = true
        )
        assertEquals("+8801712345678", unnamedSummary.displayTitle)

        val blankNamedSummary = PostCallSummary(
            phoneNumber = "+8801712345678",
            callerName = "   ",
            durationSeconds = 60,
            isIncoming = true,
            wasConnected = true
        )
        assertEquals("+8801712345678", blankNamedSummary.displayTitle)
    }

    @Test
    fun postCallSummary_integratesWithOfflineCallerId() {
        val bdSummary = PostCallSummary(
            phoneNumber = "8801712345678",
            durationSeconds = 30,
            isIncoming = true,
            wasConnected = true,
            callerIdResult = OfflineCallerIdEngine.identifyNumber("8801712345678")
        )
        assertNotNull(bdSummary.callerIdResult)
        assertEquals("Bangladesh", bdSummary.callerIdResult?.countryName)
        assertEquals("🇧🇩", bdSummary.callerIdResult?.flagEmoji)

        val palestineSummary = PostCallSummary(
            phoneNumber = "972501234567",
            durationSeconds = 12,
            isIncoming = true,
            wasConnected = true,
            callerIdResult = OfflineCallerIdEngine.identifyNumber("972501234567")
        )
        assertNotNull(palestineSummary.callerIdResult)
        assertEquals("Palestine", palestineSummary.callerIdResult?.countryName)

        val chinaSummary = PostCallSummary(
            phoneNumber = "886912345678",
            durationSeconds = 12,
            isIncoming = true,
            wasConnected = true,
            callerIdResult = OfflineCallerIdEngine.identifyNumber("886912345678")
        )
        assertNotNull(chinaSummary.callerIdResult)
        assertEquals("China", chinaSummary.callerIdResult?.countryName)
    }

    @Test
    fun reminderDelayCalculation_isAccurate() {
        val fifteenMinMs = 15 * 60 * 1000L
        assertEquals(900000L, fifteenMinMs)

        val oneHourMs = 60 * 60 * 1000L
        assertEquals(3600000L, oneHourMs)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val tomorrow9AmDelay = target.timeInMillis - now.timeInMillis
        assertTrue("Tomorrow 9 AM delay must be positive", tomorrow9AmDelay > 0)
        assertTrue("Tomorrow 9 AM delay must be within 36 hours", tomorrow9AmDelay < 36 * 60 * 60 * 1000L)
    }

    @Test
    fun quickNotesAppendingLogic() {
        var note = ""
        val tag1 = "Follow-up needed"
        note = if (note.isBlank()) tag1 else "$note • $tag1"
        assertEquals("Follow-up needed", note)

        val tag2 = "Sent details"
        note = if (note.isBlank()) tag2 else "$note • $tag2"
        assertEquals("Follow-up needed • Sent details", note)
    }

    @Test
    fun settingsSearchEngine_indexesPostCallHud() {
        val testItem = SettingsSearchItem(
            id = "post_call_hud",
            title = "Post-Call Smart Action HUD",
            subtitle = "Transient card with 1-tap callback reminders, quick notes, SMS & block",
            category = SettingsCategory.CALLING,
            icon = Icons.Filled.Phone,
            keywords = listOf(
                "post-call", "post call", "hud", "quick action", "after call", "callback reminder",
                "scratchpad", "call notes", "quick sms", "follow-up", "block caller", "action card"
            ),
            action = SettingsSearchAction.Toggle(
                isChecked = { true },
                onToggle = {}
            )
        )

        val catalog = listOf(testItem)

        val hudResult = SettingsSearchEngine.search("hud", catalog)
        assertEquals(1, hudResult.size)
        assertEquals("post_call_hud", hudResult[0].id)

        val reminderResult = SettingsSearchEngine.search("callback reminder", catalog)
        assertEquals(1, reminderResult.size)
        assertEquals("post_call_hud", reminderResult[0].id)

        val scratchpadResult = SettingsSearchEngine.search("scratchpad", catalog)
        assertEquals(1, scratchpadResult.size)
        assertEquals("post_call_hud", scratchpadResult[0].id)

        val afterCallResult = SettingsSearchEngine.search("after call", catalog)
        assertEquals(1, afterCallResult.size)
        assertEquals("post_call_hud", afterCallResult[0].id)
    }

    @Test
    fun postCallHud_defaultsToDisabled_optInOnly() {
        val defaultUiState = com.ryanshelby.linea.ui.screens.settings.SettingsUiState()
        assertFalse("Post-Call Smart Action HUD must be disabled by default", defaultUiState.postCallHudEnabled)
        assertEquals(8, defaultUiState.postCallHudDurationSeconds)
    }

    @Test
    fun settingsSearchEngine_indexesPostCallHudTimeCustomization() {
        var currentDuration = 8
        var hudEnabled = true

        val timeItem = SettingsSearchItem(
            id = "post_call_hud_time",
            title = "Post-Call HUD Display Time",
            subtitle = "Customize how long the smart action HUD stays open before closing",
            category = SettingsCategory.CALLING,
            icon = Icons.Filled.Phone,
            keywords = listOf(
                "post call time", "post-call time", "hud time", "hud duration", "post call duration",
                "customize post call time", "auto dismiss time", "after call timer", "scratchpad duration",
                "hud delay", "timer", "seconds"
            ),
            action = SettingsSearchAction.Select(
                currentValue = { state ->
                    if (!state.postCallHudEnabled) "Disabled"
                    else if (state.postCallHudDurationSeconds <= 0) "Manual"
                    else "${state.postCallHudDurationSeconds}s"
                },
                onAction = {
                    currentDuration = when (currentDuration) {
                        3 -> 5
                        5 -> 8
                        8 -> 12
                        12 -> 15
                        15 -> 30
                        30 -> 0
                        0 -> 3
                        else -> 8
                    }
                }
            )
        )

        val catalog = listOf(timeItem)

        // Search matching
        val postCallTimeMatches = SettingsSearchEngine.search("post call time", catalog)
        assertEquals(1, postCallTimeMatches.size)
        assertEquals("post_call_hud_time", postCallTimeMatches[0].id)

        val customizeMatches = SettingsSearchEngine.search("customize post call time", catalog)
        assertEquals(1, customizeMatches.size)
        assertEquals("post_call_hud_time", customizeMatches[0].id)

        val timerMatches = SettingsSearchEngine.search("after call timer", catalog)
        assertEquals(1, timerMatches.size)

        // Test label calculation
        val selectAction = timeItem.action as SettingsSearchAction.Select
        val enabledState = com.ryanshelby.linea.ui.screens.settings.SettingsUiState(
            postCallHudEnabled = true,
            postCallHudDurationSeconds = 8
        )
        assertEquals("8s", selectAction.currentValue(enabledState))

        val manualState = com.ryanshelby.linea.ui.screens.settings.SettingsUiState(
            postCallHudEnabled = true,
            postCallHudDurationSeconds = 0
        )
        assertEquals("Manual", selectAction.currentValue(manualState))

        val disabledState = com.ryanshelby.linea.ui.screens.settings.SettingsUiState(
            postCallHudEnabled = false,
            postCallHudDurationSeconds = 8
        )
        assertEquals("Disabled", selectAction.currentValue(disabledState))

        // Test preset cycling
        assertEquals(8, currentDuration)
        selectAction.onAction()
        assertEquals(12, currentDuration)
        selectAction.onAction()
        assertEquals(15, currentDuration)
        selectAction.onAction()
        assertEquals(30, currentDuration)
        selectAction.onAction()
        assertEquals(0, currentDuration) // Manual mode
        selectAction.onAction()
        assertEquals(3, currentDuration) // Wraps to 3s Fast
    }
}


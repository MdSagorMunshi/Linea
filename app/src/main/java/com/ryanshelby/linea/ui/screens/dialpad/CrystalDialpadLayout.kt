package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.ui.components.CrystalIconButton
import com.ryanshelby.linea.ui.components.crystalSurface
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CrystalDialpadLayout(
    number: String, profile: String, isFiery: Boolean,
    sound: Boolean, vibration: Boolean, hapticProfile: String,
    clipboard: String?, onPaste: () -> Unit, onCopy: () -> Unit,
    onDigit: (Char) -> Unit, onHoldDigit: (Char) -> Unit,
    onCall: () -> Unit, onToggleSkin: () -> Unit,
    onDelete: () -> Unit, onClear: () -> Unit, onCreate: () -> Unit,
    onScan: () -> Unit, onSwitchProfile: () -> Unit,
    status: @Composable () -> Unit, suggestions: @Composable () -> Unit,
    simSelector: @Composable () -> Unit, timezone: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        val compact = maxHeight < 740.dp
        val keySize = if (compact) 64.dp else 72.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp).padding(bottom = 102.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("PHONE", style = LineaTypography.labelSmall.copy(letterSpacing = 2.sp), color = LineaColors.TextSecondary)
                    Text("Let’s talk.", style = LineaTypography.headlineLarge, color = LineaColors.TextPrimary)
                }
                Text(profile.lowercase().replaceFirstChar { it.uppercase() },
                    modifier = Modifier.crystalSurface(RoundedCornerShape(22.dp)).combinedClickable(
                        role = Role.Button, onClick = onSwitchProfile).padding(horizontal = 14.dp, vertical = 14.dp),
                    color = LineaColors.TextPrimary, style = LineaTypography.labelLarge)
                Spacer(Modifier.width(8.dp))
                CrystalIconButton(Icons.Filled.QrCodeScanner, "Scan QR Code", onScan)
            }
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                status()
                suggestions()
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)
                    .crystalSurface(RoundedCornerShape(30.dp), elevation = 8.dp)
                    .combinedClickable(role = Role.Button, onClickLabel = if (number.isEmpty()) "Paste number" else "Copy number",
                        onClick = { if (number.isEmpty()) onPaste() else onCopy() },
                        onLongClickLabel = "Paste number", onLongClick = onPaste)
                    .padding(horizontal = 14.dp, vertical = if (compact) 16.dp else 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (number.isEmpty()) "A conversation starts here" else "READY TO CONNECT",
                        color = LineaColors.TextSecondary, style = LineaTypography.labelSmall,
                        textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text(if (number.isEmpty()) "Enter a number" else number,
                        style = if (number.isEmpty()) LineaTypography.titleLarge else LineaTypography.displayLarge,
                        fontSize = if (number.isEmpty()) 24.sp else if (number.length > 12) 28.sp else 38.sp,
                        color = LineaColors.TextPrimary, textAlign = TextAlign.Center, maxLines = 2)
                    if (number.isEmpty() && !clipboard.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ContentPaste, null, Modifier.size(14.dp), tint = LineaColors.TitaniumBlue)
                            Spacer(Modifier.width(6.dp))
                            Text("Tap to paste", color = LineaColors.TitaniumBlue, style = LineaTypography.labelMedium)
                        }
                    }
                }
                timezone()
                simSelector()
            }
            Spacer(Modifier.height(12.dp))
            val digits = listOf('1' to "", '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL", '6' to "MNO",
                '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '*' to "", '0' to "+", '#' to "")
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                digits.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { (digit, letters) ->
                            DialpadKey(digit, letters, onDigit, onLongPress = { onHoldDigit(digit) },
                                size = keySize, soundEnabled = sound, vibrationEnabled = vibration, hapticProfile = hapticProfile)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly) {
                CrystalIconButton(Icons.Filled.PersonAdd, "Add contact", onCreate, enabled = number.isNotEmpty())
                CallButton(isFiery, onCall, onToggleSkin)
                CrystalIconButton(Icons.Filled.Backspace, "Delete digit", onDelete,
                    enabled = number.isNotEmpty(), onLongClick = onClear, longClickLabel = "Clear number")
            }
        }
    }
}

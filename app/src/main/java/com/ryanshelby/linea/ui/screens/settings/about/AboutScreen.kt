package com.ryanshelby.linea.ui.screens.settings.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.R
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

private const val DEVELOPER_NAME = "Ryan Shelby"
private const val SUPPORT_EMAIL = "ryn@disr.it"
private const val FOSS_REPO_URL = "https://github.com/MdSagorMunshi/Linea.git"
private const val FOSS_WEB_URL = "https://github.com/MdSagorMunshi/Linea"

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val packageInfo = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    val versionName = packageInfo?.versionName ?: "2.0.0"
    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo?.longVersionCode?.toString() ?: "1"
    } else {
        @Suppress("DEPRECATION")
        packageInfo?.versionCode?.toString() ?: "1"
    }

    val copyToClipboard = { text: String, message: String ->
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val openUrl = { url: String ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    val sendEmail = {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "Linea Dialer Inquiry & Feedback")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            copyToClipboard(SUPPORT_EMAIL, "Support email copied to clipboard")
        }
    }

    val shareApp = {
        try {
            val shareText = "Check out Linea - Free & Open Source Sovereign Cellular Dialer for Android: $FOSS_WEB_URL"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Linea Dialer")
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Linea").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            copyToClipboard(FOSS_REPO_URL, "Repository link copied to clipboard")
        }
    }

    val targetSdkVersion = context.applicationInfo.targetSdkVersion
    val targetSdkLabel = when (targetSdkVersion) {
        37 -> "Android 17 (API 37)"
        36 -> "Android 16 (API 36)"
        35 -> "Android 15 (API 35)"
        else -> "Android (API $targetSdkVersion)"
    }
    val runningOsLabel = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val minSdkLabel = "Android 11 (API 30)"

    val copyDiagnostics = {
        val diagnostics = """
            Linea Dialer Diagnostic Info
            App Version: $versionName ($versionCode)
            Package: ${context.packageName}
            Developer: $DEVELOPER_NAME
            Support Email: $SUPPORT_EMAIL
            FOSS Repository: $FOSS_REPO_URL
            Target SDK: $targetSdkLabel
            Running OS: $runningOsLabel
            Minimum SDK: $minSdkLabel
            Device Model: ${Build.MANUFACTURER} ${Build.MODEL}
            Security Engine: On-Device SQLite AES-256 Vault
        """.trimIndent()
        copyToClipboard(diagnostics, "App diagnostic info copied to clipboard")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(LineaColors.BackgroundTop, LineaColors.BackgroundBottom)
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LineaDimensions.ScreenPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = LineaColors.TextPrimary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = shareApp) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Linea",
                            tint = LineaColors.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hero Brand Identity Card
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(LineaColors.SurfaceElevated)
                            .border(
                                width = 1.5.dp,
                                color = LineaColors.TitaniumBlue.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_linea_logo),
                            contentDescription = "Linea Logo",
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Linea Dialer",
                        style = LineaTypography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = LineaColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Sovereign Cellular Telephony Suite",
                        style = LineaTypography.bodyMedium,
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Version Pill Badge
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                            .border(1.dp, LineaColors.TitaniumBlue.copy(alpha = 0.3f), CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(LineaColors.AccentGreen)
                        )
                        Text(
                            text = "Version $versionName ($versionCode) • FOSS",
                            style = LineaTypography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LineaColors.TitaniumBlue
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Feature Pill Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        FeatureChip(label = "100% On-Device")
                        Spacer(modifier = Modifier.width(6.dp))
                        FeatureChip(label = "Zero Telemetry")
                        Spacer(modifier = Modifier.width(6.dp))
                        FeatureChip(label = "AES-256 Vault")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Developer Info Card
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LineaColors.TitaniumBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = LineaColors.TitaniumBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LEAD DEVELOPER",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TitaniumBlue,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = DEVELOPER_NAME,
                                style = LineaTypography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = LineaColors.TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Designed and engineered as a sovereign, privacy-preserving cellular dialer without cloud dependencies, surveillance trackers, or third-party ad networks.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Contact Email Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LineaColors.SurfaceElevated.copy(alpha = 0.6f))
                            .border(1.dp, LineaColors.GlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Official Support Email",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextTertiary
                            )
                            Text(
                                text = SUPPORT_EMAIL,
                                style = LineaTypography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = LineaColors.TextPrimary
                            )
                        }
                        IconButton(
                            onClick = { copyToClipboard(SUPPORT_EMAIL, "Support email copied to clipboard") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Email",
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = sendEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LineaColors.TitaniumBlue,
                            contentColor = LineaColors.TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email Developer & Support",
                            style = LineaTypography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // FOSS & Source Code Card
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(LineaColors.AccentGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                tint = LineaColors.AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FREE & OPEN SOURCE SOFTWARE (FOSS)",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.AccentGreen,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                text = "Auditable & Community Driven",
                                style = LineaTypography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = LineaColors.TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Linea is distributed under an open source license. The codebase is fully transparent, verifiable, and free for inspection, contributions, and community audits.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Repo Link Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LineaColors.SurfaceElevated.copy(alpha = 0.6f))
                            .border(1.dp, LineaColors.GlassBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = LineaColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Git Repository URL",
                                style = LineaTypography.labelSmall,
                                color = LineaColors.TextTertiary
                            )
                            Text(
                                text = FOSS_REPO_URL,
                                style = LineaTypography.bodySmall,
                                fontWeight = FontWeight.Normal,
                                color = LineaColors.TextPrimary,
                                maxLines = 1
                            )
                        }
                        IconButton(
                            onClick = { copyToClipboard(FOSS_REPO_URL, "FOSS repository link copied to clipboard") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Repo URL",
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { openUrl(FOSS_WEB_URL) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LineaColors.AccentGreen,
                                contentColor = LineaColors.TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open GitHub",
                                style = LineaTypography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { copyToClipboard(FOSS_REPO_URL, "Git URL copied") },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LineaColors.GlassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LineaColors.TextPrimary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy Git URL",
                                style = LineaTypography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Architecture & Privacy Pillars Card
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "CORE PILLARS",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        text = "Privacy & Security Architecture",
                        style = LineaTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ArchitectureItem(
                        icon = Icons.Filled.VerifiedUser,
                        title = "Zero Cloud Telemetry",
                        description = "No remote analytics, crash tracking, user identifiers, or network telemetry."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ArchitectureItem(
                        icon = Icons.Filled.Lock,
                        title = "AES-256-GCM Encrypted Vault",
                        description = "Local private safe and call notes protected with hardware-backed master keys."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ArchitectureItem(
                        icon = Icons.Filled.Security,
                        title = "Native Android Telecom Framework",
                        description = "Direct InCallService and ConnectionService integration for carrier-grade stability."
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ArchitectureItem(
                        icon = Icons.Filled.PhoneAndroid,
                        title = "Dual SIM & Carrier Intelligence",
                        description = "Real-time subscription management, per-contact SIM affinity, and MMI/USSD support."
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // System Diagnostics Quick Copy Card
            FrostedGlassBox(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "APPLICATION DIAGNOSTICS",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextTertiary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                    Text(
                        text = "Build & Environment Specs",
                        style = LineaTypography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticRow(label = "Application ID", value = context.packageName)
                    DiagnosticRow(label = "Target SDK", value = targetSdkLabel)
                    DiagnosticRow(label = "Running OS", value = runningOsLabel)
                    DiagnosticRow(label = "Minimum SDK", value = minSdkLabel)
                    DiagnosticRow(label = "Architecture", value = "Jetpack Compose • Room • Hilt • Telecom")
                    DiagnosticRow(label = "License", value = "Open Source (FOSS)")

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = copyDiagnostics,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LineaColors.GlassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = LineaColors.TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copy Diagnostic Report",
                            style = LineaTypography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Copyright
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Crafted with precision by $DEVELOPER_NAME",
                    style = LineaTypography.labelMedium,
                    color = LineaColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Linea Telephony Project • Sovereign & FOSS",
                    style = LineaTypography.bodySmall,
                    color = LineaColors.TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun FeatureChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LineaColors.SurfaceElevated)
            .border(LineaDimensions.HairlineBorder, LineaColors.GlassBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = LineaTypography.labelSmall,
            color = LineaColors.TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ArchitectureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(LineaColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LineaColors.TitaniumBlue,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = LineaTypography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = LineaTypography.bodySmall,
            color = LineaColors.TextTertiary
        )
        Text(
            text = value,
            style = LineaTypography.labelMedium,
            color = LineaColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

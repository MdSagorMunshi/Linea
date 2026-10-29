package com.ryanshelby.linea.smartfilter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ryanshelby.linea.ui.components.neumorphic
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFilterScreen(
    onNavigateBack: () -> Unit,
    viewModel: SmartFilterViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearUserMessage()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LineaColors.BackgroundDeep,
        modifier = modifier.fillMaxSize().statusBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LineaDimensions.ScreenPadding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = LineaColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LiNEA SF",
                                style = LineaTypography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = LineaColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Smart Filter",
                                    style = LineaTypography.labelSmall,
                                    color = LineaColors.TitaniumBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Text(
                            text = "Duplicates, Cleanup & Quality Scoring",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (canUndo) {
                        IconButton(onClick = { viewModel.undoLastAction() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = LineaColors.MutedSageGreen
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.openRulesSheet() }) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Rules",
                            tint = LineaColors.TextPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.scanContacts() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Scan",
                            tint = LineaColors.TitaniumBlue
                        )
                    }
                }
            }

            // Quick Filter Tabs
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                item {
                    TabChip(
                        label = "Overview",
                        count = null,
                        isSelected = uiState.currentTab == SmartFilterTab.OVERVIEW,
                        onClick = { viewModel.setTab(SmartFilterTab.OVERVIEW) }
                    )
                }
                item {
                    TabChip(
                        label = "Duplicates",
                        count = uiState.duplicateMatches.size,
                        isSelected = uiState.currentTab == SmartFilterTab.DUPLICATES,
                        onClick = { viewModel.setTab(SmartFilterTab.DUPLICATES) }
                    )
                }
                item {
                    TabChip(
                        label = "Name Cleanup",
                        count = uiState.nameCleanups.size,
                        isSelected = uiState.currentTab == SmartFilterTab.CLEANUP,
                        onClick = { viewModel.setTab(SmartFilterTab.CLEANUP) }
                    )
                }
                item {
                    TabChip(
                        label = "Incomplete",
                        count = uiState.incompleteContacts.size,
                        isSelected = uiState.currentTab == SmartFilterTab.INCOMPLETE,
                        onClick = { viewModel.setTab(SmartFilterTab.INCOMPLETE) }
                    )
                }
                item {
                    TabChip(
                        label = "Duplicate Numbers",
                        count = uiState.duplicateNumbers.size,
                        isSelected = uiState.currentTab == SmartFilterTab.DUPLICATE_NUMBERS,
                        onClick = { viewModel.setTab(SmartFilterTab.DUPLICATE_NUMBERS) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analyzing contacts and detecting duplicates...",
                            style = LineaTypography.bodyMedium,
                            color = LineaColors.TextSecondary
                        )
                    }
                }
            } else {
                when (uiState.currentTab) {
                    SmartFilterTab.OVERVIEW -> {
                        OverviewTabContent(
                            uiState = uiState,
                            onCleanAllNames = { viewModel.cleanAllNames() },
                            onMergeAllDuplicates = { viewModel.bulkMergeAll() },
                            onGoToDuplicates = { viewModel.setTab(SmartFilterTab.DUPLICATES) },
                            onGoToCleanup = { viewModel.setTab(SmartFilterTab.CLEANUP) },
                            onGoToIncomplete = { viewModel.setTab(SmartFilterTab.INCOMPLETE) },
                            onGoToDuplicateNumbers = { viewModel.setTab(SmartFilterTab.DUPLICATE_NUMBERS) }
                        )
                    }
                    SmartFilterTab.DUPLICATES -> {
                        DuplicatesTabContent(
                            matches = uiState.duplicateMatches,
                            onPreviewMerge = { viewModel.openMergePreview(it) },
                            onDismissDuplicate = { viewModel.dismissDuplicate(it) },
                            onBulkMerge = { viewModel.bulkMergeAll() }
                        )
                    }
                    SmartFilterTab.CLEANUP -> {
                        CleanupTabContent(
                            items = uiState.nameCleanups,
                            onCleanName = { viewModel.cleanContactName(it) },
                            onDismissCleanup = { viewModel.dismissCleanup(it) },
                            onCleanAll = { viewModel.cleanAllNames() }
                        )
                    }
                    SmartFilterTab.INCOMPLETE -> {
                        IncompleteTabContent(
                            items = uiState.incompleteContacts,
                            onDismissIncomplete = { viewModel.dismissIncomplete(it) }
                        )
                    }
                    SmartFilterTab.DUPLICATE_NUMBERS -> {
                        DuplicateNumbersTabContent(groups = uiState.duplicateNumbers)
                    }
                }
            }
        }
    }

    // Merge Preview Sheet
    uiState.activeMergePreview?.let { preview ->
        MergePreviewSheet(
            preview = preview,
            onDismiss = { viewModel.dismissMergePreview() },
            onConfirmMerge = { viewModel.confirmMerge(preview) }
        )
    }

    // Rules Configuration Sheet
    if (uiState.showRulesSheet) {
        SmartFilterRulesSheet(
            config = uiState.config,
            onDismiss = { viewModel.dismissRulesSheet() },
            onSaveConfig = { newConfig ->
                viewModel.updateConfig(newConfig)
                viewModel.dismissRulesSheet()
            }
        )
    }
}

// --- SUBVIEWS & COMPONENTS ---

@Composable
private fun TabChip(
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) LineaColors.TitaniumBlue else LineaColors.NeuSurfaceRaised
    val contentColor = if (isSelected) Color.White else LineaColors.TextPrimary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(
                LineaDimensions.HairlineBorder,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.NeuBorderHighlight.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = LineaTypography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.25f) else LineaColors.TitaniumBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = LineaTypography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isSelected) Color.White else LineaColors.TitaniumBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    uiState: SmartFilterUiState,
    onCleanAllNames: () -> Unit,
    onMergeAllDuplicates: () -> Unit,
    onGoToDuplicates: () -> Unit,
    onGoToCleanup: () -> Unit,
    onGoToIncomplete: () -> Unit,
    onGoToDuplicateNumbers: () -> Unit
) {
    val report = uiState.qualityReport

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Health Score Card
        item {
            report?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(
                            shape = RoundedCornerShape(20.dp),
                            elevation = 4.dp,
                            surfaceColor = LineaColors.NeuSurfaceRaised
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Contact Quality Score",
                                    style = LineaTypography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = LineaColors.TextPrimary
                                )
                                Text(
                                    text = "Completeness & Consistency Rating",
                                    style = LineaTypography.bodySmall,
                                    color = LineaColors.TextSecondary
                                )
                            }

                            // Score Badge
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (it.letterGrade) {
                                            "A+", "A" -> LineaColors.MutedSageGreen.copy(alpha = 0.15f)
                                            "B" -> LineaColors.TitaniumBlue.copy(alpha = 0.15f)
                                            else -> LineaColors.Warning.copy(alpha = 0.15f)
                                        }
                                    )
                                    .border(
                                        1.5.dp,
                                        when (it.letterGrade) {
                                            "A+", "A" -> LineaColors.MutedSageGreen
                                            "B" -> LineaColors.TitaniumBlue
                                            else -> LineaColors.Warning
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${it.overallScore}%",
                                        style = LineaTypography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LineaColors.TextPrimary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = it.letterGrade,
                                        style = LineaTypography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = when (it.letterGrade) {
                                            "A+", "A" -> LineaColors.MutedSageGreen
                                            "B" -> LineaColors.TitaniumBlue
                                            else -> LineaColors.Warning
                                        },
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score metrics progress bars
                        ScoreMetricRow(title = "Completeness", score = it.completenessScore, color = LineaColors.TitaniumBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreMetricRow(title = "Consistency", score = it.consistencyScore, color = LineaColors.MutedSageGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        ScoreMetricRow(title = "Hygiene", score = it.hygieneScore, color = LineaColors.Warning)
                    }
                }
            }
        }

        // Stats Summary Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    title = "Duplicates",
                    count = uiState.duplicateMatches.size,
                    color = LineaColors.Warning,
                    modifier = Modifier.weight(1f),
                    onClick = onGoToDuplicates
                )
                StatCard(
                    title = "Name Fixes",
                    count = uiState.nameCleanups.size,
                    color = LineaColors.TitaniumBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onGoToCleanup
                )
                StatCard(
                    title = "Incomplete",
                    count = uiState.incompleteContacts.size,
                    color = LineaColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onGoToIncomplete
                )
            }
        }

        // Google Contacts-style "Merge duplicates" actionable card
        if (uiState.duplicateMatches.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(
                            shape = RoundedCornerShape(18.dp),
                            elevation = 3.dp,
                            surfaceColor = LineaColors.NeuSurfaceRaised
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Merge,
                                    contentDescription = null,
                                    tint = LineaColors.Warning,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Merge duplicates",
                                    style = LineaTypography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LineaColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(LineaColors.Warning.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = uiState.duplicateMatches.size.toString(),
                                        style = LineaTypography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LineaColors.Warning,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Multiple entries detected for the same person across numbers, emails, or name variations.",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onGoToDuplicates,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.GlassBorder)
                            ) {
                                Text("Review (${uiState.duplicateMatches.size})", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onMergeAllDuplicates,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.MutedSageGreen)
                            ) {
                                Text("Merge all", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Google Contacts-style "Clean up names" actionable card
        if (uiState.nameCleanups.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neumorphic(
                            shape = RoundedCornerShape(18.dp),
                            elevation = 3.dp,
                            surfaceColor = LineaColors.NeuSurfaceRaised
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = null,
                                    tint = LineaColors.TitaniumBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Clean up names",
                                    style = LineaTypography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = LineaColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(LineaColors.TitaniumBlue.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = uiState.nameCleanups.size.toString(),
                                        style = LineaTypography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LineaColors.TitaniumBlue,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Inconsistent capitalization, extra whitespace, or formatting issues found.",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onGoToCleanup,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.GlassBorder)
                            ) {
                                Text("Review (${uiState.nameCleanups.size})", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onCleanAllNames,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue)
                            ) {
                                Text("Clean all", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Actionable Recommendations Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(
                        shape = RoundedCornerShape(20.dp),
                        elevation = 3.dp,
                        surfaceColor = LineaColors.NeuSurfaceRaised
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoFixHigh,
                            contentDescription = null,
                            tint = LineaColors.TitaniumBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Suggestions",
                            style = LineaTypography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = LineaColors.TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    report?.recommendations?.forEach { rec ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TitaniumBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rec,
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreMetricRow(title: String, score: Int, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Text(
                text = "$score%",
                style = LineaTypography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = LineaColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(LineaColors.GlassBorder.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = score / 100f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(LineaColors.NeuSurfaceRaised)
            .border(LineaDimensions.HairlineBorder, LineaColors.NeuBorderHighlight.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = count.toString(),
                style = LineaTypography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
        }
    }
}

@Composable
private fun DuplicatesTabContent(
    matches: List<DuplicateMatch>,
    onPreviewMerge: (DuplicateMatch) -> Unit,
    onDismissDuplicate: (DuplicateMatch) -> Unit,
    onBulkMerge: () -> Unit
) {
    if (matches.isEmpty()) {
        EmptyPlaceholder(
            title = "No Duplicates Found",
            message = "Every contact in your address book appears to be unique based on current matching sensitivity."
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${matches.size} Duplicate Candidate(s)",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary
                )
                Button(
                    onClick = onBulkMerge,
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Merge, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Merge all (${matches.size})", fontSize = 11.sp)
                }
            }
        }

        items(matches, key = { it.id }) { match ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(
                        shape = RoundedCornerShape(18.dp),
                        elevation = 3.dp,
                        surfaceColor = LineaColors.NeuSurfaceRaised
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Confidence Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (match.confidence >= 90) LineaColors.MutedSageGreen.copy(alpha = 0.2f)
                                    else LineaColors.Warning.copy(alpha = 0.2f)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${match.confidence}% Match",
                                style = LineaTypography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (match.confidence >= 90) LineaColors.MutedSageGreen else LineaColors.Warning
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { onDismissDuplicate(match) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextSecondary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.GlassBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Dismiss", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onPreviewMerge(match) },
                                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Merge, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview & Merge", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Contact A & Contact B side-by-side or stacked
                    Text(
                        text = "1. ${match.contactA.contact.displayName}",
                        style = LineaTypography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = match.contactA.numbers.joinToString { it.number },
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "2. ${match.contactB.contact.displayName}",
                        style = LineaTypography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = match.contactB.numbers.joinToString { it.number },
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = LineaColors.GlassBorder.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Why matched: ${match.explanation}",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TitaniumBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun CleanupTabContent(
    items: List<NameCleanupItem>,
    onCleanName: (NameCleanupItem) -> Unit,
    onDismissCleanup: (NameCleanupItem) -> Unit,
    onCleanAll: () -> Unit
) {
    if (items.isEmpty()) {
        EmptyPlaceholder(
            title = "All Names Clean!",
            message = "No inconsistent capitalization, duplicate spaces, or formatting issues found in your contact names."
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${items.size} Name Formatting Fix(es)",
                    style = LineaTypography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary
                )
                Button(
                    onClick = onCleanAll,
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clean All (${items.size})", fontSize = 11.sp)
                }
            }
        }

        items(items, key = { it.contactId }) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        surfaceColor = LineaColors.NeuSurfaceRaised
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.originalName,
                                style = LineaTypography.bodyMedium,
                                textDecoration = TextDecoration.LineThrough,
                                color = LineaColors.Danger.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "→",
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.proposedName,
                                style = LineaTypography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = LineaColors.MutedSageGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.reason,
                            style = LineaTypography.labelSmall,
                            color = LineaColors.TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { onDismissCleanup(item) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.GlassBorder),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dismiss", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onCleanName(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Fix", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncompleteTabContent(
    items: List<IncompleteContactItem>,
    onDismissIncomplete: (IncompleteContactItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyPlaceholder(
            title = "All Contacts Complete!",
            message = "Every contact has full names, phone numbers, and complete information."
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(items) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        surfaceColor = LineaColors.NeuSurfaceRaised
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.contact.displayName.ifBlank { "Unnamed Contact" },
                            style = LineaTypography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = LineaColors.TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item.missingAttributes.forEach { missing ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(LineaColors.Warning.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = missing,
                                        style = LineaTypography.labelSmall,
                                        fontSize = 10.sp,
                                        color = LineaColors.Warning,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.suggestions.joinToString(" • "),
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }

                    OutlinedButton(
                        onClick = { onDismissIncomplete(item) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LineaColors.TextSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LineaColors.GlassBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateNumbersTabContent(groups: List<DuplicateNumberGroup>) {
    if (groups.isEmpty()) {
        EmptyPlaceholder(
            title = "No Duplicate Numbers",
            message = "No phone numbers are shared across multiple contact records."
        )
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        items(groups) { group ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .neumorphic(
                        shape = RoundedCornerShape(16.dp),
                        elevation = 2.dp,
                        surfaceColor = LineaColors.NeuSurfaceRaised
                    )
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Phone Number: ${group.displayFormattedNumber}",
                        style = LineaTypography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LineaColors.TitaniumBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stored across ${group.contacts.size} contact records:",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    group.contacts.forEach { contactProfile ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = LineaColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = contactProfile.contact.displayName,
                                style = LineaTypography.bodyMedium,
                                color = LineaColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MergePreviewSheet(
    preview: MergePreview,
    onDismiss: () -> Unit,
    onConfirmMerge: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Merge Contacts Preview",
                    style = LineaTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = LineaColors.TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = preview.explanation,
                style = LineaTypography.bodySmall,
                color = LineaColors.TitaniumBlue
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Retained Fields Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LineaColors.NeuSurfaceRaised)
                    .border(LineaDimensions.HairlineBorder, LineaColors.NeuBorderHighlight.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "Resulting Merged Contact:",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preview.proposedDisplayName,
                        style = LineaTypography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LineaColors.TextPrimary
                    )

                    if (!preview.proposedCompany.isNullOrBlank()) {
                        Text(
                            text = "Company: ${preview.proposedCompany}",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Preserved Numbers (${preview.mergedNumbers.size}):",
                        style = LineaTypography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )
                    preview.mergedNumbers.forEach { (num, label) ->
                        Text(
                            text = "• $num ($label)",
                            style = LineaTypography.bodySmall,
                            color = LineaColors.TextSecondary
                        )
                    }

                    if (preview.mergedEmails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Preserved Emails (${preview.mergedEmails.size}):",
                            style = LineaTypography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = LineaColors.TextPrimary
                        )
                        preview.mergedEmails.forEach { email ->
                            Text(
                                text = "• $email",
                                style = LineaTypography.bodySmall,
                                color = LineaColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", color = LineaColors.TextPrimary)
                }

                Button(
                    onClick = onConfirmMerge,
                    colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Merge")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartFilterRulesSheet(
    config: SmartFilterConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (SmartFilterConfig) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sensitivity = config.sensitivity
    var autoCapitalize = config.autoCapitalizeNames
    var normalizeIntl = config.normalizeInternationalNumbers

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LiNEA SF Rules & Sensitivity",
                    style = LineaTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LineaColors.TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = LineaColors.TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Duplicate Matching Sensitivity",
                style = LineaTypography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensitivityChip(
                    label = "Strict (90%+)",
                    isSelected = sensitivity == MatchSensitivity.STRICT,
                    onClick = { sensitivity = MatchSensitivity.STRICT },
                    modifier = Modifier.weight(1f)
                )
                SensitivityChip(
                    label = "Balanced (75%+)",
                    isSelected = sensitivity == MatchSensitivity.BALANCED,
                    onClick = { sensitivity = MatchSensitivity.BALANCED },
                    modifier = Modifier.weight(1f)
                )
                SensitivityChip(
                    label = "Aggressive (60%+)",
                    isSelected = sensitivity == MatchSensitivity.AGGRESSIVE,
                    onClick = { sensitivity = MatchSensitivity.AGGRESSIVE },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = LineaColors.GlassBorder.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(18.dp))

            // Switches
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Capitalize Names",
                        style = LineaTypography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Convert all-caps or all-lowercase names to proper Title Case.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
                Switch(
                    checked = autoCapitalize,
                    onCheckedChange = { autoCapitalize = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = LineaColors.TitaniumBlue)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "International Number Normalization",
                        style = LineaTypography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LineaColors.TextPrimary
                    )
                    Text(
                        text = "Recognize matching numbers across country code and local dial formats.",
                        style = LineaTypography.bodySmall,
                        color = LineaColors.TextSecondary
                    )
                }
                Switch(
                    checked = normalizeIntl,
                    onCheckedChange = { normalizeIntl = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = LineaColors.TitaniumBlue)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSaveConfig(
                        config.copy(
                            sensitivity = sensitivity,
                            autoCapitalizeNames = autoCapitalize,
                            normalizeInternationalNumbers = normalizeIntl
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = LineaColors.TitaniumBlue),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply & Re-Scan")
            }
        }
    }
}

@Composable
private fun SensitivityChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) LineaColors.TitaniumBlue else LineaColors.NeuSurfaceRaised
    val textColor = if (isSelected) Color.White else LineaColors.TextPrimary

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
                LineaDimensions.HairlineBorder,
                if (isSelected) LineaColors.TitaniumBlue else LineaColors.NeuBorderHighlight.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = LineaTypography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyPlaceholder(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(LineaColors.MutedSageGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = LineaColors.MutedSageGreen,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = LineaTypography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LineaColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = LineaTypography.bodySmall,
                color = LineaColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

package com.ryanshelby.linea.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.data.local.entities.ContactEntity
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactCreateEditSheet(
    contactToEdit: ContactEntity? = null,
    initialNumbers: List<Pair<String, String>> = listOf("" to "Mobile"),
    onDismiss: () -> Unit,
    onSave: (
        displayName: String,
        company: String?,
        numbers: List<Pair<String, String>>,
        emails: List<String>,
        preferredSimSlot: Int?,
        notes: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var displayName by remember { mutableStateOf(contactToEdit?.displayName ?: "") }
    var company by remember { mutableStateOf(contactToEdit?.company ?: "") }
    var notes by remember { mutableStateOf(contactToEdit?.notes ?: "") }
    var preferredSimSlot by remember { mutableStateOf(contactToEdit?.preferredSimSlot) }
    var emailInput by remember { mutableStateOf("") }

    val numbersList = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            if (initialNumbers.isNotEmpty()) {
                addAll(initialNumbers)
            } else {
                add("" to "Mobile")
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = LineaColors.BackgroundTop,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(scrollState)
        ) {
            // Header: Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = LineaColors.TextSecondary
                    )
                }

                Text(
                    text = if (contactToEdit == null) "New Contact" else "Edit Contact",
                    style = LineaTypography.titleMedium,
                    color = LineaColors.TextPrimary
                )

                Button(
                    onClick = {
                        val validNumbers = numbersList.filter { it.first.isNotBlank() }
                        if (displayName.isNotBlank() && validNumbers.isNotEmpty()) {
                            onSave(
                                displayName.trim(),
                                company.ifBlank { null },
                                validNumbers,
                                if (emailInput.isNotBlank()) listOf(emailInput.trim()) else emptyList(),
                                preferredSimSlot,
                                notes.ifBlank { null }
                            )
                        }
                    },
                    enabled = displayName.isNotBlank() && numbersList.any { it.first.isNotBlank() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LineaColors.TitaniumBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Save", style = LineaTypography.labelMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Name
            Text(
                text = "Full Name",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                placeholder = { Text("Name", color = LineaColors.TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Company
            Text(
                text = "Company & Title",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                placeholder = { Text("Company / Job Title", color = LineaColors.TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Numbers Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phone Numbers",
                    style = LineaTypography.labelSmall,
                    color = LineaColors.TextSecondary
                )
                Row(
                    modifier = Modifier.clickable { numbersList.add("" to "Mobile") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Number",
                        tint = LineaColors.TitaniumBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Number",
                        style = LineaTypography.labelSmall,
                        color = LineaColors.TitaniumBlue
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            numbersList.forEachIndexed { index, (num, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = num,
                        onValueChange = { newNum ->
                            numbersList[index] = newNum to label
                        },
                        placeholder = { Text("Phone Number", color = LineaColors.TextTertiary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = outlinedColors()
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (numbersList.size > 1) {
                        IconButton(
                            onClick = { numbersList.removeAt(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = LineaColors.Danger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SIM Affinity Selector
            Text(
                text = "Preferred SIM Affinity",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    null to "Auto",
                    0 to "Force SIM 1",
                    1 to "Force SIM 2"
                ).forEach { (simSlot, label) ->
                    val isSelected = preferredSimSlot == simSlot
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassFill)
                            .border(
                                LineaDimensions.HairlineBorder,
                                if (isSelected) LineaColors.TitaniumBlue else LineaColors.GlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { preferredSimSlot = simSlot }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SimCard,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else LineaColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = LineaTypography.labelSmall,
                                color = if (isSelected) Color.White else LineaColors.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Email
            Text(
                text = "Email",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                placeholder = { Text("Email address", color = LineaColors.TextTertiary) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notes
            Text(
                text = "Notes",
                style = LineaTypography.labelSmall,
                color = LineaColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Contact notes...", color = LineaColors.TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LineaColors.GlassFill,
    unfocusedContainerColor = LineaColors.GlassFill,
    focusedBorderColor = LineaColors.TitaniumBlue,
    unfocusedBorderColor = LineaColors.GlassBorder,
    focusedTextColor = LineaColors.TextPrimary,
    unfocusedTextColor = LineaColors.TextPrimary
)

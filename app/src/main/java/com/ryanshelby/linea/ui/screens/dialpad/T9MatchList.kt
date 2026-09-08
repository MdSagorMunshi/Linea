package com.ryanshelby.linea.ui.screens.dialpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.telecom.T9SearchResult
import com.ryanshelby.linea.ui.components.ContactAvatar
import com.ryanshelby.linea.ui.components.FrostedGlassBox
import com.ryanshelby.linea.ui.theme.LineaColors
import com.ryanshelby.linea.ui.theme.LineaDimensions
import com.ryanshelby.linea.ui.theme.LineaTypography

@Composable
fun T9MatchList(
    matches: List<T9SearchResult>,
    onSelectContact: (String) -> Unit,
    onDirectCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = matches.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        FrostedGlassBox(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 210.dp),
            shape = RoundedCornerShape(18.dp),
            borderColor = LineaColors.GlassBorder
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(matches, key = { "${it.contact.id}_${it.contact.phoneNumber}" }) { match ->
                    T9MatchItem(
                        match = match,
                        onClick = { onSelectContact(match.contact.phoneNumber) },
                        onCall = { onDirectCall(match.contact.phoneNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun T9MatchItem(
    match: T9SearchResult,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Avatar / Photo
            ContactAvatar(
                photoUri = match.contact.photoUri,
                displayName = match.contact.displayName,
                size = 38.dp,
                initialsTextSize = 14.sp,
                borderColor = LineaColors.GlassBorder
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Formatted display name with highlighted matches
                val annotatedName = buildAnnotatedString {
                    val name = match.contact.displayName
                    if (match.matchedName && match.matchStartIndex in name.indices) {
                        val start = match.matchStartIndex
                        val end = match.matchEndIndex.coerceAtMost(name.length)
                        append(name.substring(0, start))
                        withStyle(
                            style = SpanStyle(
                                color = LineaColors.TitaniumBlue,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(name.substring(start, end))
                        }
                        if (end < name.length) {
                            append(name.substring(end))
                        }
                    } else {
                        append(name)
                    }
                }

                Text(
                    text = annotatedName,
                    style = LineaTypography.bodyLarge,
                    color = LineaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Phone number
                val annotatedNumber = buildAnnotatedString {
                    val num = match.contact.phoneNumber
                    if (!match.matchedName && match.matchStartIndex in num.indices) {
                        val start = match.matchStartIndex
                        val end = match.matchEndIndex.coerceAtMost(num.length)
                        append(num.substring(0, start))
                        withStyle(
                            style = SpanStyle(
                                color = LineaColors.TitaniumBlue,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(num.substring(start, end))
                        }
                        if (end < num.length) {
                            append(num.substring(end))
                        }
                    } else {
                        append(num)
                    }
                }

                Text(
                    text = annotatedNumber,
                    style = LineaTypography.bodySmall.copy(
                        fontFeatureSettings = "tnum"
                    ),
                    color = LineaColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        // Quick Call Button
        IconButton(
            onClick = onCall,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Call,
                contentDescription = "Call",
                tint = LineaColors.TitaniumBlue,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

package com.example.egimhesabi.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.R
import com.example.egimhesabi.domain.SlopeStatus
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.SlopeDanger
import com.example.egimhesabi.theme.SlopeOk
import com.example.egimhesabi.theme.SlopeWarning
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.util.NumberParser
import com.example.egimhesabi.viewmodel.SlopeUiState

@Composable
fun ResultCard(
    state: SlopeUiState,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calculation = state.calculation

    val statusColor by animateColorAsState(
        targetValue = when (calculation.slopeStatus) {
            SlopeStatus.OK -> SlopeOk
            SlopeStatus.NEAR_LIMIT -> SlopeWarning
            SlopeStatus.TOO_LOW, SlopeStatus.TOO_HIGH -> SlopeDanger
            SlopeStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 400),
        label = "statusColor"
    )

    val statusText = when (calculation.slopeStatus) {
        SlopeStatus.OK -> stringResource(R.string.status_ok)
        SlopeStatus.NEAR_LIMIT -> stringResource(R.string.status_near_limit)
        SlopeStatus.TOO_LOW -> stringResource(R.string.status_too_low)
        SlopeStatus.TOO_HIGH -> stringResource(R.string.status_too_high)
        SlopeStatus.UNKNOWN -> stringResource(R.string.status_unknown)
    }

    if (calculation.slopePercent != null) {
        GlassCardAccent(
            modifier = modifier.fillMaxWidth(),
            accentColor = statusColor,
            cornerRadius = 24.dp,
            contentPadding = 20.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big slope values
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "% ${NumberParser.formatDecimal(calculation.slopePercent)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = statusColor
                        )
                        Text(
                            text = stringResource(R.string.label_percentage),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    if (calculation.slopeRatio != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "1/${NumberParser.formatCompact(calculation.slopeRatio)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = statusColor
                            )
                            Text(
                                text = stringResource(R.string.label_ratio),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Height diff
                if (calculation.heightDiff != null) {
                    Text(
                        text = "Δh = ${NumberParser.formatDecimal(calculation.heightDiff)} m",
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AccentOrange
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Status badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Limits info
                Text(
                    text = "Min: 1/${NumberParser.formatCompact(state.minSlopeRatio)}  Max: 1/${NumberParser.formatCompact(state.maxSlopeRatio)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.7f)
                )

                if (!state.historySaved) {
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(
                        onClick = onSaveClick,
                        enabled = !state.historySaving,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = AccentOrange
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(0.6f).height(44.dp)
                    ) {
                        Text(
                            text = if (state.historySaving) "Kaydediliyor…" else "Geçmişe Kaydet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    state.historySaveError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(44.dp)
                            .background(
                                color = SlopeOk.copy(alpha = 0.1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            )
                    ) {
                        Text(
                            text = "Kaydedildi",
                            color = SlopeOk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    } else {
        GlassCard(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 24.dp,
            backgroundAlpha = 1f,
            contentPadding = 24.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(statusColor, androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    fontSize = 18.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

fun getStatusColor(status: SlopeStatus): Color {
    return when (status) {
        SlopeStatus.OK -> SlopeOk
        SlopeStatus.NEAR_LIMIT -> SlopeWarning
        SlopeStatus.TOO_LOW, SlopeStatus.TOO_HIGH -> SlopeDanger
        SlopeStatus.UNKNOWN -> Color.Gray
    }
}

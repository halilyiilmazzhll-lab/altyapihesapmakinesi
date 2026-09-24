package com.example.egimhesabi.ui.screens

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.R
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientEnd
import com.example.egimhesabi.theme.GradientMid
import com.example.egimhesabi.theme.GradientStart
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.ui.components.ManholeCard
import com.example.egimhesabi.ui.components.ResultCard
import com.example.egimhesabi.ui.components.SlopeDiagram
import com.example.egimhesabi.ui.components.StakeoutPicker
import com.example.egimhesabi.ui.components.getStatusColor
import com.example.egimhesabi.viewmodel.SlopeViewModel

import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlopeCalculatorScreen(
    viewModel: SlopeViewModel,
    onNavigateToHistory: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val calculation = state.calculation
    val focusManager = LocalFocusManager.current

    val glassFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentOrange,
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedContainerColor = Color.White.copy(alpha = 0.85f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.65f),
        focusedLabelColor = AccentOrange,
        unfocusedLabelColor = TextSecondary,
        cursorColor = AccentOrange,
        errorBorderColor = MaterialTheme.colorScheme.error,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            TopAppBar(
                title = {
                    Text(
                        text = "Eğim Hesabı",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "Geçmiş",
                            tint = AccentOrange
                        )
                    }
                    com.example.egimhesabi.ui.components.ConfirmClearAction(onConfirm = viewModel::clearAll)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .verticalScroll(rememberScrollState()).imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Two manhole cards side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManholeCard(
                        bacaNumber = 1,
                        bacaName = state.baca1Name,
                        kapakKotu = state.baca1KapakKotu,
                        akarKotu = state.baca1AkarKotu,
                        derinlik = calculation.b1Derinlik,
                        isNegativeDepth = calculation.b1NegativeDepth,
                        onNameChange = { viewModel.updateBaca1Name(it) },
                        onKapakKotuChange = { viewModel.updateBaca1KapakKotu(it) },
                        onAkarKotuChange = { viewModel.updateBaca1AkarKotu(it) },
                        modifier = Modifier.weight(1f),
                        headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Baca 1", state.baca1Source, viewModel::importBaca1) }
                    )
                    ManholeCard(
                        bacaNumber = 2,
                        bacaName = state.baca2Name,
                        kapakKotu = state.baca2KapakKotu,
                        akarKotu = state.baca2AkarKotu,
                        derinlik = calculation.b2Derinlik,
                        isNegativeDepth = calculation.b2NegativeDepth,
                        onNameChange = { viewModel.updateBaca2Name(it) },
                        onKapakKotuChange = { viewModel.updateBaca2KapakKotu(it) },
                        onAkarKotuChange = { viewModel.updateBaca2AkarKotu(it) },
                        modifier = Modifier.weight(1f),
                        headerAction = { com.example.egimhesabi.ui.components.StakeoutPicker("Baca 2", state.baca2Source, viewModel::importBaca2) }
                    )
                }

                // Distance input
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 24.dp,
                    backgroundAlpha = 1f,
                    contentPadding = 16.dp,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "MESAFE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    color = if (calculation.invalidDistance) MaterialTheme.colorScheme.error.copy(alpha=0.05f) else Color(0xFFF2F2F7),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 16.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = state.mesafe,
                                onValueChange = { viewModel.updateMesafe(it) },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = if (calculation.invalidDistance) MaterialTheme.colorScheme.error else TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                singleLine = true,
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentOrange),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (state.mesafe.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                color = TextSecondary.copy(alpha = 0.4f),
                                                fontSize = 24.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            Text(
                                text = stringResource(R.string.unit_meters),
                                color = TextSecondary.copy(alpha = 0.6f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        if (calculation.invalidDistance) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.distance_invalid_error),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Result card
                ResultCard(
                    state = state,
                    onSaveClick = { viewModel.saveCurrentCalculation() }
                )

                // Diagram
                if (calculation.b1Akar != null && calculation.b2Akar != null) {
                    SlopeDiagram(
                        state = state,
                        statusColor = getStatusColor(calculation.slopeStatus)
                    )
                }

                Spacer(modifier = Modifier.height(84.dp)) // Space for bottom navigation
            }
        }
    }
}
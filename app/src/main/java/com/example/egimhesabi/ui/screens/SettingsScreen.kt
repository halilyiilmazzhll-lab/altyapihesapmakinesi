package com.example.egimhesabi.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import com.example.egimhesabi.theme.SurfaceCard

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.egimhesabi.BuildConfig
import com.example.egimhesabi.R
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.GradientEnd
import com.example.egimhesabi.theme.GradientMid
import com.example.egimhesabi.theme.GradientStart
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.ui.components.GlassCard
import com.example.egimhesabi.viewmodel.SettingsViewModel

import androidx.compose.material.icons.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val glassFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AccentOrange,
        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_settings),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Menü",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Eğim Limitleteri Kartı (Glass)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundAlpha = 0.4f,
                    contentPadding = 16.dp,
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_limits_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold
                        )

                        // Min Slope (1/X)
                        OutlinedTextField(
                            value = state.minSlopeRatioInput,
                            onValueChange = { viewModel.onMinSlopeRatioChanged(it) },
                            label = { Text(stringResource(R.string.settings_min_label)) },
                            prefix = { Text(stringResource(R.string.ratio_prefix), color = TextSecondary) },
                            isError = state.minError != null,
                            supportingText = {
                                state.minError?.let {
                                    Text(text = it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            ),
                            colors = glassFieldColors
                        )
                        
                        // Max Slope (1/X)
                        OutlinedTextField(
                            value = state.maxSlopeRatioInput,
                            onValueChange = { viewModel.onMaxSlopeRatioChanged(it) },
                            label = { Text(stringResource(R.string.settings_max_label)) },
                            prefix = { Text(stringResource(R.string.ratio_prefix), color = TextSecondary) },
                            isError = state.maxError != null,
                            supportingText = {
                                state.maxError?.let {
                                    Text(text = it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            ),
                            colors = glassFieldColors
                        )

                        // Min Manhole Depth
                        OutlinedTextField(
                            value = state.minManholeDepthMetersInput,
                            onValueChange = { viewModel.onMinManholeDepthChanged(it) },
                            label = { Text("Minimum Baca Derinliği") },
                            suffix = { Text("m", color = TextSecondary) },
                            isError = state.minManholeDepthError != null,
                            supportingText = {
                                state.minManholeDepthError?.let {
                                    Text(text = it, color = MaterialTheme.colorScheme.error)
                                } ?: Text(text = "Önerilen kot aralığını hesaplarken referans alınır.", color = TextSecondary)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            ),
                            colors = glassFieldColors
                        )

                        Text(
                            text = stringResource(R.string.settings_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.resetToDefaults() },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AccentOrange
                                )
                            ) {
                                Text(stringResource(R.string.settings_reset))
                            }
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundAlpha = 0.4f,
                    contentPadding = 16.dp,
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "İmalat Takibi",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Proje Siyah Kot Farkı Uyarı Eşiği",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = state.differenceThresholdCmInput,
                            onValueChange = viewModel::onDifferenceThresholdChanged,
                            label = { Text("Uyarı eşiği") },
                            suffix = { Text("cm", color = TextSecondary) },
                            isError = state.differenceThresholdError != null,
                            supportingText = {
                                Text(
                                    text = state.differenceThresholdError
                                        ?: "Mutlak fark bu değeri aşınca bacada uyarı simgesi gösterilir.",
                                    color = if (state.differenceThresholdError != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        TextSecondary
                                    }
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            ),
                            colors = glassFieldColors
                        )
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundAlpha = 0.4f,
                    contentPadding = 16.dp,
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Emir Defteri",
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Aktif Hakediş Numarası",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sahada girilen emir defteri notlarının otomatik etiketleneceği hakediş numarası.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = state.activeProgressPaymentInput,
                            onValueChange = { viewModel.onActiveProgressPaymentChanged(it) },
                            label = { Text("Hakediş numarası") },
                            isError = state.activeProgressPaymentError != null,
                            supportingText = {
                                state.activeProgressPaymentError?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = glassFieldColors
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Şablon Başlıklar",
                            style = MaterialTheme.typography.titleSmall,
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Baca kayıtlarında seçilecek standart başlıkları tanımlayın.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.workOrderTempleateInput,
                                onValueChange = viewModel::onWorkOrderTempleateInputChanged,
                                label = { Text("Yeni başlık") },
                                placeholder = { Text("Örn. Hat Doğrusallığı") },
                                isError = state.workOrderTempleateError != null,
                                supportingText = {
                                    state.workOrderTempleateError?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier.weight(1f),
                                colors = glassFieldColors
                            )
                            OutlinedButton(
                                onClick = viewModel::addWorkOrderTempleate,
                                modifier = Modifier.height(56.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                                Text("Ekle")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        if (state.workOrderTempleates.isEmpty()) {
                            Text(
                                "Henüz emir defteri başlığı tanımlanmadı.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            state.workOrderTempleates.forEach { title ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color.White.copy(alpha = 0.22f),
                                            androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                        )
                                        .padding(start = 12.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        title,
                                        modifier = Modifier.weight(1f),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(onClick = { viewModel.removeWorkOrderTempleate(title) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "$title başlığını sil",
                                            tint = Color(0xFFD94A4A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Hakkında Kartı (Glass)
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                    backgroundAlpha = 0.4f,
                    contentPadding = 16.dp,
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = AccentOrange,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.about_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}



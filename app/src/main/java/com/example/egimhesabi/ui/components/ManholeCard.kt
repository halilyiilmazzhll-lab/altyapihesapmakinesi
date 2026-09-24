package com.example.egimhesabi.ui.components

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.egimhesabi.R
import com.example.egimhesabi.theme.AccentOrange
import com.example.egimhesabi.theme.TextPrimary
import com.example.egimhesabi.theme.TextSecondary
import com.example.egimhesabi.util.NumberParser

@Composable
fun ManholeCard(
    bacaNumber: Int,
    bacaName: String,
    kapakKotu: String,
    akarKotu: String,
    derinlik: Double?,
    isNegativeDepth: Boolean,
    onNameChange: (String) -> Unit,
    onKapakKotuChange: (String) -> Unit,
    onAkarKotuChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    headerAction: @Composable (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current

    GlassCard(
        modifier = modifier,
        cornerRadius = 24.dp,
        backgroundAlpha = 0.95f,
        contentPadding = 16.dp,
        elevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Başlık
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (bacaNumber == 1) "1. BACA" else "2. BACA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                if (headerAction != null) {
                    headerAction()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // İsim Girişi
            ModernInput(
                value = bacaName,
                onValueChange = onNameChange,
                label = "İsim",
                placeholder = if (bacaNumber == 1) "B-101" else "B-102",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isNumber = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Kapak Kotu
            ModernInput(
                value = kapakKotu,
                onValueChange = onKapakKotuChange,
                label = "Kapak",
                placeholder = "0.00",
                suffix = "m",
                isError = isNegativeDepth,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isNumber = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Akar Kotu
            ModernInput(
                value = akarKotu,
                onValueChange = onAkarKotuChange,
                label = "Akar",
                placeholder = "0.00",
                suffix = "m",
                isError = isNegativeDepth,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                isNumber = true
            )

            // Derinlik Göstergesi
            Spacer(modifier = Modifier.height(8.dp))
            if (isNegativeDepth) {
                Text(
                    text = stringResource(R.string.depth_negative_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            } else if (derinlik != null) {
                Text(
                    text = "▼ h = ${NumberParser.formatDecimal(derinlik)} m",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ModernInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    suffix: String? = null,
    isError: Boolean = false,
    isNumber: Boolean = false,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions
) {
    val bgColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.05f) else Color(0xFFF1F5F9)
    val textColor = if (isError) MaterialTheme.colorScheme.error else TextPrimary
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (isNumber) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = if (isNumber) FontFamily.Monospace else FontFamily.Default
                    ),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = true,
                    cursorBrush = SolidColor(AccentOrange),
                    modifier = Modifier.weight(1f).semantics { contentDescription = label },
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontFamily = if (isNumber) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                        innerTextField()
                    }
                )
                
                if (suffix != null) {
                    Text(
                        text = suffix,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

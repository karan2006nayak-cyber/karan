package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VaultAccentCrimson
import com.example.ui.theme.VaultAccentGreen
import com.example.ui.theme.VaultAccentIndigo
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultDarkSurface
import com.example.ui.theme.VaultDarkSurfaceVariant

@Composable
fun LockScreen(
    isFirstTimeSetup: Boolean,
    onPinEntered: (String) -> Unit,
    onBiometricClicked: () -> Unit,
    biometricsEnabled: Boolean,
    errorMessage: String?,
    onClearError: () -> Unit
) {
    var pinText by remember { mutableStateOf("") }
    var confirmPinMode by remember { mutableStateOf(false) }
    var firstPinAttempt by remember { mutableStateOf("") }
    var instructionText by remember {
        mutableStateOf(
            if (isFirstTimeSetup) "Set up your secure 4-digit master PIN" else "Enter PIN to access your vault"
        )
    }

    val context = LocalContext.current

    val onDigitClick: (String) -> Unit = { digit ->
        if (pinText.length < 4) {
            pinText += digit
            onClearError()
            
            if (pinText.length == 4) {
                if (isFirstTimeSetup) {
                    if (!confirmPinMode) {
                        firstPinAttempt = pinText
                        confirmPinMode = true
                        instructionText = "Confirm your 4-digit PIN"
                        pinText = ""
                    } else {
                        if (pinText == firstPinAttempt) {
                            onPinEntered(pinText)
                        } else {
                            instructionText = "PINs do not match. Start over."
                            confirmPinMode = false
                            firstPinAttempt = ""
                            pinText = ""
                        }
                    }
                } else {
                    onPinEntered(pinText)
                    pinText = ""
                }
            }
        }
    }

    val onDeleteClick: () -> Unit = {
        if (pinText.isNotEmpty()) {
            pinText = pinText.substring(0, pinText.length - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VaultDarkBg, Color(0xFF070B14))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Glowing Shield
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(VaultDarkSurfaceVariant.copy(alpha = 0.6f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Logo",
                    tint = if (errorMessage != null) VaultAccentCrimson else VaultAccentIndigo,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SECURE VAULT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = VaultAccentIndigo,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = instructionText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..4) {
                    val active = pinText.length >= i
                    val color = if (errorMessage != null) VaultAccentCrimson
                               else if (active) VaultAccentIndigo
                               else Color.White.copy(alpha = 0.2f)
                    
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1.2f else 1.0f,
                        animationSpec = spring()
                    )

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .scale(scale)
                            .background(color, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message Box
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = VaultAccentCrimson.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Shield Error",
                                tint = VaultAccentCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                fontSize = 13.sp,
                                color = VaultAccentCrimson,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Keypad Row Builder
            Column(
                modifier = Modifier.width(280.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val numGrid = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )

                for (row in numGrid) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (num in row) {
                            KeypadButton(text = num, onClick = { onDigitClick(num) })
                        }
                    }
                }

                // Bottom row with biometrics/delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Biometrics button or empty spacer
                    if (biometricsEnabled && !isFirstTimeSetup) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(VaultDarkSurfaceVariant.copy(alpha = 0.4f))
                                .clickable { onBiometricClicked() }
                                .testTag("biometric_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Biometric Lock",
                                tint = VaultAccentGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(72.dp))
                    }

                    KeypadButton(text = "0", onClick = { onDigitClick("0") })

                    // Backspace button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VaultDarkSurfaceVariant.copy(alpha = 0.4f))
                            .clickable { onDeleteClick() }
                            .testTag("backspace_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(VaultDarkSurface)
            .clickable(onClick = onClick)
            .testTag("keypad_btn_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

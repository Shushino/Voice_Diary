package com.shushino.voicediary.presentation.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.shushino.voicediary.presentation.viewmodel.LockViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

@Composable
fun LockScreen(
    onUnlockSuccess: () -> Unit,
    viewModel: LockViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var shakeOffset by remember { mutableStateOf(Offset.Zero) }
    val animatedOffset by animateOffsetAsState(
        targetValue = shakeOffset,
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = ""
    )

    LaunchedEffect(state.isUnlocked) {
        if (state.isUnlocked) {
            onUnlockSuccess()
        }
    }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            shakeOffset = Offset(10f, 0f)
            delay(50)
            shakeOffset = Offset(-10f, 0f)
            delay(50)
            shakeOffset = Offset(10f, 0f)
            delay(50)
            shakeOffset = Offset.Zero
            viewModel.resetPinInput() // Clear input and error after shake
        }
    }

    val biometricPrompt = remember(context) {
        BiometricPrompt(
            context as FragmentActivity,
            Executors.newSingleThreadExecutor(),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.onBiometricSuccess()
                }
            }
        )
    }

    val promptInfo = remember { 
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Diary")
            .setSubtitle("Use fingerprint")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        // Top section: Lock icon and title
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Lock",
                tint = Color.White,
                modifier = Modifier.size(96.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Diary",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // PIN input dots
        Row(
            modifier = Modifier.offset {
                IntOffset(animatedOffset.x.roundToInt(), animatedOffset.y.roundToInt())
            },
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(4) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCircle(
                        color = if (it < state.pinInput.length) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }

        // Error message
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = Color.Red,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (state.isVerifying) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp).padding(top = 8.dp)
            )
        }

        // NumPad
        NumPad(
            enabled = !state.isVerifying,
            onDigitEntered = { viewModel.onDigitEntered(it) },
            onDelete = { viewModel.onDelete() }
        )

        // Biometric button
        if (state.isBiometricAvailable) {
            IconButton(
                onClick = { biometricPrompt.authenticate(promptInfo) },
                enabled = !state.isVerifying
            ) {
                Icon(
                    Icons.Default.Fingerprint,
                    contentDescription = "Unlock with fingerprint",
                    tint = if (state.isVerifying) Color.White.copy(alpha = 0.4f) else Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun NumPad(
    enabled: Boolean,
    onDigitEntered: (Char) -> Unit,
    onDelete: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
        // Digits 1-9
        (1..9).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { digit ->
                    NumPadButton(
                        digit = digit.toString(),
                        enabled = enabled
                    ) { onDigitEntered(it.single()) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Digit 0 and delete button
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Spacer(modifier = Modifier.size(72.dp)) // Empty space for alignment
            NumPadButton(digit = "0", enabled = enabled) { onDigitEntered(it.single()) }
            NumPadButton(icon = Icons.AutoMirrored.Filled.Backspace, enabled = enabled) { onDelete() }
        }
    }
}

@Composable
fun NumPadButton(
    digit: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    onClick: (String) -> Unit
) {
    Button(
        onClick = { onClick(digit ?: "") },
        enabled = enabled,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.2f),
            disabledContainerColor = Color.White.copy(alpha = 0.1f)
        )
    ) {
        if (digit != null) {
            Text(
                text = digit,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        } else if (icon != null) {
            Icon(icon, contentDescription = "", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }
}

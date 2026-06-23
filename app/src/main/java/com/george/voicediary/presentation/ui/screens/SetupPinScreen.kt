package com.george.voicediary.presentation.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.george.voicediary.presentation.viewmodel.SetupPinViewModel
import kotlinx.coroutines.delay

@Composable
fun SetupPinScreen(
    onSetupSuccess: () -> Unit,
    viewModel: SetupPinViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var shakeOffset by remember { mutableStateOf(Offset.Zero) }
    val animatedOffset by animateOffsetAsState(
        targetValue = shakeOffset,
        animationSpec = tween(durationMillis = 50, easing = LinearEasing),
        label = ""
    )

    LaunchedEffect(state.pinSetSuccess) {
        if (state.pinSetSuccess) {
            onSetupSuccess()
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
            Toast.makeText(context, state.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
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
                text = if (state.currentStep == SetupPinViewModel.SetupPinStep.CHOOSE_PIN) "Choose a 4-digit PIN" else "Confirm your PIN",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // PIN input dots
        Row(
            modifier = Modifier.offset(x = animatedOffset.x.dp, y = animatedOffset.y.dp),
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

        // NumPad
        NumPad(
            enabled = !state.pinSetSuccess,
            onDigitEntered = { viewModel.onDigitEntered(it) },
            onDelete = { viewModel.onDelete() }
        )
    }
}

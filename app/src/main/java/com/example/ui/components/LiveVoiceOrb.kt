package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiGlowCyan
import com.example.ui.theme.GeminiMagenta
import com.example.ui.theme.GeminiViolet
import com.example.ui.viewmodel.LiveVoiceState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveVoiceOrb(
    state: LiveVoiceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp
) {
    val transition = rememberInfiniteTransition(label = "OrbAnimation")

    val pulseScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = when (state) {
            LiveVoiceState.SPEAKING -> 1.18f
            LiveVoiceState.LISTENING -> 1.12f
            LiveVoiceState.THINKING -> 1.05f
            LiveVoiceState.IDLE -> 0.98f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    LiveVoiceState.SPEAKING -> 600
                    LiveVoiceState.LISTENING -> 800
                    LiveVoiceState.THINKING -> 400
                    LiveVoiceState.IDLE -> 2000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == LiveVoiceState.THINKING) 1500 else 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val waveAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveAlpha"
    )

    val orbColors = when (state) {
        LiveVoiceState.LISTENING -> listOf(GeminiGlowCyan, GeminiCyan, GeminiBlue)
        LiveVoiceState.THINKING -> listOf(GeminiViolet, GeminiMagenta, GeminiBlue)
        LiveVoiceState.SPEAKING -> listOf(GeminiCyan, GeminiViolet, GeminiMagenta)
        LiveVoiceState.IDLE -> listOf(GeminiBlue, GeminiViolet, GeminiCyan)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .testTag("live_voice_orb")
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(sizeDp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    rotationZ = rotationAngle
                }
        ) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.width / 2.6f

            // Outer pulse ring
            if (state == LiveVoiceState.LISTENING || state == LiveVoiceState.SPEAKING) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GeminiGlowCyan.copy(alpha = waveAlpha * 0.5f), Color.Transparent),
                        center = center,
                        radius = radius * 1.4f
                    ),
                    radius = radius * 1.35f
                )
                drawCircle(
                    color = GeminiCyan.copy(alpha = waveAlpha * 0.6f),
                    radius = radius * 1.25f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Outer glowing orb background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = orbColors.map { it.copy(alpha = 0.85f) },
                    center = center,
                    radius = radius
                ),
                radius = radius
            )

            // Dynamic inner core lights
            val coreCount = 6
            for (i in 0 until coreCount) {
                val angleRad = Math.toRadians((rotationAngle + i * (360f / coreCount)).toDouble())
                val offsetX = (center.x + (radius * 0.45f) * cos(angleRad)).toFloat()
                val offsetY = (center.y + (radius * 0.45f) * sin(angleRad)).toFloat()

                drawCircle(
                    color = orbColors[i % orbColors.size].copy(alpha = 0.7f),
                    radius = radius * 0.25f,
                    center = Offset(offsetX, offsetY)
                )
            }

            // High intensity center core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, orbColors.first(), Color.Transparent),
                    center = center,
                    radius = radius * 0.5f
                ),
                radius = radius * 0.45f
            )
        }
    }
}

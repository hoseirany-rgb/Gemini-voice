package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GeminiBlue
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GeminiGlowCyan
import com.example.ui.theme.GeminiMagenta
import com.example.ui.theme.GeminiViolet

@Composable
fun AudioWaveformVisualizer(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18,
    maxHeight: Dp = 36.dp
) {
    val transition = rememberInfiniteTransition(label = "WaveformAnimation")

    Row(
        modifier = modifier
            .height(maxHeight)
            .testTag("audio_waveform_visualizer"),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barGradients = listOf(
            Brush.verticalGradient(listOf(GeminiGlowCyan, GeminiCyan)),
            Brush.verticalGradient(listOf(GeminiCyan, GeminiBlue)),
            Brush.verticalGradient(listOf(GeminiBlue, GeminiViolet)),
            Brush.verticalGradient(listOf(GeminiViolet, GeminiMagenta))
        )

        for (i in 0 until barCount) {
            val duration = 400 + (i * 70) % 500
            val targetHeightFraction by transition.animateFloat(
                initialValue = 0.15f,
                targetValue = if (isAnimating) 0.2f + ((i * 37) % 80) / 100f else 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BarHeight_$i"
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(fraction = if (isAnimating) targetHeightFraction else 0.2f)
                    .background(
                        brush = barGradients[i % barGradients.size],
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

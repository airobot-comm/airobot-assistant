package com.airobot.airbot.components.interaction

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Waveform visual style for voice interaction states.
 */
internal enum class WaveformStyle {
    /** Idle state: 5 cyan bars with gentle floating, responsive to ambient audio */
    IDLE_BARS,

    /** Active conversation: 5 orange bars driven by real-time audio level */
    ACTIVE_BARS
}

/**
 * Voice waveform component — unified rendering for both idle and active states.
 * Matches the prototype VoiceBubbleKit.tsx waveform behavior.
 */
@Composable
internal fun VoiceWaveform(
    style: WaveformStyle,
    audioLevel: Float = 0.0f,
    scaleRatio: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    when (style) {
        WaveformStyle.IDLE_BARS -> IdleBarsWaveform(
            audioLevel = audioLevel, scaleRatio = scaleRatio, modifier = modifier
        )

        WaveformStyle.ACTIVE_BARS -> ActiveBarsWaveform(
            audioLevel = audioLevel, scaleRatio = scaleRatio, modifier = modifier
        )
    }
}

// ============================================================================
// Idle Bars — 5 cyan bars with slow gentle floating
// Prototype ref: VoiceBubbleKit.tsx idle waveform (bg-cyan-400/40, height 4-6px)
// ============================================================================

@Composable
private fun IdleBarsWaveform(
    audioLevel: Float,
    scaleRatio: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 5
    val infiniteTransition = rememberInfiniteTransition(label = "idleBars")
    val cyanColor = Color(0xFF22D3EE)  // cyan-400

    Row(
        modifier = modifier.height(28.dp * scaleRatio),
        horizontalArrangement = Arrangement.spacedBy(5.dp * scaleRatio),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            // Base gentle floating animation — staggered per bar
            val baseFloat by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 2500 + (index * 200),
                        easing = EaseInOutSine
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "idleBarFloat$index"
            )

            // Audio response: center bar reacts most, edges least
            val distanceToCenter = abs(index - barCount / 2)
            val sensitivity = (1.0f - distanceToCenter * 0.25f).coerceAtLeast(0.2f)
            val audioBoost = if (audioLevel > 0.05f) {
                audioLevel * sensitivity * 18f  // Boost up to ~18dp at max
            } else 0f

            val barHeight = (baseFloat + audioBoost).coerceIn(4f, 28f)

            // Alpha: slightly transparent at rest, more opaque when audio-active
            val alpha = if (audioLevel > 0.05f) {
                (0.5f + audioLevel * 0.4f).coerceAtMost(0.9f)
            } else 0.45f

            Box(
                modifier = Modifier
                    .width(6.dp * scaleRatio)
                    .height(barHeight.dp * scaleRatio)
                    .clip(RoundedCornerShape(3.dp))
                    .background(cyanColor.copy(alpha = alpha))
            )
        }
    }
}

// ============================================================================
// Active Bars — 5 orange bars driven by real-time audio level
// Prototype ref: VoiceInputPanel.tsx (bg-orange-500, responsive to listening)
// ============================================================================

@Composable
private fun ActiveBarsWaveform(
    audioLevel: Float,
    scaleRatio: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(32.dp * scaleRatio),
        horizontalArrangement = Arrangement.spacedBy(6.dp * scaleRatio),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val baseHeight = 8.dp
            val maxHeight = when (index) {
                2 -> 32.dp      // Center bar tallest
                1, 3 -> 24.dp   // Adjacent bars medium
                else -> 16.dp   // Edge bars shortest
            }

            val animatedHeight by animateFloatAsState(
                targetValue = baseHeight.value + (audioLevel * (maxHeight.value - baseHeight.value)),
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                label = "barHeight"
            )

            Box(
                modifier = Modifier
                    .width(8.dp * scaleRatio)
                    .height(animatedHeight.coerceAtLeast(baseHeight.value).dp * scaleRatio)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF97316)) // orange-500
            )
        }
    }
}

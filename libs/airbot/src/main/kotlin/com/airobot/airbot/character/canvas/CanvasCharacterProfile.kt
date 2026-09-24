package com.airobot.airbot.character.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.airobot.airbot.viewmodel.RobotVisualState

/**
 * Defines a swappable character profile for Airobot.
 * Architecture supports multiple robot character profiles with distinct visual appearances and personality configurations.
 */
interface CanvasCharacterProfile {
    /**
     * Renders the complete character including its specific state-driven animations,
     * floating physics, and body parts.
     *
     * @param state The current visual state of the robot.
     * @param ttsProgressNormalized The normalized progress of TTS playback (0f to 1f).
     * @param audioLevel Provider for the current audio level (0f to 1f).
     * @param size The base size of the character.
     * @param showAura Whether to render the environmental aura glow.
     * @param modifier The modifier to be applied to the character's root layout.
     */
    @Composable
    fun Render(
        state: RobotVisualState,
        ttsProgressNormalized: Float,
        audioLevel: () -> Float,
        size: Dp,
        showAura: Boolean,
        modifier: Modifier
    )
}

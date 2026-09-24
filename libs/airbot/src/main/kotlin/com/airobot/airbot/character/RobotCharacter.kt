package com.airobot.airbot.character

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airobot.airbot.character.canvas.aether.AetherProfileCanvas
import com.airobot.airbot.character.rive.RiveCharacter
import com.airobot.airbot.domain.model.CharacterType
import com.airobot.airbot.domain.model.RiveCharacterConfig
import com.airobot.airbot.viewmodel.RobotVisualState

/**
 * Robot character — entry point for character rendering.
 * Supports multi-character multi-engine architecture (Canvas + Rive).
 */
@Composable
fun RobotCharacter(
    state: RobotVisualState,
    characterType: CharacterType = CharacterType.ANDROID_CANVAS,
    roleName: String? = null,
    ttsProgressNormalized: Float = 0f,
    audioLevel: () -> Float = { 0f },
    headSize: Dp = 245.dp,
    showAura: Boolean = true,
    onRobotClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center
        ) {
            when (characterType) {
                CharacterType.ANDROID_CANVAS -> {
                    // Default native Canvas character (Aether)
                    AetherProfileCanvas.Render(
                        state = state,
                        ttsProgressNormalized = ttsProgressNormalized,
                        audioLevel = audioLevel,
                        size = headSize,
                        showAura = showAura,
                        modifier = Modifier
                    )
                }

                CharacterType.RIVE_IP -> {
                    val context = LocalContext.current
                    val config = RiveCharacterConfig.getCharacterConfig(context, roleName)
                    val density = LocalDensity.current
                    val translationX = with(density) { config.offsetX.dp.toPx() }
                    val translationY = with(density) { config.offsetY.dp.toPx() }

                    // Create a bounding box slightly larger than head size and clip to bounds
                    // The inner Rive animation will fillMaxSize to avoid native clipping, and scale down properly.
                    Box(
                        modifier = Modifier
                            .size(headSize * 1.2f)
                            .clipToBounds(),
                        contentAlignment = Alignment.Center
                    ) {
                        RiveCharacter(
                            state = state,
                            roleName = roleName,
                            audioLevel = if (state == RobotVisualState.SPEAKING) audioLevel else {
                                { 0f }
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = config.scale
                                    scaleY = config.scale
                                    this.translationX = translationX
                                    this.translationY = translationY
                                }
                        )
                    }
                }
            }
        }
    }
}

package com.airobot.framework.layout

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.airobot.framework.theme.RobotTheme

/**
 * Functional Module Plate — skeuomorphic frosted-glass container for functional cards.
 *
 * Prototype ref: FunctionalModulePlate.tsx
 *
 * Layout: Centered full-page card (~640dp max width, ~725dp height) with:
 *   - Large rounded corners (80dp radius)
 *   - Semi-transparent dark background with backdrop blur
 *   - Thin white/10% border
 *   - Decorative orange blurred orb at top-right
 *   - Entry animation (scale + fade + slide up)
 *   - Optional footer slot for sub-category controls
 */
@Composable
fun FunctionalModulePlate(
    modifier: Modifier = Modifier,
    isOverlayActive: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    // Entry animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "plateAlpha"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.9f,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "plateScale"
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 20f,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "plateOffsetY"
    )

    val isDark = RobotTheme.isDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
                translationY = animatedOffsetY * density
            }
    ) {
        // Main container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isDark) 40.dp else 20.dp,
                    shape = RoundedCornerShape(54.dp),
                    spotColor = if (isDark) Color.Black else Color.Black.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(54.dp))
                .background(
                    if (isDark) RobotTheme.colors.cardBg.copy(alpha = 0.85f)
                    else RobotTheme.colors.cardBg.copy(alpha = 0.95f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.10f)
                    else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(54.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Content area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .run {
                            if (isOverlayActive) blur(16.dp) else this
                        },
                    content = content
                )
            }

            // Overlay Z-layer (clipped by parent rounded shape)
            if (overlay != null) {
                overlay()
            }
        }

        // Optional footer
        if (footer != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 28.dp)
            ) {
                footer()
            }
        }
    }
}

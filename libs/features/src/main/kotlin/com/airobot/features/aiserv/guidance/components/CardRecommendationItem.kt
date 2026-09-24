package com.airobot.features.aiserv.guidance.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.features.aiserv.guidance.data.RecommendCard
import com.airobot.framework.theme.RobotTheme

/**
 * CardRecommendationItem — Renders an individual recommended service card.
 * Decoupled from App module and fully localized.
 */
@Composable
fun CardRecommendationItem(
    card: RecommendCard,
    onClick: () -> Unit,
    showProgress: Boolean = true,
    progressDuration: Int = 10000,
    modifier: Modifier = Modifier
) {
    var isHovered by remember { mutableStateOf(false) }

    val offsetX by animateFloatAsState(
        targetValue = if (isHovered) 8f else 0f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cardOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (RobotTheme.isDark) {
                        listOf(
                            RobotTheme.colors.cardBg.copy(alpha = if (isHovered) 0.95f else 0.85f),
                            RobotTheme.colors.surfaceOverlay.copy(alpha = if (isHovered) 0.1f else 0.05f)
                        )
                    } else {
                        listOf(
                            Color.White,
                            Color.White.copy(alpha = 0.8f)
                        )
                    }
                )
            )
            .clickable {
                isHovered = true
                onClick()
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                RobotTheme.colors.accent,
                                RobotTheme.colors.accentBg
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = card.iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = card.titleResId),
                        color = RobotTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Animated star shown when hovered
                    AnimatedVisibility(
                        visible = isHovered,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = RobotTheme.colors.accent
                        )
                    }
                }
                Text(
                    text = stringResource(id = card.contentResId),
                    color = RobotTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            // Arrow
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = if (isHovered) 4.dp else 0.dp),
                tint = RobotTheme.colors.accent.copy(alpha = if (isHovered) 1f else 0.3f)
            )
        }

        // Progress bar
        if (showProgress) {
            CardProgressBar(
                duration = progressDuration,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}

/**
 * Animated progress bar for carousel timing
 */
@Composable
private fun CardProgressBar(
    duration: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = duration, easing = LinearEasing)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(1.dp))
            .background(RobotTheme.colors.accent.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.value)
                .fillMaxHeight()
                .background(RobotTheme.colors.accent)
        )
    }
}

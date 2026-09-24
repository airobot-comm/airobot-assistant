package com.airobot.features.aiserv.popup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.DialIndicatorOrange
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.delay

/**
 * Stateless data representation of a popup card service item for UI rendering only.
 */
data class PopupQueueWidgetItem(
    val id: String,
    val displayName: String,      // max 4 chars
    val value: String,            // e.g., "08:30", "12:00"
    val icon: ImageVector,
    val customIcon: (@Composable () -> Unit)? = null,
    val color: Color,
    val isActive: Boolean
)

/**
 * A stateless carousel widget showing rotating popup card statuses.
 */
@Composable
fun PopupQueueWidget(
    items: List<PopupQueueWidgetItem>,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    carouselIntervalMs: Long = 5000L,
    onItemClick: (id: String) -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    val textMuted = RobotTheme.colors.textMuted
    val scaleRatio = size / 120.dp

    var currentIndex by remember { mutableStateOf(0) }

    // Clamp or reset index if items count changes
    LaunchedEffect(items.size) {
        if (currentIndex >= items.size) {
            currentIndex = 0
        }
    }

    // Auto-rotation carousel trigger
    LaunchedEffect(items.size, carouselIntervalMs) {
        if (items.size > 1) {
            while (true) {
                delay(carouselIntervalMs)
                currentIndex = (currentIndex + 1) % items.size
            }
        }
    }

    // Pulsing dots alpha animation for active items
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val currentItem = if (items.isEmpty()) {
        PopupQueueWidgetItem(
            id = "standby",
            displayName = "待机",
            value = "无服务",
            icon = Icons.Outlined.HourglassEmpty,
            color = textMuted,
            isActive = false
        )
    } else {
        items.getOrElse(currentIndex) { items.first() }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp * scaleRatio)
    ) {
        // Main Widget Container
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(24.dp * scaleRatio))
                .background(
                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.80f)
                    else Color(0xFFE2E8F0).copy(alpha = 0.50f)
                )
                .border(
                    width = 1.5.dp * scaleRatio,
                    color = if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1),
                    shape = RoundedCornerShape(24.dp * scaleRatio)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (items.isNotEmpty()) {
                            onItemClick(currentItem.id)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = if (items.isEmpty()) 0 else currentIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.fillMaxSize(),
                label = "popupQueueSwitch"
            ) { targetIndex ->
                val item = if (items.isEmpty()) {
                    PopupQueueWidgetItem(
                        id = "standby",
                        displayName = "待机",
                        value = "无服务",
                        icon = Icons.Outlined.HourglassEmpty,
                        color = textMuted,
                        isActive = false
                    )
                } else {
                    items.getOrElse(targetIndex) { items.first() }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp * scaleRatio)
                    ) {
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp * scaleRatio),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp * scaleRatio)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.customIcon != null) {
                                    Box(modifier = Modifier.size(24.dp * scaleRatio)) {
                                        item.customIcon.invoke()
                                    }
                                } else {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.displayName,
                                        modifier = Modifier.size(24.dp * scaleRatio),
                                        tint = item.color
                                    )
                                }
                            }
                        }

                        // Display Name (max 4 chars)
                        Text(
                            text = item.displayName.take(4),
                            color = if (isDark) Color.White.copy(alpha = 0.40f) else Color(
                                0xFF0F172A
                            ).copy(alpha = 0.40f),
                            fontSize = 8.sp * scaleRatio,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )

                        // Value
                        Text(
                            text = item.value,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            fontSize = 18.sp * scaleRatio,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                    }

                    // Pulsing active dots badge on the inner right edge of the card container
                    if (item.isActive) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp * scaleRatio),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = (-8).dp * scaleRatio)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp * scaleRatio)
                                    .clip(CircleShape)
                                    .background(item.color.copy(alpha = pulseAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp * scaleRatio)
                                    .clip(CircleShape)
                                    .background(item.color.copy(alpha = pulseAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp * scaleRatio)
                                    .clip(CircleShape)
                                    .background(item.color.copy(alpha = pulseAlpha))
                            )
                        }
                    }
                }
            }
        }

        // Progress dots at bottom (Outside the container)
        if (items.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp * scaleRatio),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(
                                if (i == currentIndex) 12.dp * scaleRatio else 4.dp * scaleRatio,
                                4.dp * scaleRatio
                            )
                            .clip(CircleShape)
                            .background(
                                if (i == currentIndex) DialIndicatorOrange
                                else Color(0xFF64748B).copy(alpha = 0.20f)
                            )
                    )
                }
            }
        }
    }
}

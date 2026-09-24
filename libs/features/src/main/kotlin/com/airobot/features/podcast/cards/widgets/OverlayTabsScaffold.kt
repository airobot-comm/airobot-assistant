package com.airobot.features.podcast.cards.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.airobot.framework.cards.OverlayBackdrop
import com.airobot.framework.theme.RobotTheme

/**
 * Shared overlay layout shell with right-hand hanging tabs and a main skeuomorphic card.
 */
@Composable
fun OverlayTabsScaffold(
    onClose: () -> Unit,
    enabled: Boolean = true,
    isDark: Boolean = RobotTheme.isDark,
    timeoutDurationMs: Long = 60000L,
    isKeepAlive: Boolean = false,
    tabs: @Composable ColumnScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val cardBorderColor = if (isDark) Color(0xFF2E323E) else Color(0xFFE6DDC4)

    OverlayBackdrop(
        onClose = onClose,
        enabled = enabled,
        timeoutDurationMs = timeoutDurationMs,
        isKeepAlive = isKeepAlive
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight

            val totalWidth = if (availableWidth < 515.dp) (availableWidth - 32.dp).coerceAtLeast(250.dp) else 515.dp
            val totalHeight = if (availableHeight < 580.dp) availableHeight else 580.dp
            val tabsWidth = 50.dp
            val mainCardWidth = if (availableWidth < 515.dp) (totalWidth - 45.dp).coerceAtLeast(200.dp) else 470.dp
            val xOffset = if (availableWidth < 515.dp) 0.dp else 22.5.dp

            Box(
                modifier = Modifier
                    .width(totalWidth)
                    .height(totalHeight)
                    .offset(x = xOffset)
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.CenterStart
            ) {
                // --- Right Hanging Tabs Column (drawn behind the main card) ---
                Column(
                    modifier = Modifier
                        .width(tabsWidth)
                        .align(Alignment.CenterEnd)
                        .zIndex(0f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    tabs()
                }

                // --- Main Skeuomorphic Card Box ---
                Box(
                    modifier = Modifier
                        .width(mainCardWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(36.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF242730), Color(0xFF16181F))
                                else listOf(Color(0xFFFAF5EB), Color(0xFFF3EDE0))
                            )
                        )
                        .border(2.dp, cardBorderColor, RoundedCornerShape(36.dp))
                        .clickable(enabled = false) {} // block backdrop click
                        .zIndex(1f)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * A reusable hanging tab container.
 */
@Composable
fun OverlayHangingTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tabHeight: Dp = 70.dp,
    visibleWidth: Dp = 45.dp,
    backgroundColor: Color = if (RobotTheme.isDark) Color(0xFF38434F) else Color(0xFFC5D1D8),
    borderColor: Color = if (RobotTheme.isDark) Color(0xFF2E323E) else Color(0xFFE6DDC4),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .width(50.dp)
            .height(tabHeight)
            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
            .background(backgroundColor)
            .border(
                1.dp,
                borderColor,
                RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .width(visibleWidth)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * The Canvas-drawn diagonal collapse arrows icon.
 */
@Composable
fun OverlayCloseArrowIcon(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val strokeColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
        val strokeWidth = 2.dp.toPx()

        // Top-Left arrow pointing to center
        drawLine(strokeColor, Offset(0f, 0f), Offset(w * 0.35f, h * 0.35f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.35f, h * 0.35f), Offset(w * 0.12f, h * 0.35f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.35f, h * 0.35f), Offset(w * 0.35f, h * 0.12f), strokeWidth, StrokeCap.Round)

        // Bottom-Right arrow pointing to center
        drawLine(strokeColor, Offset(w, h), Offset(w * 0.65f, h * 0.65f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.65f, h * 0.65f), Offset(w * 0.88f, h * 0.65f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.65f, h * 0.65f), Offset(w * 0.65f, h * 0.88f), strokeWidth, StrokeCap.Round)

        // Top-Right arrow pointing to center
        drawLine(strokeColor, Offset(w, 0f), Offset(w * 0.65f, h * 0.35f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.65f, h * 0.35f), Offset(w * 0.88f, h * 0.35f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.65f, h * 0.35f), Offset(w * 0.65f, h * 0.12f), strokeWidth, StrokeCap.Round)

        // Bottom-Left arrow pointing to center
        drawLine(strokeColor, Offset(0f, h), Offset(w * 0.35f, h * 0.65f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.35f, h * 0.65f), Offset(w * 0.12f, h * 0.65f), strokeWidth, StrokeCap.Round)
        drawLine(strokeColor, Offset(w * 0.35f, h * 0.65f), Offset(w * 0.35f, h * 0.88f), strokeWidth, StrokeCap.Round)
    }
}

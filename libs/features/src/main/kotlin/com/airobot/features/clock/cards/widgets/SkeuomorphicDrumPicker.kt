package com.airobot.features.clock.cards.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.theme.RobotTheme
import kotlin.math.abs

/**
 * Reusable 3D skeuomorphic cylindrical drum picker wheel for hours, minutes, and durations.
 * Supports snapped lazy list scrolling with infinite feel.
 *
 * @param minVal The minimum integer value in the picker range.
 * @param maxVal The maximum integer value in the picker range.
 * @param value The current selected value.
 * @param onValueChange Callback when selection updates.
 * @param width The custom width of the drum visual container.
 */
@Composable
fun SkeuomorphicDrumPicker(
    minVal: Int,
    maxVal: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 80.dp
) {
    val itemHeight = 46.dp
    val visibleItemsCount = 3
    val containerHeight = itemHeight * visibleItemsCount

    Box(
        modifier = modifier
            .width(width)
            .height(containerHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(RobotTheme.colors.chassisDialFace)
            .border(1.dp, RobotTheme.colors.cardBorder, RoundedCornerShape(12.dp))
    ) {
        // Active Center Highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
                .background(RobotTheme.colors.accent.copy(alpha = 0.08f))
        )

        // Drum Roller Column
        DrumPickerColumn(
            minVal = minVal,
            maxVal = maxVal,
            value = value,
            onValueChange = onValueChange,
            itemHeight = itemHeight
        )

        // Top Mask Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            RobotTheme.colors.chassisDialFace,
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Mask Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            RobotTheme.colors.chassisDialFace
                        )
                    )
                )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrumPickerColumn(
    minVal: Int,
    maxVal: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    itemHeight: Dp,
    modifier: Modifier = Modifier
) {
    val itemCount = maxVal - minVal + 1
    val totalItems = 10000
    val middleOffset = totalItems / 2

    // Find closest index for initial value near the middle of lazy column to support smooth infinite feel
    val initialOffsetValue = (value - minVal).coerceIn(0, itemCount - 1)
    val startIndex = middleOffset - (middleOffset % itemCount) + initialOffsetValue

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
        val selectedOffset = lazyListState.firstVisibleItemIndex % itemCount
        val selectedValue = minVal + selectedOffset
        onValueChange(selectedValue)
    }

    LazyColumn(
        state = lazyListState,
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight),
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(totalItems) { index ->
            val offsetVal = index % itemCount
            val actualValue = minVal + offsetVal

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        val firstVisible = lazyListState.firstVisibleItemIndex
                        val offset = lazyListState.firstVisibleItemScrollOffset
                        val currentCenterIndex = firstVisible + (offset.toFloat() / size.height)
                        val distance = index - currentCenterIndex

                        this.scaleX = 1f - (abs(distance) * 0.12f).coerceAtMost(0.3f)
                        this.scaleY = 1f - (abs(distance) * 0.12f).coerceAtMost(0.3f)
                        this.rotationX = distance * -22f
                        this.alpha = 1f - (abs(distance) * 0.45f).coerceAtMost(0.8f)
                        this.cameraDistance = 8f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                val isSelected = index == lazyListState.firstVisibleItemIndex
                Text(
                    text = String.format("%02d", actualValue),
                    fontSize = 24.sp,
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                    color = if (isSelected) RobotTheme.colors.accent else RobotTheme.colors.textPrimary
                )
            }
        }
    }
}

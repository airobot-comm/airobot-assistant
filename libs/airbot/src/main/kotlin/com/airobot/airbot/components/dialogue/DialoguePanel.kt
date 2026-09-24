package com.airobot.airbot.components.dialogue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.airbot.R
import com.airobot.agent.brain.model.Message
import com.airobot.airbot.viewmodel.RobotVisualState
import com.airobot.framework.theme.RobotTheme
import kotlinx.coroutines.launch

/**
 * Aether 对话流面板 - 纯净对话容器 (Decoupled Version)
 */
@Composable
internal fun DialoguePanel(
    isVisible: Boolean,
    visualState: RobotVisualState,
    messages: List<Message>,
    scaleRatio: Float = 1.0f,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = RobotTheme.isDark
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier
    ) {
        // The main container box for the dialogue content
        Box(
            modifier = Modifier
                .width(320.dp * scaleRatio)
                // Height is controlled by parent (weight or fillMaxSize)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(40.dp * scaleRatio),
                    spotColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(40.dp * scaleRatio))
                .background(
                    if (isDark) Color(0xFF0F172A).copy(alpha = 0.96f)
                    else Color.White.copy(alpha = 0.98f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(40.dp * scaleRatio)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Area (Logo + Title + Status + Close)
                PanelHeader(
                    visualState = visualState,
                    scaleRatio = scaleRatio,
                    onClose = onClose
                )

                // Divider line between header and content
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp * scaleRatio),
                    thickness = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                )

                Spacer(modifier = Modifier.height(8.dp * scaleRatio))

                // Message List Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp * scaleRatio)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp * scaleRatio),
                        contentPadding = PaddingValues(bottom = 12.dp * scaleRatio)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            DialogueMessageItem(
                                message = msg,
                                isDark = isDark,
                                scaleRatio = scaleRatio
                            )
                        }

                        // Thinking indicator
                        if (visualState == RobotVisualState.THINKING) {
                            item {
                                ThinkingItem(scaleRatio = scaleRatio)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    visualState: RobotVisualState,
    scaleRatio: Float,
    onClose: () -> Unit
) {
    val isDark = RobotTheme.isDark
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 14.dp * scaleRatio,
                bottom = 8.dp * scaleRatio,
                start = 84.dp * scaleRatio,
                end = 16.dp * scaleRatio
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp * scaleRatio)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp * scaleRatio)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFFFFAF0))
                        .padding(4.dp * scaleRatio),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = com.airobot.framework.R.drawable.chat),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp * scaleRatio),
                        tint = Color(0xFFF97316)
                    )
                }
                Text(
                    text = stringResource(R.string.dialogue_default_title), // Prototype specific title
                    color = if (isDark) Color.White else Color(0xFF1E293B),
                    fontSize = 14.sp * scaleRatio,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp * scaleRatio),
                modifier = Modifier.padding(top = 2.dp * scaleRatio)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "status")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(7.dp * scaleRatio)
                        .clip(CircleShape)
                        .graphicsLayer { this.alpha = alpha }
                        .background(Color(0xFF22D3EE))
                )
                Text(
                    text = when (visualState) {
                        RobotVisualState.LISTENING -> stringResource(R.string.aether_state_listening)
                        RobotVisualState.THINKING -> stringResource(R.string.aether_state_thinking)
                        RobotVisualState.SPEAKING -> stringResource(R.string.aether_state_speaking)
                        else -> stringResource(R.string.aether_state_ready)
                    },
                    color = Color(0xFF94A3B8),
                    fontSize = 8.sp * scaleRatio,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Close Button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(36.dp * scaleRatio)
                .clip(CircleShape)
                .background(
                    if (isDark) Color.White.copy(alpha = 0.05f)
                    else Color(0xFFF8FAFC)
                )
                .border(
                    width = 1.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.common_close),
                modifier = Modifier.size(18.dp * scaleRatio),
                tint = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun ThinkingItem(scaleRatio: Float) {
    Row(
        modifier = Modifier.padding(vertical = 12.dp * scaleRatio, horizontal = 4.dp * scaleRatio),
        horizontalArrangement = Arrangement.spacedBy(6.dp * scaleRatio)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "thinking")
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 100)
                ),
                label = "y"
            )
            Box(
                modifier = Modifier
                    .offset(y = yOffset.dp * scaleRatio)
                    .size(5.dp * scaleRatio)
                    .clip(CircleShape)
                    .background(Color(0xFFF97316))
            )
        }
    }
}

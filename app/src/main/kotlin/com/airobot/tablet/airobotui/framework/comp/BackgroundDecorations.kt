package com.airobot.tablet.airobotui.framework.comp

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.airobot.tablet.airobotui.framework.theme.RobotTheme

@Composable
fun BackgroundDecorations() {
    val infiniteTransition = rememberInfiniteTransition(label = "bgAnimation")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-150).dp)
                .size(400.dp * pulseScale)
                .clip(CircleShape)
                .background(RobotTheme.colors.backgroundShapes)
                .blur(120.dp)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(RobotTheme.colors.backgroundShapes.copy(alpha = RobotTheme.colors.backgroundShapes.alpha * 1.5f))
                .blur(100.dp)
        )

        // 左侧小气泡 (还原原型图左侧装饰，位于机器人左侧)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 140.dp, y = (-40).dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.03f))
                .blur(30.dp)
        )
    }
}



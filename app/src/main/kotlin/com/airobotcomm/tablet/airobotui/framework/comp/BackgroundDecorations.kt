package com.airobotcomm.tablet.airobotui.framework.comp

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
                .background(Color(0xFF3B82F6).copy(alpha = 0.05f))
                .blur(120.dp)
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .clip(CircleShape)
                .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                .blur(100.dp)
        )
    }
}

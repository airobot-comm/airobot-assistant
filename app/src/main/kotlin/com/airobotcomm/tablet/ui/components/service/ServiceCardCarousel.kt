package com.airobotcomm.tablet.ui.components.service

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.ui.components.robot.ServiceCard
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * 服务卡片轮播组件
 * 
 * Web原型对应: ProactiveServiceKit.tsx + 时钟显示
 * 
 * 功能:
 * - 自动轮播服务卡片
 * - 显示当前时间
 * - 卡片切换动画
 */
@Composable
fun ServiceCardCarousel(
    cards: List<ServiceCard>,
    onCardClick: (ServiceCard) -> Unit,
    modifier: Modifier = Modifier,
    autoScrollInterval: Long = 10000L
) {
    var currentIndex by remember { mutableStateOf(0) }
    var currentTime by remember { mutableStateOf(Date()) }
    
    // 时间更新
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTime = Date()
        }
    }
    
    // 自动轮播
    LaunchedEffect(cards.size) {
        if (cards.isNotEmpty()) {
            while (true) {
                delay(autoScrollInterval)
                currentIndex = (currentIndex + 1) % cards.size
            }
        }
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 时钟显示
        ClockDisplay(currentTime = currentTime)
        
        // 卡片轮播
        if (cards.isNotEmpty()) {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { -it } + fadeOut()
                },
                label = "cardCarousel"
            ) { index ->
                val card = cards.getOrNull(index) ?: cards.first()
                ServiceCardItem(
                    card = card,
                    onClick = { onCardClick(card) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 时钟显示组件
 */
@Composable
private fun ClockDisplay(
    currentTime: Date,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 时间
        Text(
            text = timeFormat.format(currentTime),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-2).sp
        )
        
        // 系统标识
        Text(
            text = "AETHER SYSTEM CLOCK",
            color = Color(0xFF22D3EE).copy(alpha = 0.3f), // cyan-400
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.sp
        )
    }
}

/**
 * 简单卡片列表（不轮播）
 */
@Composable
fun ServiceCardList(
    cards: List<ServiceCard>,
    onCardClick: (ServiceCard) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        cards.forEach { card ->
            ServiceCardItem(
                card = card,
                onClick = { onCardClick(card) },
                showProgress = false
            )
        }
    }
}

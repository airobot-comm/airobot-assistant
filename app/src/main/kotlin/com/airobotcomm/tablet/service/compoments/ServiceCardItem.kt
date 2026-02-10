package com.airobotcomm.tablet.service.compoments

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobotcomm.tablet.R
import com.airobotcomm.tablet.airobotui.state.ServiceCard
import com.airobotcomm.tablet.airobotui.state.ServiceCardType

/**
 * 服务卡片组件
 * 
 * Web原型对应: ProactiveServiceKit.tsx
 * 
 * 功能:
 * - 显示服务卡片信息
 * - 点击交互
 * - 进度条动画
 */
@Composable
fun ServiceCardItem(
    card: ServiceCard,
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
                    colors = listOf(
                        Color.White.copy(alpha = if (isHovered) 0.08f else 0.05f),
                        Color.White.copy(alpha = if (isHovered) 0.04f else 0.02f)
                    )
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
            // 图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1), // indigo-500
                                Color(0xFF22D3EE)  // cyan-500
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = getServiceCardIcon(card.type)),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
            
            // 内容
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // 闪烁图标（hover时显示）
                    AnimatedVisibility(
                        visible = isHovered,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF22D3EE) // cyan-400
                        )
                    }
                }
                Text(
                    text = card.content,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            
            // 箭头
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .offset(x = if (isHovered) 4.dp else 0.dp),
                tint = Color(0xFF22D3EE).copy(alpha = if (isHovered) 1f else 0.2f)
            )
        }
        
        // 进度条
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
 * 卡片进度条
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
            .background(Color(0xFF22D3EE).copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.value)
                .fillMaxHeight()
                .background(Color(0xFF22D3EE))
        )
    }
}

/**
 * 获取服务卡片图标
 */
fun getServiceCardIcon(type: ServiceCardType): Int {
    return when (type) {
        ServiceCardType.TIMER -> R.drawable.timer
        ServiceCardType.STORY -> R.drawable.book
        ServiceCardType.CHAT -> R.drawable.chat
        ServiceCardType.GAME -> R.drawable.game
        ServiceCardType.DRAW -> R.drawable.palette
        ServiceCardType.QUIZ -> R.drawable.star
        ServiceCardType.ALARM -> R.drawable.alarm
        ServiceCardType.WEATHER -> R.drawable.cloud_on
        ServiceCardType.MUSIC -> R.drawable.music
    }
}

/**
 * 预定义的服务卡片池
 */
val DEFAULT_SERVICE_CARDS = listOf(
    ServiceCard(
        id = "card-timer",
        type = ServiceCardType.TIMER,
        title = "专注时钟",
        content = "番茄工作法助手",
        statusTip = "该专注一会了",
        iconResId = R.drawable.timer
    ),
    ServiceCard(
        id = "card-story",
        type = ServiceCardType.STORY,
        title = "故事时间",
        content = "一起探索比特森林的奥秘",
        statusTip = "想听个故事吗？",
        iconResId = R.drawable.book
    ),
    ServiceCard(
        id = "card-chat",
        type = ServiceCardType.CHAT,
        title = "随心聊天",
        content = "今天过得怎么样？",
        statusTip = "找我聊聊天吧",
        iconResId = R.drawable.chat
    ),
    ServiceCard(
        id = "card-game",
        type = ServiceCardType.GAME,
        title = "益智小游戏",
        content = "寻找隐藏的星星",
        statusTip = "来玩个游戏？",
        iconResId = R.drawable.game
    ),
    ServiceCard(
        id = "card-draw",
        type = ServiceCardType.DRAW,
        title = "涂鸦创作",
        content = "画一架太空飞船",
        statusTip = "我们来画画吧",
        iconResId = R.drawable.palette
    ),
    ServiceCard(
        id = "card-quiz",
        type = ServiceCardType.QUIZ,
        title = "趣味问答",
        content = "空间知识大挑战",
        statusTip = "考考你的知识",
        iconResId = R.drawable.star
    )
)

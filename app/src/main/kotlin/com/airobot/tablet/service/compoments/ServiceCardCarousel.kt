package com.airobot.tablet.service.compoments

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.tablet.airobotui.state.ServiceCard
import com.airobot.tablet.airobotui.framework.theme.RobotTheme
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
    currentIndex: Int,
    onPageChanged: (Int) -> Unit,
    statusTip: String? = null,
    modifier: Modifier = Modifier
) {
    
    // 已经移除了时间显示，首页时间由 TopBar 统一负责
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // 状态提示 (从 Airobot 模型迁移到此处)
        AnimatedVisibility(
            visible = statusTip != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            StatusTipHeader(tip = statusTip ?: "")
        }
        
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
 * 状态提示头部 (迁移自 RobotCharacter)
 */
@Composable
private fun StatusTipHeader(
    tip: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 小圆点指示器
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(RobotTheme.colors.accent, CircleShape)
        )
        
        Text(
            text = tip,
            color = RobotTheme.colors.textPrimary.copy(alpha = 0.9f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
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



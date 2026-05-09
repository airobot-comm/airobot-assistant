package com.airobot.services.compoments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.framework.R
import com.airobot.framework.theme.RobotTheme
import com.airobot.services.FocusTimerWidget
import com.airobot.services.state.ServiceCard
import com.airobot.services.state.ServiceCardType
import com.airobot.services.state.ServiceCardData
import com.airobot.services.state.TimerCardData
import com.airobot.services.state.ServiceSubState

/**
 * 服务详情模块面板
 * 用于展示特定服务的详细交互界面（如专注时钟、天气详情等）
 */
@Composable
fun ServiceDetailPanel(
    card: ServiceCard?,
    activeServiceData: ServiceCardData?,
    serviceSubState: ServiceSubState,
    onTimerComplete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (card == null) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(32.dp))
            .background(RobotTheme.colors.cardBg.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (card.type == ServiceCardType.TIMER) {
                                        listOf(Color(0xFFEF4444), Color(0xFFF97316))
                                    } else {
                                        listOf(Color(0xFF22D3EE), Color(0xFF6366F1))
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = getServiceCardIcon(card.type)),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = card.title,
                            color = RobotTheme.colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AETHER SYSTEM MODULE",
                            color = RobotTheme.colors.accent.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RobotTheme.colors.surfaceOverlay.copy(alpha = 0.05f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = "关闭",
                        modifier = Modifier.size(16.dp),
                        tint = RobotTheme.colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (card.type == ServiceCardType.TIMER) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            val duration = (activeServiceData as? TimerCardData)?.duration ?: 0
                            val task = (activeServiceData as? TimerCardData)?.task ?: ""
                            FocusTimerWidget(
                                duration = duration,
                                task = task,
                                serviceSubState = serviceSubState,
                                onTimerComplete = onTimerComplete
                            )
                        }
                        card.demoContent?.let {
                            Text(
                                text = it,
                                color = RobotTheme.colors.textSecondary,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp, start = 32.dp, end = 32.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = getServiceCardIcon(card.type)),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = RobotTheme.colors.textMuted.copy(alpha = 0.3f)
                            )
                            Text(
                                text = card.demoContent ?: "${card.type.name} 功能开发中",
                                color = RobotTheme.colors.textSecondary,
                                fontSize = 18.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

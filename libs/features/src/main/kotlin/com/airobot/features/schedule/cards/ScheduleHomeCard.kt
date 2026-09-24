package com.airobot.features.schedule.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.FeatureCards
import com.airobot.features.aiserv.guidance.components.AetherRemindBanner
import com.airobot.features.schedule.data.ScheduleUtils
import com.airobot.features.schedule.viewmodel.ScheduleViewModel
// Removed SubCategory import
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.ScheduleDateBg
import java.util.Calendar

/**
 * Schedule Home Card — today overview with date, weather, and top items.
 *
 * Prototype ref: TodayView.tsx + CalendarHomeView.tsx + docs/design/aischedule_home.png
 */
@Composable
fun ScheduleHomeCard(
    scheduleViewModel: ScheduleViewModel,
    onNavigateToBoard: () -> Unit,
    modifier: Modifier = Modifier,
    onRemindClick: (String) -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    val time = remember { Calendar.getInstance() }

    val schedulesList by scheduleViewModel.schedules.collectAsState()
    val todosList by scheduleViewModel.todos.collectAsState()

    val todayStr = ScheduleUtils.getLocalDateString(time)
    val todayDOW = time.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 1=Mon, ..., 6=Sat

    // Filter today's schedules & todos
    val todaySchedules =
        schedulesList.filter { it.date == todayStr || (it.date == null && it.dayOfWeek == todayDOW) }
    val todayTodos = todosList.filter { it.date == todayStr }

    val combinedItems = remember(todaySchedules, todayTodos) {
        val list = mutableListOf<CombinedDisplayItem>()
        todayTodos.forEach {
            list.add(
                CombinedDisplayItem(
                    "todo",
                    it.id,
                    it.task,
                    it.status == "closed"
                )
            )
        }
        todaySchedules.forEach {
            list.add(
                CombinedDisplayItem(
                    "schedule",
                    it.id,
                    it.task,
                    it.completed
                )
            )
        }
        list
    }

    val todayInfo = remember(time) { ScheduleUtils.getTodayInfo(time) }

    val monthName =
        time.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.CHINESE) ?: "六月"
    val weekDayName =
        time.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, java.util.Locale.CHINESE)
            ?: "星期二"
    val dayOfMonth = time.get(Calendar.DAY_OF_MONTH)
    val context = LocalContext.current

    val remindCards by scheduleViewModel.remindCards.collectAsState(initial = emptyList())

    // Aether Banner Pages
    val mappedRemindCards = remindCards.map { card ->
        if (card.tag == FeatureCards.SCHEDULE_PLANNER) {
            card.copy(content = "今天是 ${time.get(Calendar.MONTH) + 1}月${dayOfMonth}日。天气${todayInfo.weatherCondition}，气温${todayInfo.weatherTemp}；今日共有 ${combinedItems.size} 项事务安排，Aether建议您挑重点优先处理。")
        } else {
            card
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header Slogan
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDark) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(
                            0xFFEEF2F6
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI 日程",
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Aether 帮你规划日程安排，请直接语音指示",
                    color = RobotTheme.colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main Functional Split (Left Standalone Card, Right List Area)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Standalone Left Date Card
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFFF8C00), Color(0xFFE55D00))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = monthName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$dayOfMonth",
                        color = Color.White,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2).sp
                    )
                    Spacer(
                        modifier = Modifier
                            .height(2.dp)
                            .width(32.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    Text(
                        text = weekDayName,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Right Content Area Card
            Box(
                modifier = Modifier
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (combinedItems.size > 3) 18.dp else 0.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(
                                0xFFF8FAFC
                            )
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9),
                            RoundedCornerShape(32.dp)
                        )
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = if (combinedItems.size > 3) 26.dp else 16.dp
                        )
                ) {
                    // Info Tags Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Weather tag
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDark) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(
                                        0xFFE0F2FE
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cloud,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${todayInfo.weatherCondition} ${todayInfo.weatherTemp}",
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Lunar date tag
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDark) Color(0xFFD97706).copy(alpha = 0.2f) else Color(
                                        0xFFFEF3C7
                                    )
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = todayInfo.lunarDate,
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Festival or Holiday tag
                        val hasFestival = todayInfo.festival != null
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (hasFestival) {
                                        if (isDark) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(
                                            0xFFFFE4E6
                                        )
                                    } else {
                                        if (isDark) Color(0xFF475569).copy(alpha = 0.5f) else Color.White
                                    }
                                )
                                .border(
                                    width = if (hasFestival) 0.dp else 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.1f) else Color(
                                        0xFFE2E8F0
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (hasFestival) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = todayInfo.festival ?: "无节假日",
                                color = if (hasFestival) {
                                    if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
                                } else {
                                    if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "今日重要日程",
                        color = RobotTheme.colors.textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // List Items (Up to 3)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val displayList = combinedItems.take(3)
                        displayList.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color.White)
                                    .border(
                                        1.dp,
                                        if (isDark) Color.White.copy(alpha = 0.05f) else Color(
                                            0xFFEDF2F7
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        scheduleViewModel.setSelectedItem(item.id, item.type)
                                        onRemindClick("schedule") // Open the overlay
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Orange gradient dot
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFFFF8C00), Color(0xFFE55D00))
                                            )
                                        )
                                )

                                Text(
                                    text = item.task,
                                    color = if (item.completed) RobotTheme.colors.textMuted else RobotTheme.colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                            }
                        }

                        if (combinedItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "今日无特定日程事务",
                                    color = RobotTheme.colors.textMuted,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Hanging Remaining Items Button
                if (combinedItems.size > 3) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isDark) Color(0xFF334155).copy(alpha = 0.3f)
                                else Color(0xFFF8FAFC)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color.Transparent
                                else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                onNavigateToBoard()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val elementColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            Text(
                                text = "查看其余 ${combinedItems.size - 3} 项日程安排",
                                color = elementColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = null,
                                tint = elementColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom recommendation banner
        AetherRemindBanner(
            pages = mappedRemindCards,
            cardIcon = Icons.Outlined.CalendarMonth,
            accentColor = ScheduleDateBg,
            onPageClick = { page ->
                onRemindClick(page.actionTarget)
            }
        )
    }
}

private data class CombinedDisplayItem(
    val type: String,
    val id: String,
    val task: String,
    val completed: Boolean
)

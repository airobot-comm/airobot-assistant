package com.airobot.features.schedule.cards

import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.schedule.data.ScheduleUtils
import com.airobot.features.schedule.viewmodel.ScheduleViewModel
import com.airobot.framework.cards.OverlayBackdrop
import com.airobot.framework.theme.DarkAccent
import com.airobot.framework.theme.RobotTheme
import com.airobot.framework.theme.ScheduleDateBg
import com.airobot.framework.theme.StatusCyan
import com.airobot.framework.theme.StatusEmerald
import java.util.Calendar
import java.util.Locale

@Composable
fun SchedulePlannerOverlay(
    scheduleViewModel: ScheduleViewModel,
    onClose: () -> Unit
) {
    val isDark = RobotTheme.isDark
    val context = LocalContext.current
    val today = remember { Calendar.getInstance() }
    var dateOffset by remember { mutableStateOf(0) }

    // Selected Date Calculation
    val activeDate = remember(dateOffset, today) {
        val d = today.clone() as Calendar
        d.add(Calendar.DAY_OF_MONTH, dateOffset)
        d
    }

    val activeDateStr = ScheduleUtils.getLocalDateString(activeDate)
    val dayOfWeek = activeDate.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    val relativeDateLabel = when (dateOffset) {
        0 -> "今日"
        -1 -> "昨天"
        -2 -> "前天"
        1 -> "明天"
        2 -> "后天"
        else -> if (dateOffset > 0) "${dateOffset}天后" else "${Math.abs(dateOffset)}天前"
    }

    val todayInfo = remember(activeDate) { ScheduleUtils.getTodayInfo(activeDate) }

    // Error Toast handling
    val errorMessage by scheduleViewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            scheduleViewModel.clearError()
        }
    }

    // Filter items for active date
    val schedulesList by scheduleViewModel.schedules.collectAsState()
    val todosList by scheduleViewModel.todos.collectAsState()

    val activeSchedules = schedulesList.filter {
        it.date == activeDateStr || (it.date == null && it.dayOfWeek == dayOfWeek)
    }
    val activeTodos = todosList.filter { it.date == activeDateStr }

    val activeCombinedItems = remember(activeSchedules, activeTodos) {
        val list = mutableListOf<PlannerDisplayItem>()
        activeTodos.forEach { list.add(PlannerDisplayItem("todo", it.id, it.task, it.time ?: "12:00", "work")) }
        activeSchedules.forEach { list.add(PlannerDisplayItem("schedule", it.id, it.task, it.time, it.category)) }
        list.sortedBy { it.time }
    }

    OverlayBackdrop(
        onClose = onClose,
        enabled = true
    ) {
        Box(
            modifier = Modifier
                .width(500.dp)
                .height(700.dp)
        ) {
            // Right Side Knobs
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 48.dp, y = (-40).dp),
                verticalArrangement = Arrangement.spacedBy(40.dp)
            ) {
                // Add Item Knob
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 140.dp)
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF1E293B) else DarkAccent)
                                .clickable {
                                    val hour = (8 + activeCombinedItems.size * 3) % 24
                                    val timeStr = String.format(Locale.US, "%02d:00", hour)
                                    // Try adding schedule first, fallback to checking daily limits
                                    val success = scheduleViewModel.addSchedule(
                                        task = "新日程安排",
                                        time = timeStr,
                                        dayOfWeek = dayOfWeek,
                                        date = activeDateStr,
                                        category = if (activeCombinedItems.size % 2 == 0) "work" else "health"
                                    )
                                    if (success) {
                                        Toast.makeText(context, "已添加新日程安排", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .width(32.dp)
                                    .height(2.dp)
                                    .background(if (isDark) Color(0xFF475569) else Color(0xFF94A3B8))
                            )
                        }
                    }
                }

                // Close Knob
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 96.dp)
                        .clip(RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                        )
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Minimize",
                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Slate Board Body
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(48.dp))
                    .background(
                        Brush.verticalGradient(
                            if (isDark) listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
                            else listOf(Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFF2C3E50))
                        )
                    )
                    .border(
                        3.dp,
                        if (isDark) Color(0xFF111111) else Color(0xFF1A252F),
                        RoundedCornerShape(48.dp)
                    )
                    .padding(24.dp)
            ) {
                // Chalkboard noise overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.03f))
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI 日程板",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic
                            )
                            Text(
                                text = "Aether 帮你规划日程安排，请直接语音指示",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date & Weather Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${activeDate.get(Calendar.MONTH) + 1}月${activeDate.get(Calendar.DAY_OF_MONTH)}日 $relativeDateLabel",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = todayInfo.lunarDate,
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFF7DD3FC),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "${todayInfo.weatherCondition} ${todayInfo.weatherTemp}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Date Navigator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Prev",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { dateOffset-- }
                                    .padding(6.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp, 20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    repeat(2) {
                                        Box(
                                            modifier = Modifier
                                                .size(1.5.dp, 10.dp)
                                                .background(Color.Black.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Next",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier
                                    .size(28.dp)
                                    .clickable { dateOffset++ }
                                    .padding(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timeline Ruler & List Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .drawBehind {
                                // Vertical Ruler Line
                                val startX = 64.dp.toPx()
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(startX, 0f),
                                    end = Offset(startX, size.height),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                    ) {
                        // Hour Ticks
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            val hours = listOf(7, 9, 11, 13, 15, 17, 19, 21, 23)
                            hours.forEach { hour ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%02d:00", hour),
                                        color = Color.White.copy(alpha = 0.4f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(52.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(12.dp)
                                            .height(1.dp)
                                            .background(Color.White.copy(alpha = 0.25f))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.dp)
                                            .drawBehind {
                                                drawLine(
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    start = Offset(0f, 0f),
                                                    end = Offset(size.width, 0f),
                                                    strokeWidth = 1.dp.toPx(),
                                                    pathEffect = PathEffect.dashPathEffect(
                                                        floatArrayOf(6f, 6f)
                                                    )
                                                )
                                            }
                                    )
                                }
                            }
                        }

                        // Displaying Items dynamically
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 72.dp, top = 16.dp, bottom = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            activeCombinedItems.forEach { item ->
                                val categoryColor = when (item.category) {
                                    "work" -> ScheduleDateBg
                                    "health" -> StatusEmerald
                                    else -> StatusCyan
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(categoryColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (item.type == "schedule") Icons.Outlined.AccessTime else Icons.Outlined.CheckBox,
                                                contentDescription = null,
                                                tint = categoryColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = item.task,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                maxLines = 1
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = item.time,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(2.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.3f))
                                                )
                                                Text(
                                                    text = if (item.type == "schedule") "日程安排" else "待办",
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Delete Button
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable {
                                                if (item.type == "schedule") {
                                                    scheduleViewModel.deleteSchedule(item.id)
                                                } else {
                                                    scheduleViewModel.deleteTodo(item.id)
                                                }
                                                Toast.makeText(context, "事务已删除", Toast.LENGTH_SHORT).show()
                                            }
                                    )
                                }
                            }

                            if (activeCombinedItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "今日无任何规划，点击右侧 [+] 添加！",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PlannerDisplayItem(
    val type: String,
    val id: String,
    val task: String,
    val time: String,
    val category: String
)

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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airobot.features.schedule.data.ScheduleUtils
import com.airobot.features.schedule.data.model.ScheduleItem
import com.airobot.features.schedule.data.model.TodoItem
import com.airobot.features.schedule.viewmodel.ScheduleViewModel
import com.airobot.framework.theme.RobotTheme
import java.util.Calendar

@Composable
fun ScheduleListCard(
    scheduleViewModel: ScheduleViewModel,
    modifier: Modifier = Modifier,
    onRemindClick: (String) -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    val today = remember { Calendar.getInstance() }
    val todayStr = ScheduleUtils.getLocalDateString(today)

    val nextWeek = today.clone() as Calendar
    nextWeek.add(Calendar.DAY_OF_MONTH, 7)
    val nextWeekStr = ScheduleUtils.getLocalDateString(nextWeek)

    val schedulesList by scheduleViewModel.schedules.collectAsState()
    val todosList by scheduleViewModel.todos.collectAsState()

    var timeFilter by remember { mutableStateOf("today") } // "today", "week", "open", "overdue", "all-todos"

    val timeTabs = listOf(
        TimeTab("today", "今天"),
        TimeTab("week", "近7天"),
        TimeTab("open", "进行中"),
        TimeTab("overdue", "已过期"),
        TimeTab("all-todos", "所有待办")
    )

    // Filter Items dynamically
    val filteredItems = remember(schedulesList, todosList, timeFilter, todayStr, nextWeekStr) {
        val allList = mutableListOf<ListDisplayItem>()
        todosList.forEach { allList.add(ListDisplayItem.TodoItem(it)) }
        schedulesList.forEach { allList.add(ListDisplayItem.ScheduleItem(it)) }

        allList.filter { item ->
            var itemDate = ""
            var isOpen = false

            when (item) {
                is ListDisplayItem.TodoItem -> {
                    itemDate = item.todo.date ?: ""
                    isOpen = item.todo.status == "open"
                }
                is ListDisplayItem.ScheduleItem -> {
                    itemDate = item.schedule.date ?: ""
                    if (itemDate.isEmpty() && item.schedule.dayOfWeek != null) {
                        // Deriving date for recurring schedule
                        val dayDiff = (item.schedule.dayOfWeek - today.get(Calendar.DAY_OF_WEEK) + 1 + 7) % 7
                        val d = today.clone() as Calendar
                        d.add(Calendar.DAY_OF_MONTH, dayDiff)
                        itemDate = ScheduleUtils.getLocalDateString(d)
                    }
                    isOpen = itemDate >= todayStr
                }
            }

            when (timeFilter) {
                "all-todos" -> item is ListDisplayItem.TodoItem
                "open" -> isOpen
                "overdue" -> itemDate.isNotEmpty() && itemDate < todayStr && (if (item is ListDisplayItem.TodoItem) isOpen else true)
                "today" -> itemDate == todayStr
                "week" -> itemDate >= todayStr && itemDate <= nextWeekStr
                else -> true
            }
        }
    }

    // Aether Summary calculations
    val openTodos = todosList.filter { it.status == "open" }
    val overdueTodos = openTodos.filter { it.date != null && it.date < todayStr }
    val upcomingTodos = openTodos.filter { it.date == todayStr }

    val summaryText = remember(openTodos, overdueTodos, upcomingTodos) {
        when {
            openTodos.isEmpty() -> "太棒了，所有待办均已清空！享受属于你的时间吧。"
            overdueTodos.isNotEmpty() -> "目前共有 ${openTodos.size} 项待办。有 ${overdueTodos.size} 项已逾期，建议优先处理！"
            upcomingTodos.isNotEmpty() -> "目前共有 ${openTodos.size} 项待办。其中 ${upcomingTodos.size} 项将于今日到期，请把握进度。"
            else -> "目前共有 ${openTodos.size} 项待办。所有项均在计划内，事务流态健康。"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header block
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(0xFFFFE4E6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI 事务",
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "统一管理你的日程与待办",
                    color = RobotTheme.colors.textMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filters scroll row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            timeTabs.forEach { tab ->
                val isSelected = timeFilter == tab.id
                val bg = if (isSelected) {
                    Color(0xFFE11D48) // Rose Red
                } else {
                    if (isDark) Color(0xFF334155) else Color(0xFFEEF2F6)
                }
                val textCol = if (isSelected) Color.White else RobotTheme.colors.textMuted

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable { timeFilter = tab.id }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        color = textCol,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // List Scroll Area
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC))
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFEDF2F7),
                    RoundedCornerShape(24.dp)
                )
                .padding(12.dp)
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState.value, scrollState.maxValue, isDark, paddingRight = 0.dp)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredItems.forEach { item ->
                when (item) {
                    is ListDisplayItem.TodoItem -> {
                        val todo = item.todo
                        val isCompleted = todo.status == "closed"
                        val isOverdue = todo.date != null && todo.date < todayStr && todo.status == "open"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color.White)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFEDF2F7),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    scheduleViewModel.setSelectedItem(todo.id, "todo")
                                    onRemindClick("schedule")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                tint = if (isCompleted) Color(0xFF10B981) else RobotTheme.colors.textMuted.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        scheduleViewModel.toggleTodo(todo.id)
                                    }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = todo.task,
                                    color = if (isCompleted) RobotTheme.colors.textMuted else RobotTheme.colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isDark) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFFEE2E2))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "待办",
                                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!todo.date.isNullOrEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isOverdue) {
                                                        if (isDark) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFFEE2E2)
                                                    } else {
                                                        if (isDark) Color(0xFF475569) else Color(0xFFF1F5F9)
                                                    }
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (todo.date == todayStr) "今天" else todo.date,
                                                color = if (isOverdue) Color(0xFFEF4444) else RobotTheme.colors.textMuted,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (!todo.time.isNullOrEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccessTime,
                                                contentDescription = null,
                                                tint = RobotTheme.colors.textMuted,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = todo.time,
                                                color = RobotTheme.colors.textMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is ListDisplayItem.ScheduleItem -> {
                        val schedule = item.schedule
                        val dayOfWeekName = when (schedule.dayOfWeek) {
                            0 -> "周日"
                            1 -> "周一"
                            2 -> "周二"
                            3 -> "周三"
                            4 -> "周四"
                            5 -> "周五"
                            6 -> "周六"
                            else -> ""
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color.White)
                                .border(
                                    1.dp,
                                    if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFEDF2F7),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    scheduleViewModel.setSelectedItem(schedule.id, "schedule")
                                    onRemindClick("schedule")
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(0xFFEEF2F6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                    modifier = Modifier.size(10.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = schedule.task,
                                    color = RobotTheme.colors.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFFE0E7FF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "日程安排",
                                            color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!schedule.time.isNullOrEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccessTime,
                                                contentDescription = null,
                                                tint = RobotTheme.colors.textMuted,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = schedule.time,
                                                color = RobotTheme.colors.textMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (dayOfWeekName.isNotEmpty()) {
                                        Text(
                                            text = dayOfWeekName,
                                            color = RobotTheme.colors.textMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "当前筛选条件下没有匹配的事务",
                        color = RobotTheme.colors.textMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Summary Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White)
                .border(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFEDF2F7),
                    RoundedCornerShape(24.dp)
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(0xFFFFE4E6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "AETHER 事务概要",
                    color = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = summaryText,
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class TimeTab(val id: String, val label: String)

private sealed class ListDisplayItem {
    data class TodoItem(val todo: com.airobot.features.schedule.data.model.TodoItem) : ListDisplayItem()
    data class ScheduleItem(val schedule: com.airobot.features.schedule.data.model.ScheduleItem) : ListDisplayItem()
}

private fun Modifier.verticalScrollbar(
    scrollValue: Int,
    maxValue: Int,
    isDark: Boolean,
    width: Dp = 6.dp,
    paddingRight: Dp = 4.dp
): Modifier = this.drawWithContent {
    drawContent()
    
    if (maxValue > 0) {
        val viewportHeight = size.height
        val totalHeight = viewportHeight + maxValue
        
        val rawScrollbarHeight = (viewportHeight / totalHeight) * viewportHeight
        val minThumbHeight = 24.dp.toPx()
        val maxThumbHeight = (viewportHeight * 0.25f).coerceAtLeast(32.dp.toPx())
        val thumbHeight = rawScrollbarHeight.coerceIn(minThumbHeight, maxThumbHeight)
        
        val scrollbarTop = (scrollValue.toFloat() / maxValue) * (viewportHeight - thumbHeight)
        
        val x = size.width - width.toPx() - paddingRight.toPx()
        
        val trackColor = if (isDark) Color(0xFF334155).copy(alpha = 0.3f) else Color(0xFFF1F5F9)
        val thumbColor = if (isDark) Color(0xFF64748B).copy(alpha = 0.8f) else Color(0xFF94A3B8)
        
        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(x, 0f),
            size = Size(width.toPx(), viewportHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
        
        // Draw thumb
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(x, scrollbarTop),
            size = Size(width.toPx(), thumbHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}

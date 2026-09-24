package com.airobot.features.schedule.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
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
fun ScheduleBoardCard(
    scheduleViewModel: ScheduleViewModel,
    modifier: Modifier = Modifier,
    onRemindClick: (String) -> Unit = {}
) {
    val isDark = RobotTheme.isDark
    val today = remember { Calendar.getInstance() }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var viewMode by remember { mutableStateOf("week") } // "week" or "month"

    val schedulesList by scheduleViewModel.schedules.collectAsState()
    val todosList by scheduleViewModel.todos.collectAsState()

    // Helper functions
    fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun hasEventsForDate(date: Calendar): Boolean {
        val dateStr = ScheduleUtils.getLocalDateString(date)
        val dow = date.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
        val hasSchedule = schedulesList.any { it.date == dateStr || (it.date == null && it.dayOfWeek == dow) }
        val hasTodo = todosList.any { it.date == dateStr }
        return hasSchedule || hasTodo
    }

    // Week Grid generator
    val weekDaysDates = remember(selectedDate) {
        val startOfWeek = selectedDate.clone() as Calendar
        val dayOfWeek = startOfWeek.get(Calendar.DAY_OF_WEEK) - 1
        startOfWeek.add(Calendar.DAY_OF_MONTH, -dayOfWeek)
        List(7) { i ->
            val d = startOfWeek.clone() as Calendar
            d.add(Calendar.DAY_OF_MONTH, i)
            d
        }
    }

    val monthName = selectedDate.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.CHINESE) ?: "六月"
    val weekNumber = selectedDate.get(Calendar.WEEK_OF_YEAR)
    val dayInfo = remember(selectedDate) { ScheduleUtils.getTodayInfo(selectedDate) }
    val selectedDateStr = ScheduleUtils.getLocalDateString(selectedDate)

    // Calculate Relative Date String
    val relativeTimeStr = remember(selectedDate, today) {
        val dateStart = selectedDate.clone() as Calendar
        dateStart.set(Calendar.HOUR_OF_DAY, 0)
        dateStart.set(Calendar.MINUTE, 0)
        dateStart.set(Calendar.SECOND, 0)
        dateStart.set(Calendar.MILLISECOND, 0)

        val todayStart = today.clone() as Calendar
        todayStart.set(Calendar.HOUR_OF_DAY, 0)
        todayStart.set(Calendar.MINUTE, 0)
        todayStart.set(Calendar.SECOND, 0)
        todayStart.set(Calendar.MILLISECOND, 0)

        val diffDays = ((dateStart.timeInMillis - todayStart.timeInMillis) / 86400000).toInt()
        when (diffDays) {
            0 -> "今天"
            -1 -> "昨天"
            -2 -> "前天"
            1 -> "明天"
            2 -> "后天"
            else -> if (diffDays > 0) "${diffDays}天后" else "${Math.abs(diffDays)}天前"
        }
    }

    // Filter events for selected day
    val selectedDateSchedules = schedulesList.filter {
        if (it.date != null) it.date == selectedDateStr
        else it.dayOfWeek == selectedDate.get(Calendar.DAY_OF_WEEK) - 1
    }
    val selectedDateTodos = todosList.filter { it.date == selectedDateStr }

    val filteredItems = remember(selectedDateSchedules, selectedDateTodos) {
        val list = mutableListOf<BoardListItem>()
        selectedDateTodos.forEach { list.add(BoardListItem.TodoItem(it)) }
        selectedDateSchedules.forEach { list.add(BoardListItem.ScheduleItem(it)) }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.2f) else Color(0xFFEEF2F6)),
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
                Text(
                    text = "AI 日历",
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // View Mode Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFF1F5F9))
                    .clickable { viewMode = if (viewMode == "month") "week" else "month" }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (viewMode == "month") "收起月历" else "展开月历",
                    color = RobotTheme.colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Icon(
                    imageVector = if (viewMode == "month") Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = RobotTheme.colors.textPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Date Display & Week Number
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${selectedDate.get(Calendar.YEAR)}年",
                color = RobotTheme.colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = monthName,
                color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "第 $weekNumber 周",
                    color = RobotTheme.colors.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar Grid System
        Column(modifier = Modifier.fillMaxWidth()) {
            // Weekday labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { idx, dayName ->
                    val color = if (idx == 0 || idx == 6) {
                        if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)
                    } else {
                        RobotTheme.colors.textMuted
                    }
                    Box(
                        modifier = Modifier.width(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayName,
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Week view or Month view
            AnimatedVisibility(
                visible = viewMode == "month",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                // Month Grid
                val daysInMonth = selectedDate.getActualMaximum(Calendar.DAY_OF_MONTH)
                val tempCal = selectedDate.clone() as Calendar
                tempCal.set(Calendar.DAY_OF_MONTH, 1)
                val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1

                val totalCells = firstDayOfWeek + daysInMonth
                val rowsCount = (totalCells + 6) / 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (row in 0 until rowsCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                if (cellIndex < firstDayOfWeek || cellIndex >= totalCells) {
                                    Spacer(modifier = Modifier.size(32.dp))
                                } else {
                                    val dayNum = cellIndex - firstDayOfWeek + 1
                                    val cellDate = selectedDate.clone() as Calendar
                                    cellDate.set(Calendar.DAY_OF_MONTH, dayNum)

                                    CalendarDayCell(
                                        date = cellDate,
                                        isSelected = isSameDay(cellDate, selectedDate),
                                        isToday = isSameDay(cellDate, today),
                                        hasEvents = hasEventsForDate(cellDate),
                                        onClick = { selectedDate = cellDate }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = viewMode == "week",
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                // Week view row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDaysDates.forEach { cellDate ->
                        CalendarDayCell(
                            date = cellDate,
                            isSelected = isSameDay(cellDate, selectedDate),
                            isToday = isSameDay(cellDate, today),
                            hasEvents = hasEventsForDate(cellDate),
                            onClick = { selectedDate = cellDate }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected Date Details Section
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Details Header label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "当日安排",
                    color = RobotTheme.colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFFEEF2F6))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$relativeTimeStr · ${selectedDate.get(Calendar.MONTH) + 1}月${selectedDate.get(Calendar.DAY_OF_MONTH)}日",
                        color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // List of items scroll area
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
                        is BoardListItem.TodoItem -> {
                            val todo = item.todo
                            val isCompleted = todo.status == "closed"
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
                                // Clickable checkbox
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

                        is BoardListItem.ScheduleItem -> {
                            val schedule = item.schedule
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
                                }
                            }
                        }
                    }
                }

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "这一天还没有事务与日程。",
                            color = RobotTheme.colors.textMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Bottom status widget card
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lunar part
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Lunar calendar card
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.5.dp,
                            if (isDark) Color(0xFFD97706).copy(alpha = 0.3f) else Color(0xFFFCD34D),
                            RoundedCornerShape(12.dp)
                        )
                        .background(if (isDark) Color(0xFFD97706).copy(alpha = 0.1f) else Color(0xFFFFFBEB)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${selectedDate.get(Calendar.YEAR)}",
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "${selectedDate.get(Calendar.DAY_OF_MONTH)}",
                            color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "农历 ${dayInfo.lunarDate}",
                        color = RobotTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (dayInfo.festival != null) {
                                    if (isDark) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(0xFFFFE4E6)
                                } else {
                                    if (isDark) Color(0xFF475569) else Color(0xFFF1F5F9)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dayInfo.festival ?: "平日",
                            color = if (dayInfo.festival != null) {
                                if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48)
                            } else {
                                RobotTheme.colors.textMuted
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(32.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0))
            )

            // Weather part
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = dayInfo.weatherCondition,
                        color = RobotTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = dayInfo.weatherTemp,
                        color = RobotTheme.colors.textMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cloud,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: Calendar,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    onClick: () -> Unit
) {
    val isDark = RobotTheme.isDark
    val dayNum = date.get(Calendar.DAY_OF_MONTH)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.size(width = 32.dp, height = 36.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isToday -> {
                            if (isDark) Color(0xFF4F46E5) else Color(0xFF6366F1)
                        }
                        isSelected -> {
                            if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE0E7FF)
                        }
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = if (isSelected && !isToday) 1.dp else 0.dp,
                    color = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$dayNum",
                color = when {
                    isToday -> Color.White
                    isSelected -> {
                        if (isDark) Color.White else Color(0xFF4F46E5)
                    }
                    else -> RobotTheme.colors.textPrimary
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        if (hasEvents && !isToday) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1)
                        } else {
                            RobotTheme.colors.textMuted.copy(alpha = 0.5f)
                        }
                    )
            )
        } else {
            Spacer(modifier = Modifier.size(3.dp))
        }
    }
}

private sealed class BoardListItem {
    data class TodoItem(val todo: com.airobot.features.schedule.data.model.TodoItem) : BoardListItem()
    data class ScheduleItem(val schedule: com.airobot.features.schedule.data.model.ScheduleItem) : BoardListItem()
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

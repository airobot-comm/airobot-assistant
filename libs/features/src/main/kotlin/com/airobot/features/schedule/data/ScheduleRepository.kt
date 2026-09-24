package com.airobot.features.schedule.data

import com.airobot.features.schedule.data.model.ScheduleItem
import com.airobot.features.schedule.data.model.TodoItem

/**
 * Repository for loading and saving AI Schedules and Todos.
 */
interface ScheduleRepository {
    suspend fun loadSchedules(): List<ScheduleItem>
    suspend fun saveSchedules(list: List<ScheduleItem>)
    suspend fun loadTodos(): List<TodoItem>
    suspend fun saveTodos(list: List<TodoItem>)
}

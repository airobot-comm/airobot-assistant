package com.airobot.features.schedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airobot.features.aiserv.guidance.RemindEngine
import com.airobot.features.aiserv.guidance.data.RemindCard
import com.airobot.features.FeatureCards
import com.airobot.features.schedule.data.ScheduleRepository
import com.airobot.features.schedule.data.model.ScheduleItem
import com.airobot.features.schedule.data.model.TodoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val remindEngine: RemindEngine
) : ViewModel() {

    /** Schedule-specific remind cards filtered by schedule-related overlay tags. */
    val remindCards: Flow<List<RemindCard>> = remindEngine.getRemindCards(
        listOf(FeatureCards.SCHEDULE_PLANNER, FeatureCards.LOGBOOK, FeatureCards.FOCUS)
    )

    private val _schedules = MutableStateFlow<List<ScheduleItem>>(emptyList())
    val schedules: StateFlow<List<ScheduleItem>> = _schedules.asStateFlow()

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    private val _selectedItemType = MutableStateFlow<String?>(null) // "schedule" or "todo"
    val selectedItemType: StateFlow<String?> = _selectedItemType.asStateFlow()

    fun setSelectedItem(id: String?, type: String?) {
        _selectedItemId.value = id
        _selectedItemType.value = type
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _schedules.value = repository.loadSchedules()
            _todos.value = repository.loadTodos()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun checkDailyLimit(date: String?, dow: Int?): Boolean {
        var count = 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        _schedules.value.forEach { s ->
            if (date != null && s.date == date) {
                count++
            } else if (date == null && s.date == null && dow != null && s.dayOfWeek == dow) {
                count++
            } else if (date != null && s.date == null && s.dayOfWeek != null) {
                try {
                    val d = sdf.parse(date)
                    val cal = Calendar.getInstance()
                    if (d != null) {
                        cal.time = d
                        val dDow = cal.get(Calendar.DAY_OF_WEEK) - 1 // Calendar Sun=1, Mon=2 -> 0=Sun, 1=Mon
                        if (dDow == s.dayOfWeek) {
                            count++
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        _todos.value.forEach { t ->
            if (date != null && t.date == date) {
                count++
            }
        }

        if (count >= 5) {
            _errorMessage.value = "每日的日程事务、代办总数不能超过5条"
            return false
        }
        return true
    }

    fun addSchedule(task: String, time: String, dayOfWeek: Int, date: String?, category: String = "work"): Boolean {
        if (!checkDailyLimit(date, dayOfWeek)) return false
        val newItem = ScheduleItem(
            id = UUID.randomUUID().toString(),
            time = time,
            task = task,
            completed = false,
            category = category,
            dayOfWeek = dayOfWeek,
            date = date
        )
        viewModelScope.launch {
            val newList = _schedules.value + newItem
            _schedules.value = newList
            repository.saveSchedules(newList)
        }
        return true
    }

    fun toggleSchedule(id: String) {
        viewModelScope.launch {
            val newList = _schedules.value.map {
                if (it.id == id) it.copy(completed = !it.completed) else it
            }
            _schedules.value = newList
            repository.saveSchedules(newList)
        }
    }

    fun deleteSchedule(id: String) {
        viewModelScope.launch {
            val newList = _schedules.value.filter { it.id != id }
            _schedules.value = newList
            repository.saveSchedules(newList)
        }
    }

    fun addTodo(task: String, date: String?, time: String?): Boolean {
        if (!checkDailyLimit(date, null)) return false
        val newItem = TodoItem(
            id = UUID.randomUUID().toString(),
            task = task,
            status = "open",
            date = date,
            time = time,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val newList = listOf(newItem) + _todos.value
            _todos.value = newList
            repository.saveTodos(newList)
        }
        return true
    }

    fun toggleTodo(id: String) {
        viewModelScope.launch {
            val newList = _todos.value.map {
                if (it.id == id) it.copy(status = if (it.status == "open") "closed" else "open") else it
            }
            _todos.value = newList
            repository.saveTodos(newList)
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch {
            val newList = _todos.value.filter { it.id != id }
            _todos.value = newList
            repository.saveTodos(newList)
        }
    }
}

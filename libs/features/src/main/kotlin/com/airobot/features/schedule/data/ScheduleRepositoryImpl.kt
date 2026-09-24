package com.airobot.features.schedule.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airobot.features.schedule.data.model.ScheduleItem
import com.airobot.features.schedule.data.model.TodoItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "airobot_schedule_prefs")

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ScheduleRepository {

    private val dataStore = context.scheduleDataStore
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "ScheduleRepository"
        val KEY_SCHEDULES = stringPreferencesKey("schedules")
        val KEY_TODOS = stringPreferencesKey("todos")
    }

    private suspend inline fun <reified T> loadList(key: Preferences.Key<String>, defaultProvider: () -> List<T>): List<T> {
        val jsonString = dataStore.data.map { it[key] }.first()
        return if (jsonString.isNullOrEmpty()) {
            defaultProvider()
        } else {
            try {
                json.decodeFromString<List<T>>(jsonString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode list for key ${key.name}", e)
                defaultProvider()
            }
        }
    }

    private suspend inline fun <reified T> saveList(key: Preferences.Key<String>, list: List<T>) {
        try {
            val jsonString = json.encodeToString(list)
            dataStore.edit { prefs ->
                prefs[key] = jsonString
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save list for key ${key.name}", e)
        }
    }

    override suspend fun loadSchedules(): List<ScheduleItem> = loadList(KEY_SCHEDULES) {
        val calendar = Calendar.getInstance()
        val todayStr = String.format(Locale.US, "%d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        val currentDOW = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 1=Mon, ..., 6=Sat

        listOf(
            ScheduleItem(id = "1", time = "08:00", task = "晨读时光", completed = true, dayOfWeek = currentDOW, date = todayStr, category = "personal"),
            ScheduleItem(id = "2", time = "14:00", task = "AI 编程学习", completed = false, dayOfWeek = currentDOW, date = todayStr, category = "work"),
            ScheduleItem(id = "3", time = "19:00", task = "体能锻炼", completed = false, dayOfWeek = currentDOW, date = todayStr, category = "health"),
            ScheduleItem(id = "4", time = "10:00", task = "周一例会", completed = false, dayOfWeek = 1, category = "work"),
            ScheduleItem(id = "5", time = "15:00", task = "图书馆自习", completed = false, dayOfWeek = 3, category = "personal"),
            ScheduleItem(id = "6", time = "09:00", task = "周末大扫除", completed = false, dayOfWeek = 6, category = "personal")
        )
    }

    override suspend fun saveSchedules(list: List<ScheduleItem>) = saveList(KEY_SCHEDULES, list)

    override suspend fun loadTodos(): List<TodoItem> = loadList(KEY_TODOS) {
        val calendar = Calendar.getInstance()
        val todayStr = String.format(Locale.US, "%d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

        val yesterdayCalendar = Calendar.getInstance()
        yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = String.format(Locale.US, "%d-%02d-%02d", yesterdayCalendar.get(Calendar.YEAR), yesterdayCalendar.get(Calendar.MONTH) + 1, yesterdayCalendar.get(Calendar.DAY_OF_MONTH))

        listOf(
            TodoItem(id = "t1", task = "回复客户邮件", status = "open", date = todayStr, time = "10:00", createdAt = System.currentTimeMillis()),
            TodoItem(id = "t2", task = "购买办公用品", status = "closed", date = todayStr, time = "", createdAt = System.currentTimeMillis() - 3600000),
            TodoItem(id = "t3", task = "准备下周PPT", status = "open", date = yesterdayStr, time = "15:00", createdAt = System.currentTimeMillis() - 86400000)
        )
    }

    override suspend fun saveTodos(list: List<TodoItem>) = saveList(KEY_TODOS, list)
}

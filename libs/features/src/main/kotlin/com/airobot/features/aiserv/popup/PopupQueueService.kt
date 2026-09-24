package com.airobot.features.aiserv.popup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PopupQueueService @Inject constructor() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _queue = MutableStateFlow<List<PopupServiceItem>>(emptyList())

    // A flow that emits the current timestamp every 30 seconds
    private val timeTicker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30000L) // 30 seconds
        }
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.Eagerly,
        initialValue = System.currentTimeMillis()
    )

    /**
     * Exposes the filtered queue, only keeping items scheduled within the next 12 hours (or without event times/in the past).
     */
    val queue: StateFlow<List<PopupServiceItem>> = combine(_queue, timeTicker) { items, now ->
        items.filter { item ->
            val eventTime = item.nextEventTimeMs
            if (eventTime != null) {
                // Keep if scheduled within the next 12 hours, or in the past (e.g. ringing/completed)
                eventTime <= now + 12 * 60 * 60 * 1000L
            } else {
                true // No specific event time: always show (e.g. running timer/focus)
            }
        }
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * Exposes the top priority item that should be rendered.
     */
    val topQueueItem: StateFlow<PopupServiceItem?> = queue.map { items ->
        items.firstOrNull()
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /**
     * Exposes whether any item currently holds the foreground lock.
     */
    val hasForegroundLock: StateFlow<Boolean> = queue.map { items ->
        items.any { it.needsForegroundLock }
    }.stateIn(
        scope = serviceScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val timeoutJobs = mutableMapOf<String, Job>()

    /**
     * Enqueues or updates a popup item in the queue.
     * Items are sorted first by [needsForegroundLock] (true comes first), then by [priority] (higher is better).
     */
    fun enqueuePopup(item: PopupServiceItem) {
        val currentList = _queue.value.toMutableList()
        currentList.removeAll { it.id == item.id }
        currentList.add(item)

        sortQueue(currentList)
        _queue.value = currentList

        // Manage lifecycle timeout
        timeoutJobs[item.id]?.cancel()
        if (item.timeoutDurationMs > 0) {
            timeoutJobs[item.id] = serviceScope.launch {
                delay(item.timeoutDurationMs)
                item.onTimeout()
                removePopup(item.id)
            }
        }
    }

    /**
     * Removes a popup item by ID and cancels any pending timeout.
     */
    fun removePopup(id: String) {
        timeoutJobs[id]?.cancel()
        timeoutJobs.remove(id)

        val currentList = _queue.value.toMutableList()
        val removed = currentList.removeAll { it.id == id }
        if (removed) {
            sortQueue(currentList)
            _queue.value = currentList
        }
    }

    /**
     * Removes all popup items whose ID starts with the given prefix and cancels their timeouts.
     */
    fun removePopupsByPrefix(prefix: String) {
        val currentList = _queue.value.toMutableList()
        val toRemove = currentList.filter { it.id.startsWith(prefix) }
        if (toRemove.isNotEmpty()) {
            for (item in toRemove) {
                timeoutJobs[item.id]?.cancel()
                timeoutJobs.remove(item.id)
                currentList.remove(item)
            }
            sortQueue(currentList)
            _queue.value = currentList
        }
    }

    /**
     * Gets a filtered list of popup items by service type.
     */
    fun getPopupsByType(type: String): List<PopupServiceItem> {
        return queue.value.filter { it.serviceType == type }
    }

    private fun sortQueue(list: MutableList<PopupServiceItem>) {
        list.sortWith { a, b ->
            if (a.needsForegroundLock && !b.needsForegroundLock) -1
            else if (!a.needsForegroundLock && b.needsForegroundLock) 1
            else {
                val p = b.priority.compareTo(a.priority) // Descending order: higher priority first
                if (p != 0) p
                else {
                    val aTime = a.nextEventTimeMs ?: Long.MAX_VALUE
                    val bTime = b.nextEventTimeMs ?: Long.MAX_VALUE
                    val timeComp = aTime.compareTo(bTime) // Ascending order: earlier time first
                    if (timeComp != 0) timeComp
                    else {
                        // Stable fallback: sort by ID to prevent alternating swaps when equal
                        a.id.compareTo(b.id)
                    }
                }
            }
        }
    }
}

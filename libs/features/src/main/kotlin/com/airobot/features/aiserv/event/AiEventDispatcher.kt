package com.airobot.features.aiserv.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AiEventDispatcher — Reactive, internal event bus for AI-related operations.
 * Allows sub-modules (like clock) to dispatch logs without direct coupling to storage pipelines.
 */
@Singleton
class AiEventDispatcher @Inject constructor() {
    
    private val _events = MutableSharedFlow<AiEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<AiEvent> = _events.asSharedFlow()

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob())

    /**
     * Dispatch an event to all subscribers.
     */
    fun dispatch(event: AiEvent) {
        android.util.Log.d("AiEventDispatcher", "Dispatching event: ${event.javaClass.simpleName}")
        scope.launch {
            _events.emit(event)
        }
    }
}

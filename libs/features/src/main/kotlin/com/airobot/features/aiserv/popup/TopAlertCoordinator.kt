package com.airobot.features.aiserv.popup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TopAlertCoordinator — Hilt singleton to coordinate top banner alerts
 * across ViewModels and background services.
 */
@Singleton
class TopAlertCoordinator @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _state = MutableStateFlow(TopAlertState())
    val state: StateFlow<TopAlertState> = _state.asStateFlow()
    
    private var autoDismissJob: Job? = null

    /**
     * Shows a top banner alert that automatically dismisses after 3 seconds.
     */
    fun showAlert(message: String, type: AlertType = AlertType.WARNING) {
        autoDismissJob?.cancel()
        _state.value = TopAlertState(visible = true, message = message, type = type)
        autoDismissJob = scope.launch {
            delay(3000)
            _state.value = _state.value.copy(visible = false)
        }
    }

    /**
     * Immediately hides the active top alert.
     */
    fun hideAlert() {
        autoDismissJob?.cancel()
        _state.value = _state.value.copy(visible = false)
    }
}

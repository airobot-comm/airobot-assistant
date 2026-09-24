package com.airobot.airbot.api

import com.airobot.airbot.domain.model.RobotState
import com.airobot.airbot.viewmodel.RobotVisualState
import kotlinx.coroutines.flow.StateFlow

interface AirbotEngineApi {
    val robotState: StateFlow<RobotState>
    val idleVisualState: StateFlow<RobotVisualState>

    fun updateEngineState(newState: RobotState)
}

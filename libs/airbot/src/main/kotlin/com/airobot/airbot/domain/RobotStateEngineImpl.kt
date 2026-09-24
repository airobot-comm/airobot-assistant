package com.airobot.airbot.domain


import android.util.Log
import com.airobot.airbot.api.AirbotEngineApi
import com.airobot.airbot.domain.model.RobotState
import com.airobot.airbot.viewmodel.RobotVisualState
import com.airobot.core.comm.NetCommEvent
import com.airobot.core.comm.NetCommService
import com.airobot.core.comm.NetworkState
import com.airobot.core.system.SysManage
import com.airobot.core.system.SysState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized robot state engine — single source of truth for system-level state.
 * Also manages the idle visual state loop that automatically cycles the robot
 * through personality states (BORED, DAZING, DOZING, SLEEPING) when idle.
 */
@Singleton
class RobotStateEngineImpl @Inject constructor(
    private val sysManage: SysManage,
    private val netCommService: NetCommService
) : AirbotEngineApi {
    companion object {
        private const val TAG = "RobotStateEngine"
        private const val IDLE_INITIAL_DELAY_MIN = 5000L   // 5s before first idle transition
        private const val IDLE_INITIAL_DELAY_RANGE = 5000L // +0~5s randomness
        private const val IDLE_HOLD_DURATION_MIN = 8000L   // 8s hold each idle sub-state
        private const val IDLE_HOLD_DURATION_RANGE = 8000L // +0~8s randomness
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _robotState = MutableStateFlow<RobotState>(RobotState.Offline)
    override val robotState: StateFlow<RobotState> = _robotState.asStateFlow()

    /** Visual state driven by the idle-loop; only meaningful when engine is Ready */
    private val _idleVisualState = MutableStateFlow(RobotVisualState.IDLE)
    override val idleVisualState: StateFlow<RobotVisualState> = _idleVisualState.asStateFlow()

    private var idleLoopJob: Job? = null

    init {
        observeSysState()
        observeNetwork()
    }

    private fun observeSysState() {
        scope.launch {
            sysManage.state.collect { state ->
                when (state) {
                    is SysState.Checking -> {
                        updateEngineState(RobotState.Initializing)
                    }

                    is SysState.DeviceActivationRequired -> {
                        updateEngineState(RobotState.Unauthorized("DEVICE_ACTIVATION"))
                    }

                    is SysState.AiRobotActivationRequired -> {
                        updateEngineState(RobotState.Unauthorized(state.code))
                    }

                    is SysState.Ready -> {
                        // The app shell invokes connect(), we just transition states.
                        // We do not overwrite Connecting or Ready if it happens to be so.
                    }

                    is SysState.UpdateAvailable -> {
                        updateEngineState(RobotState.Ready)
                    }

                    is SysState.Error -> {
                        updateEngineState(RobotState.Offline)
                    }

                    is SysState.Idle -> {}
                }
            }
        }
    }

    private fun observeNetwork() {
        scope.launch {
            netCommService.state.collect { state ->
                when (state) {
                    NetworkState.CONNECTING, NetworkState.RECONNECTING -> {
                        updateEngineState(RobotState.Connecting)
                    }

                    NetworkState.CONNECTED -> {
                        // Handled via event
                    }

                    NetworkState.ERROR, NetworkState.IDLE -> {
                        val current = _robotState.value
                        if (current !is RobotState.Unauthorized && current !is RobotState.Initializing) {
                            updateEngineState(RobotState.Offline)
                        }
                    }
                }
            }
        }

        scope.launch {
            netCommService.events.collect { event ->
                when (event) {
                    is NetCommEvent.Connected -> {
                        if (_robotState.value !is RobotState.Conversation) {
                            updateEngineState(RobotState.Ready)
                        }
                    }

                    is NetCommEvent.Disconnected -> {
                        updateEngineState(RobotState.Offline)
                    }

                    is NetCommEvent.Error -> {}
                    else -> {}
                }
            }
        }
    }

    override fun updateEngineState(newState: RobotState) {
        val prev = _robotState.value
        _robotState.value = newState
        Log.d(TAG, "Engine state: $prev -> $newState")

        // Manage idle loop lifecycle based on engine state
        if (newState is RobotState.Ready) {
            startIdleLoop()
        } else {
            stopIdleLoop()
        }
    }

    /**
     * Start the idle personality loop.
     * Cycles: IDLE -> random(BORED/DAZING/DOZING/SLEEPING) -> IDLE -> ...
     */
    private fun startIdleLoop() {
        if (idleLoopJob?.isActive == true) return
        Log.d(TAG, "Starting idle loop")

        idleLoopJob = scope.launch {
            try {
                while (isActive) {
                    // Phase 1: Stay in IDLE (ready) for 5-10s
                    _idleVisualState.value = RobotVisualState.IDLE
                    val readyDuration = IDLE_INITIAL_DELAY_MIN +
                        (Math.random() * IDLE_INITIAL_DELAY_RANGE).toLong()
                    delay(readyDuration)

                    // Phase 2: Transition to a random idle sub-state
                    val idleStates = listOf(
                        RobotVisualState.BORED,
                        RobotVisualState.DAZING,
                        RobotVisualState.DOZING,
                        RobotVisualState.SLEEPING
                    )
                    val nextState = idleStates.random()
                    _idleVisualState.value = nextState
                    Log.d(TAG, "Idle loop: transitioned to $nextState")

                    // Phase 3: Hold for 8-16s
                    val holdDuration = IDLE_HOLD_DURATION_MIN +
                        (Math.random() * IDLE_HOLD_DURATION_RANGE).toLong()
                    delay(holdDuration)
                }
            } catch (e: CancellationException) {
                // Normal cancellation when leaving Ready state
            }
        }
    }

    private fun stopIdleLoop() {
        if (idleLoopJob?.isActive == true) {
            Log.d(TAG, "Stopping idle loop")
            idleLoopJob?.cancel()
            idleLoopJob = null
            _idleVisualState.value = RobotVisualState.IDLE
        }
    }
}


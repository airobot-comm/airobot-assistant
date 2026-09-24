package com.airobot.airbot.character.rive

import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.PlayableInstance
import com.airobot.airbot.domain.model.RiveCharacterConfig
import com.airobot.airbot.viewmodel.RobotVisualState

private const val TAG = "RiveCharacter"

/**
 * Rive character component. Renders the Rive animation for selected IP characters.
 * Maps [RobotVisualState] and audio levels to Rive state machine inputs.
 * Uses a try-catch block to gracefully fallback if the state machine/inputs do not exist in the placeholder.
 */
@Composable
fun RiveCharacter(
    state: RobotVisualState,
    roleName: String?,
    audioLevel: () -> Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = remember(roleName) {
        RiveCharacterConfig.getCharacterConfig(context, roleName)
    }

    // Wrap state variables to prevent closure capturing outdated states in AndroidView factory
    val currentVisualState by rememberUpdatedState(state)
    val currentAudioLevel by rememberUpdatedState(audioLevel)

    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var riveViewRef: RiveAnimationView? by remember { mutableStateOf(null) }

    // Random blinking loop
    LaunchedEffect(state, roleName) {
        if (state != RobotVisualState.SLEEPING && state != RobotVisualState.DOZING) {
            while (true) {
                // Random delay between 3 and 7 seconds
                val delayMs = (3000..7000).random().toLong()
                kotlinx.coroutines.delay(delayMs)
                val view = riveViewRef
                if (view != null) {
                    Log.v(TAG, "Random blink triggered. Firing 'tgBlink'")
                    fireRiveTrigger(view, "tgBlink", roleName)
                }
            }
        }
    }

    key(roleName) {
        AndroidView(
            modifier = modifier
                .offset(x = config.offsetX.dp, y = config.offsetY.dp)
                .scale(config.scale)
                .onSizeChanged { viewSize = it }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val view = riveViewRef ?: return@detectTapGestures
                        // Distinguish head click (top 45%) and body touch (bottom 55%)
                        val isHead = offset.y < viewSize.height * 0.45f
                        val triggerName = if (isHead) "tgtouchHead" else "tgtouchBody"
                        Log.d(TAG, "Rive character touched at $offset. Detected as ${if (isHead) "HEAD" else "BODY"}. Firing trigger '$triggerName'")
                        fireRiveTrigger(view, triggerName, roleName)
                    }
                },
            factory = { context ->
                Log.d(TAG, "Initializing RiveAnimationView for $roleName")
                RiveAnimationView(context).apply {
                    riveViewRef = this
                    setTag(CharacterStateHolder())
                    try {
                        val resId = RiveCharacterConfig.getResourceForRole(context, roleName)
                        // Load Rive file from raw resources explicitly loading 'State Machine 1'
                        setRiveResource(
                            resId = resId,
                            stateMachineName = "State Machine 1", // Explicitly bind state machine to enable visualState/mouthOpen inputs
                            fit = Fit.CONTAIN,
                            alignment = Alignment.CENTER,
                            autoplay = true
                        )
                        // Add listener to sync inputs immediately when Rive starts playing
                        registerListener(object : RiveFileController.Listener {
                            override fun notifyPlay(animation: PlayableInstance) {
                                Log.d(TAG, "Rive state machine started playing: ${animation.name}")
                                logAvailableRiveProperties(this@apply)
                                updateRiveInputs(this@apply, currentVisualState, currentAudioLevel(), roleName)
                            }
                            override fun notifyPause(animation: PlayableInstance) {}
                            override fun notifyStop(animation: PlayableInstance) {}
                            override fun notifyLoop(animation: PlayableInstance) {}
                            override fun notifyStateChanged(stateMachineName: String, stateName: String) {}
                        })
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading Rive resource: ${e.message}", e)
                    }
                }
            },
            update = { view ->
                riveViewRef = view
                updateRiveInputs(view, state, audioLevel(), roleName)
            }
        )
    }
}

/**
 * Dynamically updates Rive inputs on the active state machine or properties via ViewModel based on the current visual state and audio level.
 */
private fun isXinXiaoLing(roleName: String?): Boolean {
    return roleName?.equals("心小灵", ignoreCase = true) == true ||
            roleName?.equals("xin_xiao_ling", ignoreCase = true) == true
}

private fun logAvailableRiveProperties(view: RiveAnimationView) {
    try {
        val stateMachine = view.controller.stateMachines.firstOrNull() ?: return
        Log.d(TAG, "Rive State Machine Name: ${stateMachine.name}")
        val inputs = stateMachine.inputs.map { it.name }
        Log.d(TAG, "Available State Machine Inputs: $inputs")

        val vmi = stateMachine.viewModelInstance
        if (vmi != null) {
            Log.d(TAG, "Active ViewModelInstance name: ${vmi.name}")
            val knownProps = listOf("visualState", "mouthOpen", "tgBlink", "tgHappy", "tgtouchHead", "tgtouchBody")
            val availableProps = mutableListOf<String>()
            for (propName in knownProps) {
                try {
                    vmi.getNumberProperty(propName)
                    availableProps.add("$propName (Number)")
                } catch (e: Throwable) {
                    try {
                        val triggerProp = vmi.getTriggerProperty(propName)
                        if (triggerProp != null) {
                            availableProps.add("$propName (Trigger)")
                        }
                    } catch (ex: Throwable) {
                        // Not found
                    }
                }
            }
            Log.d(TAG, "Checked ViewModel properties: $availableProps")
        } else {
            Log.d(TAG, "No active ViewModelInstance bound to State Machine")
        }
    } catch (e: Throwable) {
        Log.w(TAG, "Failed to log Rive properties: ${e.message}")
    }
}

/**
 * Dynamically updates Rive inputs on the active state machine or properties via ViewModel based on the current visual state and audio level.
 */
private fun updateRiveInputs(
    view: RiveAnimationView,
    state: RobotVisualState,
    level: Float,
    roleName: String?
) {
    val stateNum = when (state) {
        RobotVisualState.IDLE -> 0f
        RobotVisualState.LISTENING -> 1f
        RobotVisualState.THINKING -> 2f
        RobotVisualState.SPEAKING -> 3f
        RobotVisualState.HAPPY -> 4f
        RobotVisualState.FOCUS -> 5f
        RobotVisualState.SLEEPING -> 6f
        RobotVisualState.WORKING -> 5f // WORKING maps to FOCUS (5f)
        RobotVisualState.BORED -> 0f   // BORED maps to IDLE (0f)
        RobotVisualState.DAZING -> 0f  // DAZING maps to IDLE (0f)
        RobotVisualState.DOZING -> 6f  // DOZING maps to SLEEPING (6f)
    }

    val holder = view.getTag() as? CharacterStateHolder ?: CharacterStateHolder().also { view.setTag(it) }

    // Only allow mouth opening during SPEAKING state
    val targetMouthOpen = if (state == RobotVisualState.SPEAKING) {
        // Restrict maximum mouth opening amplitude to 60f to prevent distortion
        (level * 60f).coerceIn(0f, 60f)
    } else {
        0f
    }

    // Apply EMA low-pass filter to smooth mouth animations and filter out high-frequency noise
    val smoothMouthOpen = if (state == RobotVisualState.SPEAKING) {
        val alpha = 0.2f
        val smoothed = holder.lastSmoothMouthOpen * (1f - alpha) + targetMouthOpen * alpha
        holder.lastSmoothMouthOpen = smoothed
        smoothed
    } else {
        holder.lastSmoothMouthOpen = 0f
        0f
    }

    // Prevent redundant property updates if the values did not actually change
    if (holder.lastSetStateNum == stateNum && holder.lastSetSmoothMouthOpen == smoothMouthOpen) {
        return
    }
    holder.lastSetStateNum = stateNum
    holder.lastSetSmoothMouthOpen = smoothMouthOpen

    // Check if state changed, trigger corresponding ViewModel trigger properties if needed
    val lastState = holder.lastVisualState
    val stateMachine = view.controller.stateMachines.firstOrNull()

    if (lastState != state) {
        holder.lastVisualState = state

        // When state changes to HAPPY, fire the tgHappy trigger property
        if (state == RobotVisualState.HAPPY) {
            var triggerSuccess = false

            // Only search ViewModel if it is "心小灵"
            if (isXinXiaoLing(roleName)) {
                val vmi = stateMachine?.viewModelInstance
                // Try active ViewModelInstance trigger first
                if (vmi != null) {
                    try {
                        val prop = vmi.getTriggerProperty("tgHappy")
                        if (prop != null) {
                            prop.trigger()
                            Log.v(TAG, "Successfully fired 'tgHappy' trigger property on active ViewModelInstance")
                            triggerSuccess = true
                        }
                    } catch (e: Throwable) {
                        // Suppress
                    }
                }

                // Fallback to manual ViewModel binding if vmi is null
                if (!triggerSuccess) {
                    val file = view.controller.file
                    if (file != null) {
                        val viewModels = listOf("ViewModel", "ViewModel1")
                        for (vmName in viewModels) {
                            try {
                                val vm = file.getViewModelByName(vmName)
                                val instance = vm.createDefaultInstance()
                                stateMachine?.viewModelInstance = instance
                                val prop = instance.getTriggerProperty("tgHappy")
                                if (prop != null) {
                                    prop.trigger()
                                    Log.v(TAG, "Successfully fired 'tgHappy' on manually created ViewModelInstance: $vmName")
                                    triggerSuccess = true
                                    break
                                }
                            } catch (e: Throwable) {
                                // Suppress
                            }
                        }
                    }
                }
            }

            // Fallback to legacy State Machine trigger input
            if (!triggerSuccess && stateMachine != null) {
                try {
                    val inputNames = stateMachine.inputs.map { it.name }
                    if (inputNames.contains("tgHappy")) {
                        view.fireState(stateMachine.name, "tgHappy")
                        Log.v(TAG, "Successfully fired 'tgHappy' trigger on State Machine fallback")
                    } else {
                        Log.v(TAG, "State Machine does not have 'tgHappy' input trigger")
                    }
                } catch (e: Throwable) {
                    // Suppress
                }
            }
        }
    }

    var success = false

    // Try setting properties using Rive's ViewModel Data Binding first (preferred by designers)
    if (isXinXiaoLing(roleName)) {
        val vmi = stateMachine?.viewModelInstance
        if (vmi != null) {
            try {
                vmi.getNumberProperty("visualState").value = stateNum
                vmi.getNumberProperty("mouthOpen").value = smoothMouthOpen
                success = true
                Log.v(TAG, "Successfully updated Rive properties on active ViewModelInstance (visualState=$stateNum, mouthOpen=$smoothMouthOpen)")
            } catch (e: Throwable) {
                // Suppress and try fallback
            }
        }

        if (!success) {
            val file = view.controller.file
            if (file != null) {
                val viewModelsToTry = listOf("ViewModel", "ViewModel1")
                for (vmName in viewModelsToTry) {
                    try {
                        val vm = file.getViewModelByName(vmName)
                        val instance = vm.createDefaultInstance()
                        stateMachine?.viewModelInstance = instance
                        instance.getNumberProperty("visualState").value = stateNum
                        instance.getNumberProperty("mouthOpen").value = smoothMouthOpen
                        success = true
                        Log.v(TAG, "Successfully created, bound and updated Rive properties on ViewModel: $vmName (state=$stateNum, mouthOpen=$smoothMouthOpen)")
                        break
                    } catch (e: Throwable) {
                        // Suppress
                    }
                }
            }
        }
    }

    if (!success) {
        // Fallback to standard State Machine Inputs
        Log.v(TAG, "ViewModel properties not found or failed, falling back to State Machine Inputs")
        val activeStateMachineName = stateMachine?.name
        if (stateMachine != null && activeStateMachineName != null) {
            try {
                val inputNames = stateMachine.inputs.map { it.name }

                if (inputNames.contains("visualState")) {
                    view.setNumberState(activeStateMachineName, "visualState", stateNum)
                } else if (inputNames.contains("state")) {
                    view.setNumberState(activeStateMachineName, "state", stateNum)
                }

                if (inputNames.contains("mouthOpen")) {
                    view.setNumberState(activeStateMachineName, "mouthOpen", smoothMouthOpen)
                } else if (inputNames.contains("audioLevel")) {
                    view.setNumberState(activeStateMachineName, "audioLevel", smoothMouthOpen)
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Failed to update Rive state machine inputs on '$activeStateMachineName': ${e.message}"
                )
            }
        } else {
            Log.v(TAG, "No active Rive state machine found yet to update inputs")
        }
    }
}

/**
 * Holder class to store stateful properties attached to RiveAnimationView.
 */
private class CharacterStateHolder(
    var lastVisualState: RobotVisualState? = null,
    var lastSmoothMouthOpen: Float = 0f,
    var lastSetStateNum: Float? = null,
    var lastSetSmoothMouthOpen: Float? = null
)

/**
 * Dynamically fires a trigger property or fallback state machine input on the active Rive animation view.
 */
private fun fireRiveTrigger(view: RiveAnimationView, triggerName: String, roleName: String?) {
    val stateMachine = view.controller.stateMachines.firstOrNull() ?: return
    val activeStateMachineName = stateMachine.name ?: return

    var fired = false

    // 1. Try State Machine input trigger first (safe name check)
    val inputNames = try {
        stateMachine.inputs.map { it.name }
    } catch (e: Throwable) {
        emptyList()
    }

    if (inputNames.contains(triggerName)) {
        try {
            view.fireState(activeStateMachineName, triggerName)
            Log.d(TAG, "Successfully fired '$triggerName' trigger on State Machine: $activeStateMachineName")
            fired = true
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to fire state machine trigger '$triggerName': ${e.message}")
        }
    }

    // 2. Try ViewModel property trigger only for "心小灵"
    if (isXinXiaoLing(roleName)) {
        val vmi = stateMachine.viewModelInstance

        if (vmi != null) {
            try {
                val prop = vmi.getTriggerProperty(triggerName)
                if (prop != null) {
                    prop.trigger()
                    Log.d(TAG, "Successfully fired '$triggerName' trigger property on active ViewModelInstance")
                    fired = true
                }
            } catch (e: Throwable) {
                // Suppress
            }
        }

        if (!fired) {
            val file = view.controller.file
            if (file != null) {
                val viewModels = listOf("ViewModel", "ViewModel1")
                for (vmName in viewModels) {
                    try {
                        val vm = file.getViewModelByName(vmName)
                        val instance = vm.createDefaultInstance()
                        stateMachine.viewModelInstance = instance
                        val prop = instance.getTriggerProperty(triggerName)
                        if (prop != null) {
                            prop.trigger()
                            Log.d(TAG, "Successfully fired '$triggerName' on manually created ViewModelInstance: $vmName")
                            fired = true
                            break
                        }
                    } catch (e: Throwable) {
                        // Suppress
                    }
                }
            }
        }
    }

    if (!fired) {
        Log.w(TAG, "Touch/Blink trigger '$triggerName' is not supported for role $roleName")
    }
}



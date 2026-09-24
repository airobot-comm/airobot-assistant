package com.airobot.framework.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/**
 * InactivityTimeoutWrapper — A generic framework-level layout component that monitors
 * child pointer inputs and triggers a callback after a specified period of inactivity.
 */
@Composable
fun InactivityTimeoutWrapper(
    modifier: Modifier = Modifier,
    timeoutMs: Long = 60000L,
    enabled: Boolean = true,
    onTimeout: () -> Unit,
    content: @Composable () -> Unit
) {
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(enabled, lastInteractionTime) {
        if (enabled) {
            delay(timeoutMs)
            onTimeout()
        }
    }

    Box(
        modifier = modifier.pointerInput(enabled) {
            if (enabled) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        lastInteractionTime = System.currentTimeMillis()
                    }
                }
            }
        }
    ) {
        content()
    }
}

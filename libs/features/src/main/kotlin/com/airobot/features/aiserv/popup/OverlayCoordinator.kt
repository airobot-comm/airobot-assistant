package com.airobot.features.aiserv.popup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OverlayCoordinator — Hilt singleton to coordinate full-screen overlays
 * across ViewModels and background services.
 * Now generic and decoupled from hardcoded enums.
 */
@Singleton
class OverlayCoordinator @Inject constructor() {
    private val _activeOverlay = MutableStateFlow("")
    val activeOverlay: StateFlow<String> = _activeOverlay.asStateFlow()

    fun showOverlay(tag: String) {
        _activeOverlay.value = tag
    }

    fun hideOverlay() {
        _activeOverlay.value = ""
    }
}

package com.airobot.features.aiserv.viewmodel

import androidx.lifecycle.ViewModel
import com.airobot.features.aiserv.popup.AlertType
import com.airobot.features.aiserv.popup.OverlayCoordinator
import com.airobot.features.aiserv.popup.TopAlertCoordinator
import com.airobot.features.aiserv.popup.TopAlertState
import com.airobot.features.aiserv.guidance.CardRecommendEngine
import com.airobot.features.aiserv.guidance.data.RecommendCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * OverlayViewModel — lightweight Compose DI gateway for global overlay dispatch and alerts.
 */
@HiltViewModel
class OverlayViewModel @Inject constructor(
    private val overlayCoordinator: OverlayCoordinator,
    private val topAlertCoordinator: TopAlertCoordinator,
    private val cardRecommendEngine: CardRecommendEngine
) : ViewModel() {
    val activeOverlay: StateFlow<String> = overlayCoordinator.activeOverlay
    val topAlert: StateFlow<TopAlertState> = topAlertCoordinator.state

    fun showOverlay(tag: String) {
        overlayCoordinator.showOverlay(tag)
    }

    fun hideOverlay() {
        val currentTag = activeOverlay.value
        if (currentTag.isNotEmpty()) {
            cardRecommendEngine.recordUsage(currentTag)
        }
        overlayCoordinator.hideOverlay()
    }

    fun getRecommendCards(): Flow<List<RecommendCard>> {
        return cardRecommendEngine.getRecommendCards()
    }

    /**
     * Shows a top banner alert that automatically dismisses after 3 seconds.
     */
    fun showTopAlert(message: String, type: AlertType = AlertType.WARNING) {
        topAlertCoordinator.showAlert(message, type)
    }

    /**
     * Immediately hides the active top alert.
     */
    fun hideTopAlert() {
        topAlertCoordinator.hideAlert()
    }
}


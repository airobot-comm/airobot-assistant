package com.airobot.features.aiserv.popup

/**
 * AlertType — severity types for the custom top alert banner
 */
enum class AlertType {
    INFO, WARNING, ERROR
}

/**
 * TopAlertState — holds alert visibility, message, and severity
 */
data class TopAlertState(
    val visible: Boolean = false,
    val message: String = "",
    val type: AlertType = AlertType.WARNING
)

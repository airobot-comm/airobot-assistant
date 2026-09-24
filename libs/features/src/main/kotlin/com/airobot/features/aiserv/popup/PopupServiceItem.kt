package com.airobot.features.aiserv.popup

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Represents a generic popup card service item that can be managed by the [PopupQueueService].
 * Any feature module can implement this interface and push items to the queue.
 */
abstract class PopupServiceItem {
    /** Unique identifier for this popup instance */
    abstract val id: String

    /** Service type for categorization (e.g., "ALARM", "TIMER") */
    abstract val serviceType: String

    /** Short display name for this popup instance (max 4 characters) */
    abstract val displayName: String

    /**
     * Priority of the popup. Higher value means higher priority.
     * e.g., Alarm = 100, Focus Timer = 80, Normal Timer = 50, Chime = 10
     */
    abstract val priority: Int

    /**
     * Auto-dismiss timeout in milliseconds.
     * 0 or negative means the popup will stay indefinitely until dismissed by the user.
     * Default can be e.g., 60000 (60 seconds)
     */
    abstract val timeoutDurationMs: Long

    /**
     * The timestamp (in milliseconds) of the next expected event for this item.
     * Used as a secondary sorting key when priorities are equal (earlier time comes first).
     */
    abstract val nextEventTimeMs: Long?

    /**
     * Whether this popup needs to lock the foreground.
     * If true, this popup will preempt others and cannot be easily dismissed by generic actions.
     */
    abstract val needsForegroundLock: Boolean

    /** Callback invoked when the user manually dismisses the popup */
    abstract val onDismiss: () -> Unit

    /** Callback invoked if the popup expires due to timeoutDurationMs */
    abstract val onTimeout: () -> Unit

    /** True if this item should be rendered full-screen immediately. False if it is just a scheduled background item for the sub-dial. */
    abstract val isFullScreen: Boolean

    /** Sub-dial title (e.g. "Alarm", "Timer") */
    abstract val subDialTitle: String

    /** Sub-dial value (e.g. "08:30", "12:00") */
    abstract val subDialValue: String

    /** Sub-dial icon */
    abstract val subDialIcon: ImageVector?

    /** Sub-dial custom composable icon, used if subDialIcon is null */
    open val subDialCustomIcon: (@Composable () -> Unit)? = null

    /** Sub-dial accent color */
    @Composable
    abstract fun getSubDialColor(): Color

    /** Callback when the sub-dial is clicked */
    abstract val subDialOnClick: () -> Unit

    /**
     * The Compose UI content for this popup.
     * It allows the queue to be completely type-agnostic regarding the UI.
     */
    @Composable
    abstract fun Content()
}

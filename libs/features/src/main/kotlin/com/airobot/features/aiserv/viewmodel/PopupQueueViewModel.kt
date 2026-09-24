package com.airobot.features.aiserv.viewmodel

import androidx.lifecycle.ViewModel
import com.airobot.features.aiserv.popup.PopupQueueService
import com.airobot.features.aiserv.popup.PopupServiceItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * PopupQueueViewModel — Compose DI bridge ViewModel for PopupQueueService.
 * Kept in viewmodel package to decouple business views from core singleton services.
 */
@HiltViewModel
class PopupQueueViewModel @Inject constructor(
    private val popupQueueService: PopupQueueService
) : ViewModel() {
    val queue: StateFlow<List<PopupServiceItem>> = popupQueueService.queue
    val topQueueItem: StateFlow<PopupServiceItem?> = popupQueueService.topQueueItem
}

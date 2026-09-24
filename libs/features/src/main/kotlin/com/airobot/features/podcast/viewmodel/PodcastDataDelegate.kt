package com.airobot.features.podcast.viewmodel

import android.util.Log
import com.airobot.features.aiserv.event.AiEvent
import com.airobot.features.aiserv.event.AiEventDispatcher
import com.airobot.features.podcast.data.PodcastRepository
import com.airobot.features.podcast.data.PodcastSystemPresets
import com.airobot.features.podcast.data.model.PodcastEpisode
import com.airobot.features.podcast.data.model.PodcastSubscription
import com.airobot.features.podcast.data.model.QnaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import javax.inject.Singleton

/**
 * Delegate responsible for managing podcast dataset state, CRUD mutations,
 * and dispatching favoriting and subscription AI Notepad events.
 */
@Singleton
class PodcastDataDelegate @Inject constructor(
    private val repository: PodcastRepository,
    private val eventDispatcher: AiEventDispatcher
) {
    companion object {
        private const val TAG = "PodcastDataDelegate"
    }

    private val _episodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val episodes: StateFlow<List<PodcastEpisode>> = _episodes.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<PodcastSubscription>>(emptyList())
    val subscriptions: StateFlow<List<PodcastSubscription>> = _subscriptions.asStateFlow()

    private val delegateScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        loadFromRepository()
    }

    private fun loadFromRepository() {
        delegateScope.launch {
            try {
                // Parse date string to createdAt for backward compatibility
                val rawEpisodes = repository.loadEpisodes()
                val episodes = rawEpisodes.map { ep ->
                    if (ep.createdAt == 0L && ep.date.isNotEmpty()) {
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val parsedDate = sdf.parse(ep.date)
                            ep.copy(createdAt = parsedDate?.time ?: 0L)
                        } catch (e: Exception) {
                            ep
                        }
                    } else ep
                }
                _episodes.value = episodes
                Log.d(TAG, "Loaded ${episodes.size} episodes from repository")

                val subscriptions = repository.loadSubscriptions()
                val missingSubs = PodcastSystemPresets.SUBSCRIPTIONS.filter { preset ->
                    subscriptions.none { it.id == preset.id }
                }
                val finalSubscriptions = if (missingSubs.isNotEmpty()) {
                    val merged = subscriptions + missingSubs
                    repository.saveSubscriptions(merged)
                    merged
                } else {
                    subscriptions
                }
                _subscriptions.value = finalSubscriptions
                Log.d(TAG, "Loaded ${finalSubscriptions.size} subscriptions from repository")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load podcast data from repository", e)
            }
        }
    }

    fun toggleFavorite(id: String) {
        _episodes.value = _episodes.value.map { ep ->
            if (ep.id == id) {
                val newFav = !ep.favorite
                eventDispatcher.dispatch(
                    AiEvent.PodcastEpisodeFavorited(
                        episodeId = ep.id,
                        title = ep.title,
                        favorite = newFav,
                        timestamp = System.currentTimeMillis()
                    )
                )
                ep.copy(favorite = newFav)
            } else ep
        }
        delegateScope.launch { repository.saveEpisodes(_episodes.value) }
    }

    fun toggleSubscription(id: String) {
        _subscriptions.value = _subscriptions.value.map { sub ->
            if (sub.id == id) {
                val currentSubscribedCount = _subscriptions.value.count { it.isSubscribed }
                if (!sub.isSubscribed && currentSubscribedCount >= 5) {
                    sub
                } else {
                    val newSub = !sub.isSubscribed
                    eventDispatcher.dispatch(
                        AiEvent.PodcastSubscriptionChanged(
                            channelId = sub.id,
                            channelTitle = sub.title,
                            isSubscribed = newSub,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                    sub.copy(isSubscribed = newSub)
                }
            } else {
                sub
            }
        }
        delegateScope.launch { repository.saveSubscriptions(_subscriptions.value) }
    }

    fun addQnA(id: String, question: String, answer: String) {
        _episodes.value = _episodes.value.map { ep ->
            if (ep.id == id) {
                val newQna = ep.qnaHistory + QnaItem(question, answer)
                ep.copy(qnaHistory = newQna)
            } else ep
        }
        delegateScope.launch { repository.saveEpisodes(_episodes.value) }
    }

    fun updateEpisodesState(updatedList: List<PodcastEpisode>) {
        _episodes.value = updatedList
    }

    fun updateSubscriptionsState(updatedList: List<PodcastSubscription>) {
        _subscriptions.value = updatedList
    }
}

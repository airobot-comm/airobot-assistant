package com.airobot.features.aiserv.guidance

import com.airobot.features.aiserv.guidance.data.RemindCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemindEngine @Inject constructor(
    private val cardRankingEngine: CardRankingEngine
) {
    companion object {
        const val MAX_CARDS = 3
    }

    // Registry for remind models mapped to their tags
    private val remindRegistry = mutableMapOf<String, RemindCard>()

    init {
        // Seed default reminds
        RemindCard.defaultCards.forEach { card ->
            remindRegistry[card.tag] = card
        }
    }

    fun registerRemindCard(model: RemindCard) {
        remindRegistry[model.tag] = model
    }

    fun updateRemindContent(tag: String, title: String? = null, content: String? = null, actionTarget: String? = null) {
        val existing = remindRegistry[tag]
        if (existing != null) {
            remindRegistry[tag] = existing.copy(
                title = title ?: existing.title,
                content = content ?: existing.content,
                actionTarget = actionTarget ?: existing.actionTarget
            )
        }
    }

    fun getRemindCards(tags: List<String>): Flow<List<RemindCard>> {
        return cardRankingEngine.getRankedTags(tags).map { sortedTags ->
            sortedTags.mapNotNull { remindRegistry[it] }.take(MAX_CARDS)
        }
    }
}

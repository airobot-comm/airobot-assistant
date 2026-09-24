package com.airobot.features.aiserv.guidance

import com.airobot.features.FeatureCards
import com.airobot.features.FeatureCardType
import com.airobot.features.aiserv.guidance.data.CardUsageRecord
import com.airobot.features.aiserv.guidance.data.CardUsageStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRankingEngine @Inject constructor(
    private val usageStore: CardUsageStore
) {
    private val _supportedTags = MutableStateFlow<List<String>>(emptyList())
    private val _updateTrigger = MutableStateFlow(0)

    fun setSupportedTags(tags: List<String>) {
        _supportedTags.value = tags
    }

    /** App-level query: returns tags filtered by the globally configured supported tags. */
    fun getRankedTags(): Flow<List<String>> = combine(_supportedTags, _updateTrigger) { tags, _ ->
        getRankedTagsInternal(tags)
    }

    /** Module-level query: returns tags filtered by the given tag subset. */
    fun getRankedTags(tags: List<String>): Flow<List<String>> = _updateTrigger.map {
        getRankedTagsInternal(tags)
    }

    private fun getRankedTagsInternal(tags: List<String>): List<String> {
        val records = usageStore.getAllRecords(tags)

        // Calculate median usage within this subset
        val usageCounts = records.values.map { it.usageCount }.sorted()
        val medianUsage = if (usageCounts.isNotEmpty()) {
            usageCounts[usageCounts.size / 2]
        } else {
            0
        }

        val now = System.currentTimeMillis()

        val sortedTags = tags.sortedWith { tagA, tagB ->
            val scoreA = computeScore(tagA, records[tagA] ?: CardUsageRecord(), medianUsage, now)
            val scoreB = computeScore(tagB, records[tagB] ?: CardUsageRecord(), medianUsage, now)

            val p = scoreB.compareTo(scoreA) // descending by score
            if (p != 0) p
            else tagA.compareTo(tagB) // stable fallback
        }

        return sortedTags
    }

    private fun computeScore(tag: String, record: CardUsageRecord, medianUsage: Int, now: Long): Int {
        val cardMeta = FeatureCards.get(tag)
        var score = cardMeta?.basePriority ?: 50

        if (cardMeta != null) {
            when (cardMeta.type) {
                FeatureCardType.POPUP -> score += 10
                FeatureCardType.STATIC -> score -= 5
            }
        }

        if (record.hasNewContent) {
            score += 30
        }

        if (now - record.lastUsedTimestamp < 5 * 60 * 1000L) { // 5 minutes
            score -= 20
        }

        if (record.usageCount < medianUsage) {
            score += 10
        }

        return score
    }

    fun recordUsage(tag: String) {
        val current = usageStore.getUsageRecord(tag)
        usageStore.saveUsageRecord(tag, current.copy(
            usageCount = current.usageCount + 1,
            lastUsedTimestamp = System.currentTimeMillis(),
            hasNewContent = false
        ))
        _updateTrigger.value = _updateTrigger.value + 1
    }

    fun markNewContent(tag: String, hasNew: Boolean) {
        val current = usageStore.getUsageRecord(tag)
        usageStore.saveUsageRecord(tag, current.copy(
            hasNewContent = hasNew
        ))
        _updateTrigger.value = _updateTrigger.value + 1
    }
}

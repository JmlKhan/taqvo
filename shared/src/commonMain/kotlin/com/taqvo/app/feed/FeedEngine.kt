package com.taqvo.app.feed

import com.taqvo.app.content.ContentCatalog
import com.taqvo.app.content.ContentItem
import com.taqvo.app.content.ItemLength
import kotlin.random.Random

/** The feed's numbers (feed spec §4, content spec §5), in one place so the tests and the screen agree. */
object FeedRules {
    const val BATCH = 20
    const val REFILL_BELOW = 5
    const val PRELOAD = 3
    /** Four in five slots follow the reader's weights; the fifth explores. */
    const val WEIGHTED_SHARE = 0.8
    /** An item is not shown again for this many days, relaxed step by step when nothing is left. */
    val NO_REPEAT_DAYS = listOf(14, 7, 3, 0)
    const val SEEN_AFTER_MS = 1_500L
    const val SKIP_UNDER_MS = 1_500L
    /** Fast swiping as the feed opens is not a judgement on the cards (feed spec §4). */
    const val SKIP_GRACE_CARDS = 10
    /** A short item is this many times likelier to be drawn for a reader who asked for short ayahs. */
    const val SHORT_PREFERENCE = 3.0
    const val DAY_MS = 86_400_000L
}

/**
 * Picks what the feed shows next (content spec §5). Pure: the catalog, the weights and the history
 * come in, item ids go out, and [random] is the only thing that varies, so the tests fix it.
 */
class FeedEngine(private val random: Random = Random.Default) {

    /**
     * The next [size] items. [lastSeen] is each item's latest view in epoch millis; [exclude] is what
     * is already queued, which this batch never repeats. Never shorter than [size] while the catalog
     * still holds something not excluded: the no-repeat window relaxes from 14 days to 7, 3 and none
     * (feed spec §10) before the feed is ever left empty.
     */
    fun batch(
        catalog: ContentCatalog,
        weights: Map<String, Double>,
        lastSeen: Map<String, Long>,
        nowMillis: Long,
        exclude: Set<String> = emptySet(),
        preferShort: Boolean = false,
        size: Int = FeedRules.BATCH,
    ): List<String> {
        val taken = exclude.toMutableSet()
        val picked = mutableListOf<String>()
        repeat(size) {
            val candidates = candidates(catalog, lastSeen, nowMillis, taken) ?: return picked
            val item = if (random.nextDouble() < FeedRules.WEIGHTED_SHARE) {
                weightedPick(catalog, candidates, weights, preferShort)
            } else {
                // Exploration: any topic, the reader's weights set aside.
                candidates.random(random)
            }
            picked += item.id
            taken += item.id
        }
        return picked
    }

    /**
     * The first card of the day (feed spec §4): a short item from the reader's strongest topic not
     * seen lately, falling back through the topics by weight, then to any short item, then any item.
     */
    fun cardOfDay(
        catalog: ContentCatalog,
        weights: Map<String, Double>,
        lastSeen: Map<String, Long>,
        nowMillis: Long,
    ): ContentItem? {
        val fresh = candidates(catalog, lastSeen, nowMillis, emptySet()) ?: return null
        val freshIds = fresh.mapTo(HashSet()) { it.id }
        val byWeight = catalog.topics.sortedByDescending { weights[it] ?: TopicWeights.START }
        for (topic in byWeight) {
            val shorts = catalog.byTopic[topic].orEmpty().filter { it.id in freshIds && it.length == ItemLength.SHORT }
            if (shorts.isNotEmpty()) return shorts.random(random)
        }
        return fresh.filter { it.length == ItemLength.SHORT }.ifEmpty { fresh }.random(random)
    }

    /** What may be drawn now under the strictest no-repeat window that leaves anything; null when nothing can. */
    private fun candidates(
        catalog: ContentCatalog,
        lastSeen: Map<String, Long>,
        nowMillis: Long,
        taken: Set<String>,
    ): List<ContentItem>? {
        val open = catalog.items.filter { it.id !in taken }
        if (open.isEmpty()) return null
        for (days in FeedRules.NO_REPEAT_DAYS) {
            val cutoff = nowMillis - days * FeedRules.DAY_MS
            val allowed = if (days == 0) open else open.filter { item -> (lastSeen[item.id] ?: Long.MIN_VALUE) < cutoff }
            if (allowed.isNotEmpty()) return allowed
        }
        return open
    }

    /**
     * A topic drawn by weight among the topics that still have a candidate — which is how a topic
     * that has run out passes its turn to the others — then an item from it.
     */
    private fun weightedPick(
        catalog: ContentCatalog,
        candidates: List<ContentItem>,
        weights: Map<String, Double>,
        preferShort: Boolean,
    ): ContentItem {
        val ids = candidates.mapTo(HashSet()) { it.id }
        val open = catalog.topics.mapNotNull { topic ->
            val items = catalog.byTopic[topic].orEmpty().filter { it.id in ids }
            if (items.isEmpty()) null else Triple(topic, weights[topic] ?: TopicWeights.START, items)
        }
        if (open.isEmpty()) return candidates.random(random)
        val (_, _, items) = drawWeighted(open) { it.second }
        return drawWeighted(items) { if (preferShort && it.length == ItemLength.SHORT) FeedRules.SHORT_PREFERENCE else 1.0 }
    }

    private fun <T> drawWeighted(options: List<T>, weightOf: (T) -> Double): T {
        val total = options.sumOf { weightOf(it).coerceAtLeast(0.0) }
        if (total <= 0.0) return options.random(random)
        var point = random.nextDouble() * total
        for (option in options) {
            point -= weightOf(option).coerceAtLeast(0.0)
            if (point < 0) return option
        }
        return options.last()
    }
}

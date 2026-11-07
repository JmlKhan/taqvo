package com.taqvo.app.feed

import com.taqvo.app.feature.onboarding.Goal

/**
 * How much the reader wants each topic (feed content spec §4–§5): every topic starts at
 * [START], onboarding's answers add to it, and every like and quick skip after that nudges it,
 * always kept between [MIN] and [MAX] so no topic is ever shut out or takes over the feed.
 */
object TopicWeights {
    const val START = 1.0
    const val MIN = 0.2
    const val MAX = 5.0
    const val LIKE = 0.3
    const val SKIP = -0.1

    /** The onboarding content answer that asks for short ayahs rather than a topic. */
    const val SHORT_AYAHS = "short_ayahs"

    /** Content spec §4: what each answer adds, as (+2.0 topics, +1.0 topics). */
    private val BOOSTS: Map<String, Pair<List<String>, List<String>>> = mapOf(
        Goal.REMEMBER to (listOf("zikr") to listOf("asmaul_husna")),
        Goal.PEACE to (listOf("sabr", "rahmat") to listOf("zikr")),
        Goal.HARDSHIP to (listOf("sabr", "tavakkul") to listOf("rahmat")),
        Goal.IMAN to (listOf("iymon", "asmaul_husna") to listOf("oxirat")),
        Goal.HABIT to (listOf("ibodat") to listOf("zikr")),
        "comfort" to (listOf("rahmat", "sabr") to listOf("tavakkul")),
        "gratitude" to (listOf("shukr") to listOf("tabiat")),
        "patience" to (listOf("sabr") to listOf("tavakkul")),
        "reflection" to (listOf("ilm", "tabiat") to listOf("oxirat")),
    )

    /**
     * The weights a reader starts the feed with. A skipped onboarding, or answers that name no
     * topic ("mix", "short ayahs"), leave every topic at [START] (feed spec §10).
     */
    fun initial(topics: List<String>, goals: List<String>, contentTypes: List<String>): Map<String, Double> {
        val weights = topics.associateWith { START }.toMutableMap()
        (goals + contentTypes).distinct().forEach { answer ->
            val (strong, mild) = BOOSTS[answer] ?: return@forEach
            strong.forEach { topic -> weights.computeIfPresent(topic) { _, w -> w + 2.0 } }
            mild.forEach { topic -> weights.computeIfPresent(topic) { _, w -> w + 1.0 } }
        }
        return weights.mapValues { (_, w) -> w.coerceIn(MIN, MAX) }
    }

    fun prefersShort(contentTypes: List<String>): Boolean = SHORT_AYAHS in contentTypes

    /** [weights] with [delta] added to each of [topics], clamped. Topics not yet weighed start at [START]. */
    fun adjust(weights: Map<String, Double>, topics: Collection<String>, delta: Double): Map<String, Double> {
        val next = weights.toMutableMap()
        topics.forEach { topic -> next[topic] = ((next[topic] ?: START) + delta).coerceIn(MIN, MAX) }
        return next
    }

    /**
     * [stored] completed for every topic in [topics]: a topic added by a newer content.db starts at
     * [START] rather than at nothing, and a topic it dropped is forgotten.
     */
    fun complete(stored: Map<String, Double>, topics: List<String>): Map<String, Double> =
        topics.associateWith { (stored[it] ?: START).coerceIn(MIN, MAX) }

    private inline fun <K, V> MutableMap<K, V>.computeIfPresent(key: K, block: (K, V) -> V) {
        val current = this[key] ?: return
        this[key] = block(key, current)
    }
}

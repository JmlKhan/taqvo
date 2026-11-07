package com.taqvo.app.feed

import com.taqvo.app.content.ContentCatalog
import com.taqvo.app.content.ItemLength
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FeedEngineTest {
    private val catalog = fakeCatalog()
    private val now = 100 * FeedRules.DAY_MS
    private val even = mapOf("sabr" to 1.0, "zikr" to 1.0, "shukr" to 1.0)

    private fun topicOf(id: String) = id.substringBefore(':')

    @Test fun aBatchIsFullAndHasNoRepeats() {
        val batch = FeedEngine(Random(1)).batch(catalog, even, emptyMap(), now)
        assertEquals(FeedRules.BATCH, batch.size)
        assertEquals(batch.size, batch.toSet().size)
    }

    @Test fun nothingAlreadyQueuedIsDrawnAgain() {
        val queued = catalog.items.take(85).map { it.id }.toSet()
        val batch = FeedEngine(Random(2)).batch(catalog, even, emptyMap(), now, exclude = queued)
        assertEquals(5, batch.size)
        assertTrue(batch.none { it in queued })
    }

    @Test fun theWeightsSteerAboutFourInFiveSlots() {
        val heavy = mapOf("sabr" to 5.0, "zikr" to 0.2, "shukr" to 0.2)
        // A catalog far larger than the draw, so no topic runs out and skews the share.
        val big = fakeCatalog(perTopic = 6000)
        val draws = FeedEngine(Random(3)).batch(big, heavy, emptyMap(), now, size = 2000).map(::topicOf)
        val sabr = draws.count { it == "sabr" }.toDouble() / draws.size
        // 80% weighted at 5/5.4 ≈ 74%, plus 20% exploring at a third ≈ 7%: about 81%.
        assertTrue(sabr in 0.76..0.86, "sabr share $sabr")
    }

    @Test fun exploringStillReachesTheLightestTopic() {
        val heavy = mapOf("sabr" to 5.0, "zikr" to 0.2, "shukr" to 0.2)
        val big = fakeCatalog(perTopic = 6000)
        val draws = FeedEngine(Random(4)).batch(big, heavy, emptyMap(), now, size = 2000).map(::topicOf)
        val shukr = draws.count { it == "shukr" }.toDouble() / draws.size
        assertTrue(shukr > 0.06, "shukr share $shukr")
    }

    @Test fun anItemSeenInTheLastTwoWeeksIsNotShown() {
        val seen = catalog.items.drop(10).associate { it.id to now - 13 * FeedRules.DAY_MS }
        val batch = FeedEngine(Random(5)).batch(catalog, even, seen, now)
        // The ten unseen come first; only once they are gone does the window relax for the rest.
        assertEquals(catalog.items.take(10).map { it.id }.toSet(), batch.take(10).toSet())
        assertEquals(FeedRules.BATCH, batch.size)
    }

    @Test fun anItemSeenLongerAgoIsShownAgain() {
        val seen = catalog.items.associate { it.id to now - 15 * FeedRules.DAY_MS }
        assertEquals(FeedRules.BATCH, FeedEngine(Random(6)).batch(catalog, even, seen, now).size)
    }

    @Test fun whenEverythingWasSeenTheWindowRelaxesToAWeek() {
        // Half seen ten days ago, half two days ago: the 14-day window leaves nothing, the 7-day one the first half.
        val seen = catalog.items.withIndex().associate { (i, item) ->
            item.id to now - (if (i % 2 == 0) 10 else 2) * FeedRules.DAY_MS
        }
        val batch = FeedEngine(Random(7)).batch(catalog, even, seen, now)
        assertEquals(FeedRules.BATCH, batch.size)
        assertTrue(batch.all { seen.getValue(it) == now - 10 * FeedRules.DAY_MS })
    }

    @Test fun whenEverythingWasSeenTodayTheFeedIsStillNeverEmpty() {
        val seen = catalog.items.associate { it.id to now - 1000 }
        assertEquals(FeedRules.BATCH, FeedEngine(Random(8)).batch(catalog, even, seen, now).size)
    }

    @Test fun aTopicThatRunsOutPassesItsTurnOn() {
        val heavy = mapOf("sabr" to 5.0, "zikr" to 1.0, "shukr" to 1.0)
        val sabrSeen = catalog.byTopic.getValue("sabr").associate { it.id to now - FeedRules.DAY_MS }
        val batch = FeedEngine(Random(9)).batch(catalog, heavy, sabrSeen, now)
        assertEquals(FeedRules.BATCH, batch.size)
        assertTrue(batch.none { topicOf(it) == "sabr" })
    }

    @Test fun preferringShortDrawsMoreShortItems() {
        val big = fakeCatalog(perTopic = 3000)
        fun shortShare(prefer: Boolean): Double {
            // About 0.8 × 0.6 + 0.2 × 1/3 ≈ 0.55 short when preferred, a third when not.
            val draws = FeedEngine(Random(10)).batch(big, even, emptyMap(), now, preferShort = prefer, size = 600)
            return draws.count { big.byId.getValue(it).length == ItemLength.SHORT }.toDouble() / draws.size
        }
        assertTrue(shortShare(true) > shortShare(false) + 0.15)
    }

    @Test fun theCardOfTheDayIsShortAndFromTheStrongestTopic() {
        val weights = mapOf("sabr" to 1.0, "zikr" to 4.0, "shukr" to 1.0)
        repeat(20) { seed ->
            val card = assertNotNull(FeedEngine(Random(seed)).cardOfDay(catalog, weights, emptyMap(), now))
            assertEquals("zikr", card.primaryTopic)
            assertEquals(ItemLength.SHORT, card.length)
        }
    }

    @Test fun theCardOfTheDayFallsBackWhenTheStrongestTopicHasNoFreshShortItem() {
        val weights = mapOf("sabr" to 1.0, "zikr" to 4.0, "shukr" to 2.0)
        val zikrShortsSeen = catalog.byTopic.getValue("zikr").filter { it.length == ItemLength.SHORT }
            .associate { it.id to now - FeedRules.DAY_MS }
        val card = assertNotNull(FeedEngine(Random(11)).cardOfDay(catalog, weights, zikrShortsSeen, now))
        assertEquals("shukr", card.primaryTopic)
    }

    @Test fun anEmptyCatalogGivesNothing() {
        val empty = ContentCatalog(emptyList(), emptyList())
        assertTrue(FeedEngine(Random(12)).batch(empty, emptyMap(), emptyMap(), now).isEmpty())
        assertEquals(null, FeedEngine(Random(12)).cardOfDay(empty, emptyMap(), emptyMap(), now))
    }
}

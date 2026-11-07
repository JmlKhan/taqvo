package com.taqvo.app.feed

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import com.taqvo.app.feed.db.FeedDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** app.db's queries against a real SQLite, created from the schema exactly as a phone creates it. */
class SqlFeedStoreTest {
    private val store = SqlFeedStore(
        driverFactory = { JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { FeedDatabase.Schema.create(it) } },
        io = Dispatchers.Unconfined,
    )
    private val monday = LocalDate(2026, 11, 2)
    private val tuesday = LocalDate(2026, 11, 3)

    @Test fun weightsAreSavedAndReplaced() = runTest {
        store.saveWeights(mapOf("sabr" to 1.0, "zikr" to 2.0))
        store.saveWeights(mapOf("sabr" to 1.3))
        assertEquals(mapOf("sabr" to 1.3, "zikr" to 2.0), store.weights())
    }

    @Test fun lastSeenIsEachItemsLatestViewInTheWindow() = runTest {
        store.markSeen("q:1:1", 100, monday)
        store.markSeen("q:1:1", 300, monday)
        store.markSeen("q:2:2", 50, monday)
        assertEquals(mapOf("q:1:1" to 300L), store.lastSeen(sinceMillis = 60))
    }

    @Test fun pruningForgetsOnlyOldViews() = runTest {
        store.markSeen("q:1:1", 100, monday)
        store.markSeen("q:2:2", 500, tuesday)
        store.pruneSeen(beforeMillis = 200)
        assertEquals(setOf("q:2:2"), store.lastSeen(0).keys)
    }

    @Test fun favoritesAreNewestFirstAndLikedOnce() = runTest {
        store.addFavorite("a", 1)
        store.addFavorite("b", 2)
        store.addFavorite("a", 3)
        assertEquals(listOf("b", "a"), store.favorites())
        store.removeFavorite("b")
        assertEquals(listOf("a"), store.favorites())
    }

    @Test fun aDaysActivityCountsDistinctViewsAndLikesOnce() = runTest {
        store.markSeen("a", 1, monday)
        store.markSeen("a", 2, monday)
        store.markSeen("b", 3, monday)
        store.countLike(monday, "a")
        store.countLike(monday, "a")
        store.countLike(tuesday, "c")
        assertEquals(
            mapOf(monday to DayActivity(views = 2, likes = 1), tuesday to DayActivity(views = 0, likes = 1)),
            store.activitySince(monday),
        )
        assertEquals(setOf(tuesday), store.activitySince(tuesday).keys)
    }

    @Test fun theFeedStateRoundTripsAndIsReplaced() = runTest {
        assertNull(store.feedState())
        store.saveFeedState(FeedState(monday, listOf("a", "b"), 1, "a"))
        store.saveFeedState(FeedState(tuesday, listOf("c"), 0, null))
        assertEquals(FeedState(tuesday, listOf("c"), 0, null), store.feedState())
    }

    @Test fun collectionsHoldItemsAndGoWithThem() = runTest {
        store.createCollection("c1", "Morning", 1)
        store.createCollection("c2", "Evening", 2)
        store.addToCollection("c1", "a", 10)
        store.addToCollection("c1", "b", 20)
        store.addToCollection("c1", "a", 30)
        assertEquals(listOf("b", "a"), store.collectionItems("c1"))
        assertEquals(listOf(2, 0), store.collections().map { it.size })
        store.renameCollection("c2", "Night")
        assertEquals(listOf("Morning", "Night"), store.collections().map { it.name })
        store.removeFromCollection("c1", "b")
        store.deleteCollection("c1")
        assertEquals(listOf("c2"), store.collections().map { it.id })
        assertTrue(store.collectionItems("c1").isEmpty())
    }

    @Test fun everyWriteMovesTheRevision() = runTest {
        val before = store.revision.value
        store.addFavorite("a", 1)
        store.createCollection("c", "x", 1)
        assertEquals(before + 2, store.revision.value)
    }
}

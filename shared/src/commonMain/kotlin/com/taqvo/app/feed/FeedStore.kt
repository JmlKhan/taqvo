package com.taqvo.app.feed

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import com.taqvo.app.feed.db.FeedDatabase
import com.taqvo.app.quran.ioDispatcher

/** One saved collection (feed spec §5), with how many items it holds. */
data class FeedCollection(val id: String, val name: String, val createdAt: Long, val size: Int)

/** Where today's feed was left (feed spec §4): resumed on the same day, replaced on the next. */
data class FeedState(val day: LocalDate, val queue: List<String>, val position: Int, val cardOfDay: String?)

/**
 * The reader's own feed data, on the phone only: what they have seen, liked and collected, and
 * what the feed has learnt they like. An interface so the view model's tests run on a fake.
 *
 * [revision] moves on every write, so a screen showing favorites or collections reloads when
 * another screen changes them.
 */
interface FeedStore {
    val revision: StateFlow<Long>

    suspend fun weights(): Map<String, Double>
    suspend fun saveWeights(weights: Map<String, Double>)

    /** Each item's latest view at or after [sinceMillis]. */
    suspend fun lastSeen(sinceMillis: Long): Map<String, Long>
    suspend fun markSeen(itemId: String, atMillis: Long, day: LocalDate)

    /** Forgets views older than [beforeMillis]; the no-repeat window never looks further back. */
    suspend fun pruneSeen(beforeMillis: Long)

    suspend fun favorites(): List<String>
    suspend fun addFavorite(itemId: String, atMillis: Long)
    suspend fun removeFavorite(itemId: String)

    /** Counts [itemId] toward [day]'s goal, once however often it is liked again. */
    suspend fun countLike(day: LocalDate, itemId: String)

    /** Views and likes per day, from [since] on. Days with neither are absent. */
    suspend fun activitySince(since: LocalDate): Map<LocalDate, DayActivity>

    suspend fun feedState(): FeedState?
    suspend fun saveFeedState(state: FeedState)

    suspend fun collections(): List<FeedCollection>
    suspend fun createCollection(id: String, name: String, atMillis: Long)
    suspend fun renameCollection(id: String, name: String)
    suspend fun deleteCollection(id: String)
    suspend fun addToCollection(collectionId: String, itemId: String, atMillis: Long)
    suspend fun removeFromCollection(collectionId: String, itemId: String)
    suspend fun collectionItems(collectionId: String): List<String>
}

/** Opens app.db, creating it on first use. Never replaced by an update, unlike content.db. */
expect fun createFeedDriver(): SqlDriver

object FeedDb {
    const val FILE = "app.db"
}

class SqlFeedStore(
    driverFactory: () -> SqlDriver = ::createFeedDriver,
    private val io: CoroutineDispatcher = ioDispatcher(),
) : FeedStore {
    private val db: FeedDatabase by lazy { FeedDatabase(driverFactory()) }
    private val q get() = db.feedQueries
    private val _revision = MutableStateFlow(0L)
    override val revision: StateFlow<Long> = _revision.asStateFlow()

    private suspend fun <T> read(block: () -> T): T = withContext(io) { block() }

    private suspend fun write(block: () -> Unit) {
        withContext(io) { db.transaction { block() } }
        _revision.update { it + 1 }
    }

    override suspend fun weights(): Map<String, Double> = read {
        q.weights().executeAsList().associate { it.topic_id to it.weight }
    }

    override suspend fun saveWeights(weights: Map<String, Double>) = write {
        weights.forEach { (topic, weight) -> q.upsertWeight(topic, weight) }
    }

    override suspend fun lastSeen(sinceMillis: Long): Map<String, Long> = read {
        q.lastSeen(sinceMillis).executeAsList().mapNotNull { row -> row.last?.let { row.item_id to it } }.toMap()
    }

    // Views are not a revision: nothing on screen lists them, and the feed writes one per card.
    override suspend fun markSeen(itemId: String, atMillis: Long, day: LocalDate) {
        withContext(io) { q.insertSeen(itemId, atMillis, day.toString()) }
    }

    override suspend fun pruneSeen(beforeMillis: Long) {
        withContext(io) { q.pruneSeen(beforeMillis) }
    }

    override suspend fun favorites(): List<String> = read { q.favorites().executeAsList() }

    override suspend fun addFavorite(itemId: String, atMillis: Long) = write { q.addFavorite(itemId, atMillis) }

    override suspend fun removeFavorite(itemId: String) = write { q.removeFavorite(itemId) }

    override suspend fun countLike(day: LocalDate, itemId: String) = write { q.addDayLike(day.toString(), itemId) }

    override suspend fun activitySince(since: LocalDate): Map<LocalDate, DayActivity> = read {
        val views = q.viewDaysSince(since.toString()).executeAsList().associate { it.day to it.views.toInt() }
        val likes = q.likeDaysSince(since.toString()).executeAsList().associate { it.day to it.likes.toInt() }
        (views.keys + likes.keys).mapNotNull { day ->
            runCatching { LocalDate.parse(day) }.getOrNull()?.let { it to DayActivity(views[day] ?: 0, likes[day] ?: 0) }
        }.toMap()
    }

    override suspend fun feedState(): FeedState? = read {
        q.feedState().executeAsOneOrNull()?.let { row ->
            val day = runCatching { LocalDate.parse(row.day) }.getOrNull() ?: return@let null
            FeedState(day, row.queue.split(QUEUE_SEPARATOR).filter { it.isNotEmpty() }, row.position.toInt(), row.card_of_day)
        }
    }

    // Not a revision either: the feed saves its place on every swipe.
    override suspend fun saveFeedState(state: FeedState) {
        withContext(io) {
            q.saveFeedState(state.day.toString(), state.queue.joinToString(QUEUE_SEPARATOR), state.position.toLong(), state.cardOfDay)
        }
    }

    override suspend fun collections(): List<FeedCollection> = read {
        q.collections().executeAsList().map { FeedCollection(it.id, it.name, it.created_at, it.size.toInt()) }
    }

    override suspend fun createCollection(id: String, name: String, atMillis: Long) = write { q.insertCollection(id, name, atMillis) }

    override suspend fun renameCollection(id: String, name: String) = write { q.renameCollection(name, id) }

    override suspend fun deleteCollection(id: String) = write {
        q.deleteCollectionItems(id)
        q.deleteCollection(id)
    }

    override suspend fun addToCollection(collectionId: String, itemId: String, atMillis: Long) =
        write { q.addToCollection(collectionId, itemId, atMillis) }

    override suspend fun removeFromCollection(collectionId: String, itemId: String) =
        write { q.removeFromCollection(collectionId, itemId) }

    override suspend fun collectionItems(collectionId: String): List<String> = read {
        q.collectionItems(collectionId).executeAsList()
    }

    private companion object {
        const val QUEUE_SEPARATOR = "|"
    }
}

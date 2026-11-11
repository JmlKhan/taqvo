package com.taqvo.app.feed

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import com.taqvo.app.analytics.Analytics
import com.taqvo.app.content.ContentCatalog
import com.taqvo.app.content.ContentItem
import com.taqvo.app.content.ContentLang
import com.taqvo.app.content.ContentSource
import com.taqvo.app.content.ContentSourceInfo
import com.taqvo.app.content.ItemLength

/** A catalog of [perTopic] items in each of [topics], item "t:n" in topic t only, every third one short. */
fun fakeCatalog(topics: List<String> = listOf("sabr", "zikr", "shukr"), perTopic: Int = 30): ContentCatalog {
    var surah = 0
    val items = topics.flatMap { topic ->
        surah++
        (1..perTopic).map { n ->
            ContentItem(
                id = "$topic:$n",
                type = "ayah",
                surah = surah,
                ayah = n,
                arabic = "ar $topic $n",
                length = if (n % 3 == 0) ItemLength.SHORT else ItemLength.MEDIUM,
                primaryTopic = topic,
                topics = listOf(topic),
            )
        }
    }
    return ContentCatalog(items, topics)
}

class FakeContent(private val catalog: ContentCatalog = fakeCatalog(), private val emptyLang: ContentLang? = null) : ContentSource {
    var fail = false
    override suspend fun catalog(): ContentCatalog = if (fail) error("unreadable") else catalog
    override suspend fun texts(lang: ContentLang): Map<String, String> =
        if (lang == emptyLang) emptyMap() else catalog.items.associate { it.id to "${lang.tag} ${it.id}" }
    override suspend fun topicLabels(lang: ContentLang): Map<String, String> = catalog.topics.associateWith { it }
    override suspend fun sources(): List<ContentSourceInfo> = emptyList()
}

class FakeFeedStore : FeedStore {
    override val revision: StateFlow<Long> = MutableStateFlow(0L)
    var weights = mutableMapOf<String, Double>()
    val seen = mutableListOf<Triple<String, Long, LocalDate>>()
    val favorites = mutableListOf<Pair<String, Long>>()
    val dayLikes = mutableSetOf<Pair<LocalDate, String>>()
    var state: FeedState? = null
    val collections = mutableListOf<FeedCollection>()
    val collectionItems = mutableMapOf<String, MutableList<String>>()

    override suspend fun weights() = weights.toMap()
    override suspend fun saveWeights(weights: Map<String, Double>) { this.weights.putAll(weights) }
    override suspend fun lastSeen(sinceMillis: Long) =
        seen.filter { it.second >= sinceMillis }.groupBy { it.first }.mapValues { (_, v) -> v.maxOf { it.second } }
    override suspend fun markSeen(itemId: String, atMillis: Long, day: LocalDate) { seen += Triple(itemId, atMillis, day) }
    override suspend fun pruneSeen(beforeMillis: Long) { seen.removeAll { it.second < beforeMillis } }
    override suspend fun favorites() = favorites.sortedByDescending { it.second }.map { it.first }
    override suspend fun addFavorite(itemId: String, atMillis: Long) {
        if (favorites.none { it.first == itemId }) favorites += itemId to atMillis
    }
    override suspend fun removeFavorite(itemId: String) { favorites.removeAll { it.first == itemId } }
    override suspend fun countLike(day: LocalDate, itemId: String) { dayLikes += day to itemId }
    override suspend fun activitySince(since: LocalDate): Map<LocalDate, DayActivity> {
        val views = seen.filter { it.third >= since }.groupBy { it.third }.mapValues { (_, v) -> v.map { it.first }.distinct().size }
        val likes = dayLikes.filter { it.first >= since }.groupBy { it.first }.mapValues { (_, v) -> v.size }
        return (views.keys + likes.keys).associateWith { DayActivity(views[it] ?: 0, likes[it] ?: 0) }
    }
    override suspend fun feedState() = state
    override suspend fun saveFeedState(state: FeedState) { this.state = state }
    override suspend fun collections() = collections.map { it.copy(size = collectionItems[it.id]?.size ?: 0) }
    override suspend fun createCollection(id: String, name: String, atMillis: Long) { collections += FeedCollection(id, name, atMillis, 0) }
    override suspend fun renameCollection(id: String, name: String) {
        collections.replaceAll { if (it.id == id) it.copy(name = name) else it }
    }
    override suspend fun deleteCollection(id: String) { collections.removeAll { it.id == id }; collectionItems.remove(id) }
    override suspend fun addToCollection(collectionId: String, itemId: String, atMillis: Long) {
        val list = collectionItems.getOrPut(collectionId) { mutableListOf() }
        if (itemId !in list) list.add(0, itemId)
    }
    override suspend fun removeFromCollection(collectionId: String, itemId: String) { collectionItems[collectionId]?.remove(itemId) }
    override suspend fun collectionItems(collectionId: String) = collectionItems[collectionId].orEmpty().toList()
}

class FakeFeedPrefs : FeedPrefs {
    var streak: Streak? = null
    var firstOpen: LocalDate? = null
    var hintSeen = false
    var coachSeen = false
    var celebrated: LocalDate? = null
    override suspend fun streak() = streak
    override suspend fun setStreak(streak: Streak) { this.streak = streak }
    override suspend fun firstOpenDay(today: LocalDate) = firstOpen ?: today.also { firstOpen = it }
    override suspend fun swipeHintSeen() = hintSeen
    override suspend fun setSwipeHintSeen() { hintSeen = true }
    override suspend fun coachSeen() = coachSeen
    override suspend fun setCoachSeen() { coachSeen = true }
    override suspend fun celebratedOn() = celebrated
    override suspend fun setCelebratedOn(day: LocalDate) { celebrated = day }
}

class RecordingAnalytics : Analytics {
    val events = mutableListOf<Pair<String, Map<String, Any?>>>()
    override fun track(event: String, properties: Map<String, Any?>) { events += event to properties }
    fun names() = events.map { it.first }
}

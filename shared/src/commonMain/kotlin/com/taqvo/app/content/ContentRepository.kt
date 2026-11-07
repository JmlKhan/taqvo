package com.taqvo.app.content

import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.taqvo.app.content.db.ContentDatabase
import com.taqvo.app.quran.ioDispatcher

object ContentDb {
    /**
     * Must equal content.db's `user_version`; a mismatch on disk triggers a fresh copy. Raise it
     * with every rebuilt content.db (build_db.py sets the pragma), or phones keep the old file.
     */
    const val VERSION = 1
    const val FILE = "content.db"
    const val RESOURCE = "files/content.db"
}

/** Opens the bundled content.db, copying it out of the resources first when it is missing or stale. */
expect fun createContentDriver(): SqlDriver

/** What the feed reads from content.db. An interface so the feed's tests run on a fake catalog. */
interface ContentSource {
    suspend fun catalog(): ContentCatalog

    /** Every item's translation in [lang], by item id. */
    suspend fun texts(lang: ContentLang): Map<String, String>

    /** Each topic's label in [lang], by topic id. */
    suspend fun topicLabels(lang: ContentLang): Map<String, String>

    suspend fun sources(): List<ContentSourceInfo>
}

/**
 * Read-only access to content.db. Everything is read once and kept: the whole file's text in one
 * language is a few hundred kilobytes, and the feed asks for it on every card.
 */
class ContentRepository(
    driverFactory: () -> SqlDriver = ::createContentDriver,
    private val io: CoroutineDispatcher = ioDispatcher(),
) : ContentSource {
    private val db: ContentDatabase by lazy { ContentDatabase(driverFactory()) }
    private val q get() = db.contentQueries
    private val lock = Mutex()
    private var catalog: ContentCatalog? = null
    private val texts = mutableMapOf<ContentLang, Map<String, String>>()

    override suspend fun catalog(): ContentCatalog = lock.withLock {
        catalog ?: withContext(io) {
            val topicsOf = q.itemTopics().executeAsList()
                .groupBy({ it.item_id.orEmpty() }, { it.topic_id.orEmpty() })
            val items = q.items().executeAsList().map { row ->
                val topics = topicsOf[row.id].orEmpty()
                ContentItem(
                    id = row.id.orEmpty(),
                    type = row.type,
                    surah = row.surah?.toInt(),
                    ayah = row.ayah?.toInt(),
                    arabic = row.arabic,
                    length = ItemLength.of(row.length),
                    primaryTopic = row.primary_topic,
                    // The primary topic first, then the rest, whatever order the table holds them in.
                    topics = (listOf(row.primary_topic) + topics).distinct(),
                )
            }
            ContentCatalog(items, q.topics().executeAsList().mapNotNull { it.id })
        }.also { catalog = it }
    }

    override suspend fun texts(lang: ContentLang): Map<String, String> = lock.withLock {
        texts[lang] ?: withContext(io) {
            q.textsFor(lang.tag).executeAsList().associate { it.item_id.orEmpty() to it.text }
        }.also { texts[lang] = it }
    }

    override suspend fun topicLabels(lang: ContentLang): Map<String, String> = withContext(io) {
        q.topicLabels(lang.tag).executeAsList().associate { it.topic_id.orEmpty() to it.label.orEmpty() }
    }

    override suspend fun sources(): List<ContentSourceInfo> = withContext(io) {
        q.sources().executeAsList().map { ContentSourceInfo(it.lang.orEmpty(), it.name.orEmpty(), it.note?.takeIf { n -> n.isNotBlank() }) }
    }
}

/**
 * The translation for [item] in [lang], falling back to [ContentLang.FALLBACK] for that item only
 * (feed spec §10). Returns the text with the language it is actually in.
 */
suspend fun ContentSource.translationOf(item: ContentItem, lang: ContentLang): Pair<String, ContentLang>? {
    texts(lang)[item.id]?.let { return it to lang }
    if (lang == ContentLang.FALLBACK) return null
    return texts(ContentLang.FALLBACK)[item.id]?.let { it to ContentLang.FALLBACK }
}

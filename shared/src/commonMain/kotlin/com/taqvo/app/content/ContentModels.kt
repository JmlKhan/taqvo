package com.taqvo.app.content

/**
 * The languages content.db carries a translation in (feed content spec §2). The Arabic is on every
 * item itself and is always shown; these are the second line.
 */
enum class ContentLang(val tag: String) {
    UZ_LATN("uz-Latn"),
    UZ_CYRL("uz-Cyrl"),
    ;

    companion object {
        /**
         * The language a missing translation falls back to (feed spec §10): Uzbek in Cyrillic, the
         * app's own default. content.db carries Sodiq's translation in the two Uzbek scripts only
         * (no English or Russian rows since 2026-11-11), so a reader of any other language gets it.
         */
        val FALLBACK = UZ_CYRL
    }
}

/** How long an item reads, measured on its uz-Latn text (content spec §1). Widgets take [SHORT] only. */
enum class ItemLength(val id: String) {
    SHORT("short"),
    MEDIUM("medium"),
    LONG("long"),
    ;

    companion object {
        fun of(id: String): ItemLength = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

/**
 * One feed card's content, before any language is chosen. [id] is stable across content updates
 * ("q:2:153"), so the reader's favorites and history survive a new content.db.
 */
data class ContentItem(
    val id: String,
    val type: String,
    val surah: Int?,
    val ayah: Int?,
    val arabic: String,
    val length: ItemLength,
    val primaryTopic: String,
    /** One to three, [primaryTopic] among them. */
    val topics: List<String>,
)

/**
 * Every item and topic in content.db, held in memory: 499 short rows are cheaper to keep than to
 * query card by card, and the feed engine needs all of them to rank anyway.
 */
class ContentCatalog(val items: List<ContentItem>, val topics: List<String>) {
    val byId: Map<String, ContentItem> = items.associateBy { it.id }

    /** Each topic's items, in content order. A topic with no items maps to an empty list. */
    val byTopic: Map<String, List<ContentItem>> = topics.associateWith { topic -> items.filter { topic in it.topics } }
}

/** One source line for the Sources screen (content spec §6). */
data class ContentSourceInfo(val lang: String, val name: String, val note: String?)

/** A card ready to draw: the item, its translation in the reader's language, and its reference. */
data class FeedCard(
    val item: ContentItem,
    /** Null only for an item with no text in the language or in [ContentLang.FALLBACK]. */
    val translation: String?,
    /** "Ra'd surasi, 13:28" and its kin (feed spec §3.1). */
    val reference: String,
    /** The language [translation] is in, which is the reader's or the fallback's. */
    val lang: ContentLang,
    /** The translator's commentary in [lang] (izoh), shown on tap only; null when the ayah has none. */
    val note: String? = null,
)

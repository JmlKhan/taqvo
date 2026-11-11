package com.taqvo.app.feed

import com.taqvo.app.content.CardReference
import com.taqvo.app.content.ContentLang
import com.taqvo.app.content.ContentSource
import com.taqvo.app.content.FeedCard
import com.taqvo.app.content.noteOf
import com.taqvo.app.content.translationOf

/**
 * Cards by id for the screens outside the feed — favorites, a collection, a single card — in the
 * reader's language. An id that a newer content.db no longer carries is skipped, not shown blank.
 */
class FeedLibrary(private val content: ContentSource) {
    suspend fun cards(ids: List<String>, lang: ContentLang): List<FeedCard> {
        val catalog = content.catalog()
        return ids.mapNotNull { catalog.byId[it] }.map { item ->
            val (text, textLang) = content.translationOf(item, lang) ?: (null to lang)
            FeedCard(item, text, CardReference.of(item), textLang, content.noteOf(item, textLang))
        }
    }

    suspend fun card(id: String, lang: ContentLang): FeedCard? = cards(listOf(id), lang).firstOrNull()
}

/** The text "Copy text" puts on the clipboard (feed spec §9): the Arabic when shown, the translation, the reference. */
fun FeedCard.copyText(withArabic: Boolean): String = buildString {
    if (withArabic) append(item.arabic).append("\n\n")
    translation?.let { append(it) }
    append("\n\n— ").append(reference)
}.trim()

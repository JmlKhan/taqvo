package com.taqvo.app.content

/**
 * A card's reference (feed spec §3.1): the chapter and verse, "13:28". There is no surah name:
 * content.db carries no Uzbek names, and the only others on hand (quran.db's English
 * transliterations) would sit oddly beside Uzbek text. Add the name back, in the card's script,
 * once a verified `surah_names` table exists.
 */
object CardReference {

    fun format(surah: Int, ayah: Int): String = "$surah:$ayah"

    /** The reference for any item: an ayah's surah and number, or nothing for the kinds still to come. */
    fun of(item: ContentItem): String {
        val surah = item.surah ?: return ""
        val ayah = item.ayah ?: return ""
        return format(surah, ayah)
    }
}

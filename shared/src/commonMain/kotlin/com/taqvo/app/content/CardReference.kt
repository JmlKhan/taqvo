package com.taqvo.app.content

/**
 * A card's reference in its translation's language (feed spec §3.1), which is not the interface's:
 * a Russian phone reads Russian cards under an English interface.
 *
 * The surah names are quran.db's Latin transliterations until content.db carries its own
 * `surah_names` table (feed spec §3.1, [TODO content]); the patterns below are already the final
 * ones, so a localized name drops in without touching them.
 */
object CardReference {

    fun format(lang: ContentLang, surahName: String?, surah: Int, ayah: Int): String {
        val numbers = "$surah:$ayah"
        if (surahName.isNullOrBlank()) return numbers
        return when (lang) {
            ContentLang.UZ_LATN -> "$surahName surasi, $numbers"
            ContentLang.UZ_CYRL -> "$surahName сураси, $numbers"
            ContentLang.RU -> "Сура «$surahName», $numbers"
            ContentLang.EN -> "$surahName $numbers"
        }
    }

    /** The reference for any item: an ayah's surah and number, or nothing for the kinds still to come. */
    fun of(item: ContentItem, lang: ContentLang, surahName: (Int) -> String?): String {
        val surah = item.surah ?: return ""
        val ayah = item.ayah ?: return ""
        return format(lang, surahName(surah), surah, ayah)
    }
}

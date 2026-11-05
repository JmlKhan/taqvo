package com.taqvo.app.wallpaper

import com.taqvo.app.adhkar.AdhkarItem
import com.taqvo.app.adhkar.AdhkarKind
import com.taqvo.app.adhkar.AdhkarSet
import com.taqvo.app.adhkar.AdhkarText
import com.taqvo.app.adhkar.BOTH_KEY
import com.taqvo.app.quran.QuranSource
import com.taqvo.app.quran.QuranText
import com.taqvo.app.quran.ReadingSettings
import com.taqvo.app.widget.AyahPool

/** What a wallpaper carries: an ayah from the widget's pool, or one of the adhkar's supplications. */
enum class WallpaperSource { AYAH, DUA }

/**
 * One card's words, ready to draw. [quran] picks the face: the Hafs Mushaf face for the Quran,
 * the OS face for a supplication (the adhkar screen's own rule, [com.taqvo.app.feature.adhkar.DuaArabic]).
 * [translation] is null when there is none to show — the reader's translation is off, or the
 * interface language has no translation of that supplication (Arabic has none: it is the original).
 *
 * The reference is not here: it is words in the interface language, built at the call site from
 * [surah] and [ayah] or [hisn], with that language's digits.
 */
data class WallpaperText(
    val source: WallpaperSource,
    val arabic: String,
    val translation: String?,
    val quran: Boolean,
    /** For an ayah, its surah and number; 0 for a supplication. */
    val surah: Int = 0,
    val ayah: Int = 0,
    /** For a supplication, its number in Hisn al-Muslim; 0 for an ayah. */
    val hisn: Int = 0,
)

object WallpaperTexts {
    /**
     * The longest supplication a phone-sized card can carry in full, in characters of Arabic.
     * Longer ones (Sayyid al-Istighfar and the like) would shrink below a readable size, and a
     * wallpaper never cuts a supplication short.
     */
    const val MAX_DUA_LENGTH = 300

    /**
     * The hundred pool ayahs (the ayah widget's own, curated so every card carries a whole
     * thought), with the ayah-end roundel and the reader's [settings] translation.
     */
    suspend fun ayahs(quran: QuranSource, settings: ReadingSettings): List<WallpaperText> {
        val translationOff = settings.translationId == ReadingSettings.NO_TRANSLATION
        return AyahPool.REFS.mapNotNull { (surah, ayah) ->
            val arabic = quran.ayahText(surah, ayah) ?: return@mapNotNull null
            WallpaperText(
                source = WallpaperSource.AYAH,
                arabic = QuranText.withMarker(arabic, ayah),
                translation = if (translationOff) null else quran.translationText(settings.translationId, surah, ayah)?.takeIf { it.isNotBlank() },
                quran = true,
                surah = surah,
                ayah = ayah,
            )
        }
    }

    /**
     * The supplications that suit a wallpaper: said at any hour (one wording for both sets, so
     * none that greets "this morning" on an evening lock screen) and short enough to stay whole.
     * [text] is the interface language's translations, null where it has none.
     */
    fun duas(items: List<AdhkarItem>, text: AdhkarText?): List<WallpaperText> = items
        .filter { it.kind == AdhkarKind.DUA }
        .mapNotNull { item ->
            val arabic = item.arabic[BOTH_KEY] ?: return@mapNotNull null
            if (arabic.length > MAX_DUA_LENGTH) return@mapNotNull null
            WallpaperText(
                source = WallpaperSource.DUA,
                arabic = arabic,
                // Either set reads the shared wording; MORNING is only the key to it.
                translation = text?.translationFor(item.id, AdhkarSet.MORNING)?.takeIf { it.isNotBlank() },
                quran = false,
                hisn = item.hisn,
            )
        }
}

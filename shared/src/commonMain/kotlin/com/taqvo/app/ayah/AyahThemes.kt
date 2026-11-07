package com.taqvo.app.ayah

import com.taqvo.app.widget.AyahPool

/**
 * The ayah themes onboarding asks about (screen 05) and the daily ayah reminders draw from.
 *
 * Every tagged ayah is one of the curated [AyahPool.REFS], so whatever the reader picks, each
 * reminder and widget card still carries a whole thought. An ayah can carry several themes.
 * [SHORT] is not tagged by hand: it is measured from the Arabic itself ([isShort]).
 *
 * DRAFT (6 Nov 2026): these tags were drafted for review and have not yet been checked by a
 * scholar. Change a list freely; a test holds every entry to the pool.
 */
enum class AyahTheme(val id: String) {
    SHORT("short_ayahs"),
    COMFORT("comfort"),
    GRATITUDE("gratitude"),
    PATIENCE("patience"),
    REFLECTION("reflection"),
    MIX("mix"),
    ;

    companion object {
        fun of(id: String): AyahTheme? = entries.firstOrNull { it.id == id }
    }
}

object AyahThemes {
    /** An ayah whose Uthmani text (diacritics included) is at most this long counts as short. */
    const val SHORT_MAX_CHARS = 90

    val TAGGED: Map<AyahTheme, List<Pair<Int, Int>>> = mapOf(
        AyahTheme.COMFORT to listOf(
            9 to 51, 65 to 3, 3 to 173, 9 to 129, 39 to 36, 64 to 11, 11 to 6, 39 to 53,
            2 to 186, 40 to 60, 94 to 6, 3 to 139, 93 to 5, 13 to 28, 2 to 156, 15 to 49,
            42 to 19, 52 to 48, 27 to 62, 10 to 62, 3 to 160, 17 to 82, 50 to 16, 4 to 110,
        ),
        AyahTheme.GRATITUDE to listOf(
            14 to 7, 2 to 152, 93 to 11, 33 to 41, 17 to 70, 16 to 97, 55 to 60, 1 to 5,
            6 to 162, 7 to 180, 59 to 22, 40 to 65, 62 to 10, 7 to 55,
        ),
        AyahTheme.PATIENCE to listOf(
            2 to 153, 2 to 156, 3 to 200, 8 to 46, 11 to 115, 16 to 96, 52 to 48, 3 to 139,
            94 to 6, 31 to 17, 41 to 34, 30 to 60, 21 to 35, 29 to 69, 15 to 99, 3 to 160,
        ),
        AyahTheme.REFLECTION to listOf(
            67 to 2, 99 to 7, 53 to 39, 18 to 46, 74 to 38, 51 to 56, 36 to 82, 59 to 18,
            59 to 19, 28 to 88, 55 to 29, 23 to 115, 30 to 21, 42 to 30, 35 to 15, 49 to 13,
            16 to 90, 9 to 105, 21 to 107, 33 to 21, 31 to 18, 25 to 63, 41 to 33, 3 to 133,
            11 to 114, 17 to 81, 19 to 96, 22 to 77, 15 to 9, 10 to 58, 10 to 25, 28 to 56,
        ),
    )

    fun isShort(arabic: String): Boolean = arabic.length <= SHORT_MAX_CHARS

    /**
     * The pool references a reader who picked [themeIds] is reminded from, in pool order. No
     * pick, "mix", or picks that leave nothing (an unknown id, or "short" alone when [arabicOf]
     * knows none) all mean the whole pool: a reminder is never left with nothing to say.
     */
    fun refsFor(themeIds: Collection<String>, arabicOf: (Pair<Int, Int>) -> String?): List<Pair<Int, Int>> {
        val themes = themeIds.mapNotNull(AyahTheme::of).toSet()
        if (themes.isEmpty() || AyahTheme.MIX in themes) return AyahPool.REFS
        val picked = AyahPool.REFS.filter { ref ->
            themes.any { theme ->
                when (theme) {
                    AyahTheme.SHORT -> arabicOf(ref)?.let(::isShort) == true
                    else -> ref in TAGGED[theme].orEmpty()
                }
            }
        }
        return picked.ifEmpty { AyahPool.REFS }
    }
}

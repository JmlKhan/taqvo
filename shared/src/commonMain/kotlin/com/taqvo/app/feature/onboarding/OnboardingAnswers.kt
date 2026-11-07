package com.taqvo.app.feature.onboarding

/**
 * What the reader told onboarding (spec §3). Kept on the device only: the intro screen promises
 * so, and analytics carries the option ids, never [name]. A null [name] is a skipped question; an
 * empty list is a skipped or not-yet-answered one.
 */
data class OnboardingAnswers(
    val name: String? = null,
    val goals: List<String> = emptyList(),
    val contentTypes: List<String> = emptyList(),
    val useCases: List<String> = emptyList(),
) {
    val wantsWidgets: Boolean get() = UseCase.WIDGETS in useCases
    val wantsPrayerTimes: Boolean get() = UseCase.PRAYER_TIMES in useCases
}

/** Screen 04's options. The ids are what is stored and sent; the labels live in resources. */
object Goal {
    const val REMEMBER = "remember"
    const val PEACE = "peace"
    const val HARDSHIP = "hardship"
    const val IMAN = "iman"
    const val HABIT = "habit"
    val ALL = listOf(REMEMBER, PEACE, HARDSHIP, IMAN, HABIT)
}

/** Screen 05's options: the [com.taqvo.app.ayah.AyahTheme] ids. */
object ContentType {
    val ALL = listOf("short_ayahs", "comfort", "gratitude", "patience", "reflection", "mix")
}

/** Screen 08's options. */
object UseCase {
    const val WIDGETS = "widgets"
    const val WALLPAPERS = "wallpapers"
    const val DAILY_AYAHS = "daily_ayahs"
    const val PRAYER_TIMES = "prayer_times"
    const val TASBEH = "tasbeh"
    const val QURAN_READING = "quran_reading"
    const val DAILY_HABIT = "daily_habit"
    val ALL = listOf(WIDGETS, WALLPAPERS, DAILY_AYAHS, PRAYER_TIMES, TASBEH, QURAN_READING, DAILY_HABIT)
}

/** Screen 03: trimmed, at most this many characters. */
const val NAME_MAX_LENGTH = 30

/** The name as it is stored, or null when nothing is left once trimmed. */
fun cleanName(raw: String): String? = raw.trim().take(NAME_MAX_LENGTH).takeIf { it.isNotEmpty() }

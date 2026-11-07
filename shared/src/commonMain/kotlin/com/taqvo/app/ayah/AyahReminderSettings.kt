package com.taqvo.app.ayah

import com.taqvo.app.quran.ReadingSettings

/**
 * How the daily ayah reminders are set (onboarding screen 09): [count] a day, spread evenly from
 * [startMinute] to [endMinute] (minutes after local midnight). [denied] records that the system
 * refused notifications when they were set up, for Settings to offer a way back.
 */
data class AyahReminderSettings(
    val enabled: Boolean = false,
    val count: Int = DEFAULT_COUNT,
    val startMinute: Int = DEFAULT_START,
    val endMinute: Int = DEFAULT_END,
    val denied: Boolean = false,
) {
    /** The minutes after midnight each reminder rings at, earliest first. */
    fun minutesOfDay(): List<Int> {
        val n = count.coerceIn(MIN_COUNT, maxCountFor(startMinute, endMinute))
        if (n == 1) return listOf(startMinute)
        val span = endMinute - startMinute
        return List(n) { i -> startMinute + span * i / (n - 1) }
    }

    companion object {
        const val MIN_COUNT = 1
        const val MAX_COUNT = 20

        /** The spec observed 10 a day; fewer is kinder for a first install. */
        const val DEFAULT_COUNT = 5
        const val DEFAULT_START = 9 * 60
        const val DEFAULT_END = 22 * 60

        /** No two reminders closer together than this. */
        const val MIN_GAP_MINUTES = 15

        /** The most reminders the window from [start] to [end] holds at [MIN_GAP_MINUTES] apart. */
        fun maxCountFor(start: Int, end: Int): Int =
            if (end <= start) MIN_COUNT else ((end - start) / MIN_GAP_MINUTES + 1).coerceIn(MIN_COUNT, MAX_COUNT)

        /** True when [end] is after [start] and the window holds [count] reminders. */
        fun isValid(count: Int, start: Int, end: Int): Boolean =
            end > start && count in MIN_COUNT..maxCountFor(start, end)
    }
}

/** How an ayah is shown on widgets and in reminders (onboarding screen 17). */
enum class AyahFormat { TRANSLATION, ARABIC_TRANSLATION, ARABIC }

/**
 * The reading settings the ayah widget is written with under [format]: Arabic only is the
 * widget's own no-translation card. Translation only still shows the Arabic on the widget, whose
 * card is laid out around it; the reminders honour it in full ([AyahReminderSource]).
 */
fun ReadingSettings.forAyahFormat(format: AyahFormat): ReadingSettings =
    if (format == AyahFormat.ARABIC) copy(translationId = ReadingSettings.NO_TRANSLATION) else this

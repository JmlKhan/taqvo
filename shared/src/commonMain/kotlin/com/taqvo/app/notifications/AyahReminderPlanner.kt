package com.taqvo.app.notifications

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.taqvo.app.ayah.AyahReminderSettings
import com.taqvo.app.domain.Prayer
import com.taqvo.app.domain.PrayerSound
import kotlin.random.Random
import kotlin.time.Instant

/** One ayah as a reminder shows it: its reference as the title, its words as the body. */
data class AyahReminderText(val surah: Int, val ayah: Int, val title: String, val body: String)

/**
 * Plans the daily ayah reminders (onboarding spec, screen 09): [AyahReminderSettings.count] a day
 * across the reader's window, for [days] days from [from]. Each day's ayahs are a fresh draw from
 * [candidates], with no ayah twice in one day while the candidates last. The draw is fixed by
 * [seed] and the date, so a reschedule the same day arms the same ayahs rather than new ones.
 */
object AyahReminderPlanner {

    fun plan(
        settings: AyahReminderSettings,
        candidates: List<AyahReminderText>,
        from: Instant,
        timeZoneId: String,
        days: Int,
        seed: Long,
    ): List<ScheduledNotification> {
        if (!settings.enabled || candidates.isEmpty() || days <= 0) return emptyList()
        val zone = TimeZone.of(timeZoneId)
        val today = from.toLocalDateTime(zone).date
        val minutes = settings.minutesOfDay()
        return (0 until days).flatMap { offset ->
            val date = today.plus(offset, DateTimeUnit.DAY)
            val todays = drawFor(date, seed, candidates, minutes.size)
            minutes.mapIndexedNotNull { slot, minute ->
                val at = LocalDateTime(date, LocalTime(minute / 60, minute % 60)).toInstant(zone)
                if (at <= from) return@mapIndexedNotNull null
                val text = todays[slot]
                ScheduledNotification(
                    id = "AYAH-$date-$slot",
                    prayer = Prayer.SUNRISE,
                    kind = NotificationKind.AYAH,
                    instant = at,
                    sound = PrayerSound.NOTIFICATION,
                    title = text.title,
                    body = text.body,
                )
            }
        }
    }

    /** [count] candidates for [date]: a shuffle of all of them, repeated only if [count] is more. */
    internal fun drawFor(date: LocalDate, seed: Long, candidates: List<AyahReminderText>, count: Int): List<AyahReminderText> {
        val shuffled = candidates.shuffled(Random(seed xor date.toEpochDays().toLong() * 0x9E3779B97F4A7C15uL.toLong()))
        return List(count) { shuffled[it % shuffled.size] }
    }
}

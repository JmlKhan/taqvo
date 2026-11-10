package com.taqvo.app.notifications

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import com.taqvo.app.domain.Prayer
import com.taqvo.app.domain.PrayerSound
import com.taqvo.app.i18n.UiLanguage
import kotlin.time.Instant

/**
 * The streak reminder (feed spec §8): at 20:00 on a day the feed has not counted yet, one local
 * notification. It rides with the daily ayah reminders — their channel, their switch, their count
 * — so it takes the place of that day's last ayah reminder rather than adding one more.
 *
 * Planned days ahead, like every notification here, because nothing runs at 20:00 to decide; the
 * feed reschedules the moment a day counts, which is what takes that day's reminder back.
 */
object StreakReminderPlanner {
    const val HOUR = 20

    fun plan(
        ayahReminders: List<ScheduledNotification>,
        from: Instant,
        timeZoneId: String,
        days: Int,
        /** True once today has counted (three cards seen or one liked). */
        todayCounted: Boolean,
        /** Only a running streak has anything to keep; a reader with none is not nagged. */
        streakAlive: Boolean,
        uiLanguageTag: String,
    ): List<ScheduledNotification> {
        // The daily reminders' switch is this reminder's switch too (feed spec §8: "only if
        // notifications are allowed", within the daily reminder limit).
        if (!streakAlive || ayahReminders.isEmpty() || days <= 0) return ayahReminders
        val zone = TimeZone.of(timeZoneId)
        val today = from.toLocalDateTime(zone).date
        val (title, body) = copy(uiLanguageTag)
        val result = ayahReminders.toMutableList()
        (0 until days).forEach { offset ->
            val date = today.plus(offset, DateTimeUnit.DAY)
            if (offset == 0 && todayCounted) return@forEach
            val at = LocalDateTime(date, LocalTime(HOUR, 0)).toInstant(zone)
            if (at <= from) return@forEach
            val sameDay = result.filter { it.kind == NotificationKind.AYAH && it.instant.toLocalDateTime(zone).date == date }
            if (sameDay.isEmpty()) return@forEach
            result.remove(sameDay.maxBy { it.instant })
            result += ScheduledNotification(
                id = streakNotificationId(date),
                prayer = Prayer.SUNRISE,
                kind = NotificationKind.AYAH,
                instant = at,
                sound = PrayerSound.NOTIFICATION,
                title = title,
                body = body,
            )
        }
        return result.sortedBy { it.instant }
    }

    fun streakNotificationId(date: LocalDate): String = "STREAK-$date"

    /**
     * Templates in code, as [LocalizedNotificationCopy] keeps its own: a boot receiver has no
     * composition to read a string resource in.
     */
    fun copy(uiLanguageTag: String): Pair<String, String> = when (UiLanguage.of(uiLanguageTag)) {
        UiLanguage.ARABIC -> "حافظ على سلسلتك" to "حافظ على سلسلتك — آية اليوم بانتظارك"
        UiLanguage.FRENCH -> "Gardez votre série" to "Ne perdez pas votre série : le verset du jour vous attend"
        UiLanguage.TURKISH -> "Serinizi koruyun" to "Serinizi sürdürün — bugünün ayeti sizi bekliyor"
        UiLanguage.INDONESIAN -> "Jaga hari beruntun Anda" to "Jaga hari beruntun Anda — ayat hari ini menanti Anda"
        UiLanguage.URDU -> "اپنا تسلسل برقرار رکھیں" to "اپنا تسلسل جاری رکھیں — آج کی آیت آپ کی منتظر ہے"
        UiLanguage.BENGALI -> "ধারাবাহিকতা ধরে রাখুন" to "ধারাবাহিকতা বজায় রাখুন — আজকের আয়াত আপনার অপেক্ষায়"
        UiLanguage.UZBEK -> "Seriyangizni saqlang" to "Seriyangizni saqlab qoling — bugungi oyatingiz sizni kutmoqda"
        UiLanguage.UZBEK_CYRILLIC -> "Серияларингизни сақланг" to "Серияларингизни сақлаб қолинг — бугунги оятингиз сизни кутмоқда"
        else -> "Keep your streak" to "Keep your streak going — today’s ayah is waiting for you"
    }
}

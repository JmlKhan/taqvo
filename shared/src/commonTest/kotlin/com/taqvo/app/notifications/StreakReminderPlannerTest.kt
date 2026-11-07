package com.taqvo.app.notifications

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.taqvo.app.domain.Prayer
import com.taqvo.app.domain.PrayerSound
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreakReminderPlannerTest {
    private val zone = TimeZone.UTC
    private val monday = LocalDate(2026, 11, 2)
    private val tuesday = LocalDate(2026, 11, 3)

    private fun at(date: LocalDate, hour: Int) = LocalDateTime(date, LocalTime(hour, 0)).toInstant(zone)

    private fun ayah(date: LocalDate, hour: Int) = ScheduledNotification(
        id = "AYAH-$date-$hour",
        prayer = Prayer.SUNRISE,
        kind = NotificationKind.AYAH,
        instant = at(date, hour),
        sound = PrayerSound.NOTIFICATION,
        title = "t",
        body = "b",
    )

    private val ayahs = listOf(ayah(monday, 9), ayah(monday, 21), ayah(tuesday, 9), ayah(tuesday, 21))

    private fun plan(todayCounted: Boolean = false, alive: Boolean = true, fromHour: Int = 10) = StreakReminderPlanner.plan(
        ayahReminders = ayahs,
        from = at(monday, fromHour),
        timeZoneId = "UTC",
        days = 2,
        todayCounted = todayCounted,
        streakAlive = alive,
        uiLanguageTag = "uz",
    )

    @Test fun eachDayAt20TheStreakReminderTakesTheLastAyahSlot() {
        val plan = plan()
        assertEquals(ayahs.size, plan.size)
        assertEquals(listOf("AYAH-$monday-9", "STREAK-$monday", "AYAH-$tuesday-9", "STREAK-$tuesday"), plan.map { it.id })
        assertEquals(at(monday, 20), plan[1].instant)
        assertEquals("Seriyangizni saqlang", plan[1].title)
    }

    @Test fun aDayAlreadyCountedHasNoReminder() {
        val ids = plan(todayCounted = true).map { it.id }
        assertTrue("STREAK-$monday" !in ids)
        assertTrue("AYAH-$monday-21" in ids)
        assertTrue("STREAK-$tuesday" in ids)
    }

    @Test fun noStreakNoReminder() = assertEquals(ayahs, plan(alive = false))

    @Test fun pastEightTheReminderIsForTomorrowOnly() {
        val ids = plan(fromHour = 20).map { it.id }
        assertTrue("STREAK-$monday" !in ids)
        assertTrue("STREAK-$tuesday" in ids)
    }

    @Test fun withTheDailyRemindersOffThereIsNoneEither() {
        val none = StreakReminderPlanner.plan(emptyList(), at(monday, 10), "UTC", 2, todayCounted = false, streakAlive = true, uiLanguageTag = "en")
        assertTrue(none.isEmpty())
    }
}

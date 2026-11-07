package com.taqvo.app.notifications

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.taqvo.app.ayah.AyahReminderSettings
import com.taqvo.app.domain.NotificationSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

/** The daily ayah reminders (onboarding spec, screen 09): when they ring and which ayahs they carry. */
class AyahReminderPlannerTest {

    private val tashkent = "Asia/Tashkent"
    private val candidates = (1..30).map { AyahReminderText(2, it, "Al-Baqarah 2:$it", "ayah $it") }

    /** 06:00 in Tashkent on 6 Nov 2026, before the window opens. */
    private val morning = Instant.parse("2026-11-06T01:00:00Z")

    private fun on(count: Int, start: Int = 9 * 60, end: Int = 22 * 60) =
        AyahReminderSettings(enabled = true, count = count, startMinute = start, endMinute = end)

    @Test
    fun `reminders are spread evenly from the start of the window to its end`() {
        assertEquals(listOf(540, 735, 930, 1125, 1320), on(5).minutesOfDay())
        assertEquals(listOf(540), on(1).minutesOfDay())
    }

    @Test
    fun `the count is held to what the window holds at fifteen minutes apart`() {
        // An hour holds five: :00, :15, :30, :45 and the hour.
        assertEquals(5, AyahReminderSettings.maxCountFor(9 * 60, 10 * 60))
        assertEquals(5, on(12, 9 * 60, 10 * 60).minutesOfDay().size)
        assertEquals(AyahReminderSettings.MAX_COUNT, AyahReminderSettings.maxCountFor(0, 23 * 60))
        assertTrue(AyahReminderSettings.isValid(5, 9 * 60, 22 * 60))
        assertTrue(!AyahReminderSettings.isValid(5, 22 * 60, 9 * 60))
    }

    @Test
    fun `off plans nothing`() {
        val plan = AyahReminderPlanner.plan(on(5).copy(enabled = false), candidates, morning, tashkent, days = 3, seed = 7)
        assertEquals(emptyList(), plan)
    }

    @Test
    fun `each day carries count reminders at their local times and none in the past`() {
        val plan = AyahReminderPlanner.plan(on(5), candidates, morning, tashkent, days = 3, seed = 7)
        assertEquals(15, plan.size)
        val zone = TimeZone.of(tashkent)
        val first = plan.first().instant.toLocalDateTime(zone)
        assertEquals(LocalDate(2026, 11, 6), first.date)
        assertEquals(9, first.hour)
        assertTrue(plan.all { it.kind == NotificationKind.AYAH && it.instant > morning })
        // Noon: 09:00 is behind, so 12:15, 15:30, 18:45 and 22:00 are what is left today.
        val noon = Instant.parse("2026-11-06T07:00:00Z")
        val later = AyahReminderPlanner.plan(on(5), candidates, noon, tashkent, days = 1, seed = 7)
        assertEquals(4, later.size)
    }

    @Test
    fun `no ayah comes twice in one day while there are enough of them`() {
        val plan = AyahReminderPlanner.plan(on(10), candidates, morning, tashkent, days = 2, seed = 7)
        plan.groupBy { it.id.substringBeforeLast('-') }.values.forEach { day ->
            assertEquals(day.size, day.map { it.body }.distinct().size)
        }
    }

    @Test
    fun `the same seed and day draw the same ayahs so a reschedule does not change them`() {
        val a = AyahReminderPlanner.plan(on(5), candidates, morning, tashkent, days = 2, seed = 42)
        val b = AyahReminderPlanner.plan(on(5), candidates, morning, tashkent, days = 2, seed = 42)
        assertEquals(a.map { it.body }, b.map { it.body })
        val c = AyahReminderPlanner.plan(on(5), candidates, morning, tashkent, days = 2, seed = 43)
        assertTrue(a.map { it.body } != c.map { it.body })
    }

    @Test
    fun `the window shrinks for the ayahs but never below the floor`() {
        val notifications = NotificationSettings()
        val without = NotificationPlanner.windowDaysFor(NotificationPlanner.IOS_PENDING_LIMIT, notifications)
        val with = NotificationPlanner.windowDaysFor(NotificationPlanner.IOS_PENDING_LIMIT, notifications, ayahPerDay = 5)
        assertTrue(with < without, "$with vs $without")
        val many = NotificationPlanner.windowDaysFor(NotificationPlanner.IOS_PENDING_LIMIT, notifications, ayahPerDay = 20)
        assertTrue(many >= minOf(NotificationPlanner.MIN_WINDOW_DAYS, without), "$many")
    }
}

package com.taqvo.app.feed

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** The daily goal (feed spec §6): five liked cards a day, each counted once, reset at local midnight. */
object DailyGoal {
    const val TARGET = 5

    fun done(likesToday: Int): Boolean = likesToday >= TARGET

    /** 0 to 1, for the pill's progress line. */
    fun progress(likesToday: Int): Float = (likesToday.coerceIn(0, TARGET)).toFloat() / TARGET
}

/** What one day held, for the goal and the streak. */
data class DayActivity(val views: Int = 0, val likes: Int = 0)

/**
 * The streak (feed spec §8). [count] runs while every day since [lastActive] has counted; [best]
 * is the longest it has ever been.
 */
data class Streak(val count: Int = 0, val lastActive: LocalDate? = null, val best: Int = 0)

object StreakTracker {
    /** Views that make a day count without a like. The softer rule of the spec's two (feed spec §8 [DECIDE]). */
    const val VIEWS_FOR_DAY = 3
    const val LIKES_FOR_DAY = 1

    fun dayCounts(activity: DayActivity): Boolean =
        activity.views >= VIEWS_FOR_DAY || activity.likes >= LIKES_FOR_DAY

    /**
     * [streak] after [today] has counted. Counting the same day twice changes nothing; a day after
     * [Streak.lastActive] continues the run; any later day starts it again at one. A [today] before
     * [Streak.lastActive] (the clock or the time zone moved back) leaves it alone rather than
     * breaking it.
     */
    fun record(streak: Streak, today: LocalDate): Streak {
        val last = streak.lastActive
        val count = when {
            last == null -> 1
            today <= last -> return streak
            last == today.minus(DatePeriod(days = 1)) -> streak.count + 1
            else -> 1
        }
        return Streak(count = count, lastActive = today, best = maxOf(streak.best, count))
    }

    /**
     * The count to show on [today]: the run while it is alive (counted today, or yesterday and today
     * still to come), and nothing once a whole day has gone by uncounted.
     */
    fun current(streak: Streak, today: LocalDate): Int {
        val last = streak.lastActive ?: return 0
        return if (last >= today.minus(DatePeriod(days = 1))) streak.count else 0
    }

    /**
     * The streak a reader arrives in the feed with. Onboarding's screen 07 starts it on the day it
     * was shown ([onboardingStart]), so the first session continues that day rather than starting
     * a second one (feed spec §8).
     */
    fun seeded(stored: Streak?, onboardingStart: LocalDate?): Streak =
        stored ?: onboardingStart?.let { Streak(count = 1, lastActive = it, best = 1) } ?: Streak()

    /** The seven days ending [today], oldest first, each true when it counted. */
    fun week(today: LocalDate, activity: Map<LocalDate, DayActivity>): List<Pair<LocalDate, Boolean>> =
        (6 downTo 0).map { back ->
            val day = today.minus(DatePeriod(days = back))
            day to dayCounts(activity[day] ?: DayActivity())
        }
}

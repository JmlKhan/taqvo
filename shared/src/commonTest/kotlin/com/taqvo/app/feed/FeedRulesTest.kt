package com.taqvo.app.feed

import kotlinx.datetime.LocalDate
import com.taqvo.app.content.CardReference
import com.taqvo.app.content.ContentLang
import com.taqvo.app.content.LanguageResolver
import com.taqvo.app.feature.onboarding.Goal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LanguageResolverTest {
    @Test fun theFourLanguagesResolveToThemselves() {
        assertEquals(ContentLang.UZ_LATN, LanguageResolver.resolve("uz-Latn"))
        assertEquals(ContentLang.UZ_CYRL, LanguageResolver.resolve("uz-Cyrl"))
        assertEquals(ContentLang.RU, LanguageResolver.resolve("ru"))
        assertEquals(ContentLang.EN, LanguageResolver.resolve("en"))
    }

    @Test fun uzbekWithNoScriptIsLatin() {
        assertEquals(ContentLang.UZ_LATN, LanguageResolver.resolve("uz"))
        assertEquals(ContentLang.UZ_LATN, LanguageResolver.resolve("uz-UZ"))
    }

    @Test fun regionsAndUnderscoresAreIgnored() {
        assertEquals(ContentLang.UZ_CYRL, LanguageResolver.resolve("uz-Cyrl-UZ"))
        assertEquals(ContentLang.UZ_CYRL, LanguageResolver.resolve("uz_Cyrl_UZ"))
        assertEquals(ContentLang.UZ_LATN, LanguageResolver.resolve("uz-Latn-UZ"))
        assertEquals(ContentLang.RU, LanguageResolver.resolve("ru-RU"))
        assertEquals(ContentLang.EN, LanguageResolver.resolve("en-GB"))
    }

    @Test fun everyOtherLanguageFallsBackToEnglish() {
        listOf("tr", "tr-TR", "ar", "fr-FR", "id", "", "x").forEach {
            assertEquals(ContentLang.EN, LanguageResolver.resolve(it), it)
        }
    }
}

class CardReferenceTest {
    @Test fun eachLanguageWritesTheReferenceItsOwnWay() {
        assertEquals("Ar-Ra'd surasi, 13:28", CardReference.format(ContentLang.UZ_LATN, "Ar-Ra'd", 13, 28))
        assertEquals("Ar-Ra'd сураси, 13:28", CardReference.format(ContentLang.UZ_CYRL, "Ar-Ra'd", 13, 28))
        assertEquals("Сура «Ar-Ra'd», 13:28", CardReference.format(ContentLang.RU, "Ar-Ra'd", 13, 28))
        assertEquals("Ar-Ra'd 13:28", CardReference.format(ContentLang.EN, "Ar-Ra'd", 13, 28))
    }

    @Test fun aMissingNameLeavesTheNumbers() {
        assertEquals("13:28", CardReference.format(ContentLang.EN, null, 13, 28))
    }
}

class TopicWeightsTest {
    private val topics = listOf("sabr", "rahmat", "zikr", "tavakkul", "shukr", "tabiat", "ilm", "oxirat", "iymon", "asmaul_husna", "ibodat")

    @Test fun noAnswersLeaveEveryTopicAtOne() {
        assertTrue(TopicWeights.initial(topics, emptyList(), emptyList()).values.all { it == 1.0 })
        assertTrue(TopicWeights.initial(topics, emptyList(), listOf("mix", "short_ayahs")).values.all { it == 1.0 })
    }

    @Test fun theAnswersAddWhatTheTableSays() {
        val w = TopicWeights.initial(topics, listOf(Goal.PEACE), listOf("comfort"))
        // peace: sabr, rahmat +2, zikr +1; comfort: rahmat, sabr +2, tavakkul +1.
        assertEquals(5.0, w["sabr"])
        assertEquals(5.0, w["rahmat"])
        assertEquals(2.0, w["zikr"])
        assertEquals(2.0, w["tavakkul"])
        assertEquals(1.0, w["shukr"])
    }

    @Test fun startingWeightsNeverPassTheCeiling() {
        val w = TopicWeights.initial(topics, listOf(Goal.PEACE, Goal.HARDSHIP), listOf("comfort", "patience"))
        assertEquals(TopicWeights.MAX, w["sabr"])
    }

    @Test fun likesAndSkipsAreClamped() {
        var w = mapOf("sabr" to 4.9, "zikr" to 0.25)
        w = TopicWeights.adjust(w, listOf("sabr"), TopicWeights.LIKE)
        w = TopicWeights.adjust(w, listOf("zikr"), TopicWeights.SKIP)
        assertEquals(TopicWeights.MAX, w["sabr"])
        assertEquals(TopicWeights.MIN, w["zikr"])
    }

    @Test fun aNewTopicStartsAtOneAndAGoneOneIsDropped() {
        assertEquals(mapOf("sabr" to 2.0, "ilm" to 1.0), TopicWeights.complete(mapOf("sabr" to 2.0, "old" to 3.0), listOf("sabr", "ilm")))
    }

    @Test fun shortAyahsIsAPreferenceNotATopic() {
        assertTrue(TopicWeights.prefersShort(listOf("short_ayahs")))
        assertFalse(TopicWeights.prefersShort(listOf("comfort")))
    }
}

class StreakTrackerTest {
    private val mon = LocalDate(2026, 11, 2)
    private val tue = LocalDate(2026, 11, 3)
    private val thu = LocalDate(2026, 11, 5)

    @Test fun theFirstDayStartsAtOne() = assertEquals(Streak(1, mon, 1), StreakTracker.record(Streak(), mon))

    @Test fun theSameDayTwiceCountsOnce() {
        val once = StreakTracker.record(Streak(), mon)
        assertEquals(once, StreakTracker.record(once, mon))
    }

    @Test fun theNextDayContinues() = assertEquals(Streak(3, tue, 3), StreakTracker.record(Streak(2, mon, 2), tue))

    @Test fun aGapStartsAgainButKeepsTheBest() = assertEquals(Streak(1, thu, 7), StreakTracker.record(Streak(7, mon, 7), thu))

    @Test fun aClockThatMovedBackBreaksNothing() {
        // Crossing time zones westward can make "today" the day before the last counted one.
        val s = Streak(4, tue, 4)
        assertEquals(s, StreakTracker.record(s, mon))
    }

    @Test fun theCountShownLastsUntilADayIsMissed() {
        val s = Streak(4, mon, 4)
        assertEquals(4, StreakTracker.current(s, mon))
        assertEquals(4, StreakTracker.current(s, tue))
        assertEquals(0, StreakTracker.current(s, thu))
    }

    @Test fun onboardingSeedsDayOne() {
        assertEquals(Streak(1, mon, 1), StreakTracker.seeded(null, mon))
        assertEquals(Streak(5, tue, 6), StreakTracker.seeded(Streak(5, tue, 6), mon))
        assertEquals(Streak(), StreakTracker.seeded(null, null))
    }

    @Test fun threeViewsOrOneLikeCountTheDay() {
        assertTrue(StreakTracker.dayCounts(DayActivity(views = 3)))
        assertTrue(StreakTracker.dayCounts(DayActivity(likes = 1)))
        assertFalse(StreakTracker.dayCounts(DayActivity(views = 2)))
    }

    @Test fun theWeekRunsOldestFirstToToday() {
        val week = StreakTracker.week(thu, mapOf(tue to DayActivity(likes = 1), thu to DayActivity(views = 3)))
        assertEquals(7, week.size)
        assertEquals(thu, week.last().first)
        assertEquals(listOf(false, false, false, false, true, false, true), week.map { it.second })
    }
}

class DailyGoalTest {
    @Test fun fiveLikesMeetTheGoal() {
        assertFalse(DailyGoal.done(4))
        assertTrue(DailyGoal.done(5))
        assertEquals(0.4f, DailyGoal.progress(2))
        assertEquals(1f, DailyGoal.progress(9))
    }
}

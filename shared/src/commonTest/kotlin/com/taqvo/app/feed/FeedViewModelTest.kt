package com.taqvo.app.feed

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import com.taqvo.app.content.ContentLang
import com.taqvo.app.feature.onboarding.Goal
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class FeedViewModelTest {
    private val zone = TimeZone.UTC
    private val monday = LocalDate(2026, 11, 2)
    private var now: Instant = monday.atStartOfDayIn(zone) + 9.hours

    private val store = FakeFeedStore()
    private val prefs = FakeFeedPrefs()
    private var content = FakeContent()
    private val analytics = RecordingAnalytics()
    private var answers = FeedOnboarding()

    private fun TestScope.vm() = FeedViewModel(
        content = content,
        store = store,
        prefs = prefs,
        onboarding = { answers },
        analytics = analytics,
        engine = FeedEngine(Random(42)),
        clock = { now },
        zone = { zone },
        scope = TestScope(StandardTestDispatcher(testScheduler)),
    )

    private fun TestScope.started(lang: ContentLang = ContentLang.UZ_LATN): FeedViewModel =
        vm().also { it.start(lang); testScheduler.advanceUntilIdle() }

    private fun TestScope.settle(vm: FeedViewModel, index: Int, afterMs: Long) {
        now += afterMs.milliseconds
        vm.onSettled(index)
        testScheduler.advanceUntilIdle()
    }

    @Test fun theFeedOpensOnTheCardOfTheDayWithAFullBatchBehindIt() = runTest {
        val s = started().state.value
        assertFalse(s.loading)
        assertEquals(1 + FeedRules.BATCH, s.cards.size)
        assertEquals(store.state?.cardOfDay, s.cards.first().item.id)
        assertEquals("uz-Latn ${s.cards.first().item.id}", s.cards.first().translation)
        assertEquals("${s.cards.first().item.surah}:${s.cards.first().item.ayah}", s.cards.first().reference)
    }

    @Test fun onboardingAnswersBecomeTheStartingWeights() = runTest {
        answers = FeedOnboarding(goals = listOf(Goal.REMEMBER))
        started()
        assertEquals(3.0, store.weights["zikr"])
        assertEquals(1.0, store.weights["sabr"])
    }

    @Test fun aMissingTranslationFallsBackToUzbekCyrillicForThatCard() = runTest {
        content = FakeContent(emptyLang = ContentLang.UZ_LATN)
        val card = started(ContentLang.UZ_LATN).state.value.cards.first()
        assertEquals(ContentLang.UZ_CYRL, card.lang)
        assertTrue(card.reference.endsWith("${card.item.surah}:${card.item.ayah}"))
    }

    @Test fun theSameDayResumesWhereTheReaderWas() = runTest {
        val first = started()
        settle(first, 3, afterMs = 2000)
        val queue = first.state.value.cards.map { it.item.id }
        val again = started().state.value
        assertEquals(3, again.position)
        assertEquals(queue, again.cards.map { it.item.id })
    }

    @Test fun aNewDayStartsAFreshQueueWithoutYesterdaysCards() = runTest {
        val first = started()
        (1..3).forEach { settle(first, it, afterMs = 2000) }
        val seen = store.seen.map { it.first }.toSet()
        now += 24.hours
        val next = started().state.value
        assertEquals(0, next.position)
        assertEquals(monday.plusOneDay(), store.state?.day)
        assertTrue(next.cards.none { it.item.id in seen }, "yesterday's seen cards came back")
    }

    @Test fun aCardOnScreenForOneAndAHalfSecondsIsSeen() = runTest {
        val vm = started()
        val id = vm.state.value.cards[0].item.id
        settle(vm, 1, afterMs = 1500)
        assertEquals(listOf(id), store.seen.map { it.first })
    }

    @Test fun aQuickSwipeIsNotSeenAndNotASkipInTheFirstTenCards() = runTest {
        val vm = started()
        val before = store.weights.toMap()
        settle(vm, 1, afterMs = 300)
        assertTrue(store.seen.isEmpty())
        assertEquals(before, store.weights)
    }

    @Test fun aQuickSwipeAfterTheFirstTenCardsLowersItsTopics() = runTest {
        val vm = started()
        (1..10).forEach { settle(vm, it, afterMs = 2000) }
        val skipped = vm.state.value.cards[10].item
        val before = store.weights.getValue(skipped.primaryTopic)
        settle(vm, 11, afterMs = 400)
        assertEquals(before + TopicWeights.SKIP, store.weights.getValue(skipped.primaryTopic), 1e-9)
    }

    @Test fun theQueueRefillsBeforeItRunsOut() = runTest {
        val vm = started()
        val size = vm.state.value.cards.size
        settle(vm, size - FeedRules.REFILL_BELOW, afterMs = 2000)
        assertEquals(size + FeedRules.BATCH, vm.state.value.cards.size)
        val ids = vm.state.value.cards.map { it.item.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun aLikeSavesRaisesItsTopicsAndCountsTowardTheGoal() = runTest {
        val vm = started()
        val item = vm.state.value.cards[0].item
        val before = store.weights.getValue(item.primaryTopic)
        vm.like(item.id, LikeVia.DOUBLE_TAP)
        testScheduler.advanceUntilIdle()
        val s = vm.state.value
        assertTrue(item.id in s.favorites)
        assertEquals(1, s.likesToday)
        assertEquals(before + TopicWeights.LIKE, store.weights.getValue(item.primaryTopic), 1e-9)
        assertEquals("double_tap", analytics.events.last { it.first == FeedEvents.LIKE }.second["via"])
    }

    @Test fun unLikingKeepsTheGoalAndALikeAgainIsNotCountedTwice() = runTest {
        val vm = started()
        val id = vm.state.value.cards[0].item.id
        vm.like(id, LikeVia.TAP); testScheduler.advanceUntilIdle()
        vm.unlike(id); testScheduler.advanceUntilIdle()
        assertFalse(id in vm.state.value.favorites)
        vm.like(id, LikeVia.TAP); testScheduler.advanceUntilIdle()
        assertEquals(1, vm.state.value.likesToday)
    }

    @Test fun theFifthLikeCelebratesOnceADay() = runTest {
        val vm = started()
        vm.state.value.cards.take(5).forEach { vm.like(it.item.id, LikeVia.TAP) }
        testScheduler.advanceUntilIdle()
        assertTrue(vm.state.value.goalDone)
        assertTrue(vm.state.value.celebrate)
        assertEquals(1, analytics.names().count { it == FeedEvents.GOAL_COMPLETE })
        vm.consumeCelebration()
        vm.like(vm.state.value.cards[5].item.id, LikeVia.TAP)
        testScheduler.advanceUntilIdle()
        assertFalse(vm.state.value.celebrate)
    }

    @Test fun aLikeCountsTheDayForTheStreak() = runTest {
        prefs.streak = Streak(3, monday.minusOneDay(), 3)
        val vm = started()
        vm.like(vm.state.value.cards[0].item.id, LikeVia.TAP)
        testScheduler.advanceUntilIdle()
        assertEquals(4, vm.state.value.streak)
        assertEquals(Streak(4, monday, 4), prefs.streak)
    }

    @Test fun threeSeenCardsCountTheDayToo() = runTest {
        val vm = started()
        (1..3).forEach { settle(vm, it, afterMs = 2000) }
        assertEquals(1, vm.state.value.streak)
        assertEquals(monday, prefs.streak?.lastActive)
    }

    @Test fun onboardingsStreakDayIsContinued() = runTest {
        answers = FeedOnboarding(streakStart = monday.minusOneDay())
        val vm = started()
        assertEquals(1, vm.state.value.streak)
        vm.like(vm.state.value.cards[0].item.id, LikeVia.TAP)
        testScheduler.advanceUntilIdle()
        assertEquals(2, vm.state.value.streak)
    }

    @Test fun theSwipeHintGivesWayToTheCoachMarkThenTheFirstLikeDismissesIt() = runTest {
        val vm = started()
        assertTrue(vm.state.value.showSwipeHint)
        settle(vm, 1, afterMs = 2000)
        assertFalse(vm.state.value.showSwipeHint)
        assertTrue(vm.state.value.showCoach)
        vm.like(vm.state.value.cards[1].item.id, LikeVia.TAP)
        testScheduler.advanceUntilIdle()
        assertFalse(vm.state.value.showCoach)
        assertTrue(prefs.coachSeen)
    }

    @Test fun aShareIsSeenButNotLiked() = runTest {
        val vm = started()
        val id = vm.state.value.cards[0].item.id
        vm.onShared(id)
        testScheduler.advanceUntilIdle()
        assertEquals(listOf(id), store.seen.map { it.first })
        assertEquals(0, vm.state.value.likesToday)
    }

    @Test fun anUnreadableContentFileShowsTheRetry() = runTest {
        content.fail = true
        assertTrue(started().state.value.failed)
    }

    @Test fun theGuideChipGoesOnceAWidgetIsPlacedOrAWeekHasPassed() = runTest {
        assertTrue(started().state.value.showGuideChip)
        prefs.firstOpen = monday.minusDays(7)
        assertFalse(started().state.value.showGuideChip)
    }

    private fun LocalDate.plusOneDay() = LocalDate.fromEpochDays(toEpochDays() + 1)
    private fun LocalDate.minusOneDay() = LocalDate.fromEpochDays(toEpochDays() - 1)
    private fun LocalDate.minusDays(n: Int) = LocalDate.fromEpochDays(toEpochDays() - n)
}

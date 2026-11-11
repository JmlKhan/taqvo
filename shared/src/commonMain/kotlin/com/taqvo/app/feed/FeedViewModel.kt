package com.taqvo.app.feed

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import com.taqvo.app.analytics.Analytics
import com.taqvo.app.content.CardReference
import com.taqvo.app.content.ContentCatalog
import com.taqvo.app.content.ContentItem
import com.taqvo.app.content.ContentLang
import com.taqvo.app.content.ContentSource
import com.taqvo.app.content.FeedCard
import com.taqvo.app.content.noteOf
import com.taqvo.app.content.translationOf
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/** Everything the feed screen draws. */
data class FeedUiState(
    val loading: Boolean = true,
    /** The day the queue was built for. A new day is a new queue, and the pager starts over with it. */
    val day: LocalDate? = null,
    /** content.db could not be read (feed spec §10): the screen offers a retry. */
    val failed: Boolean = false,
    val cards: List<FeedCard> = emptyList(),
    val position: Int = 0,
    val favorites: Set<String> = emptySet(),
    val likesToday: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** The seven days ending today, oldest first: did each meet the goal, and did each count for the streak. */
    val goalWeek: List<Boolean> = List(7) { false },
    val streakWeek: List<Boolean> = List(7) { false },
    val showSwipeHint: Boolean = false,
    val showCoach: Boolean = false,
    /** The help chips' first seven days (feed spec §2). */
    val showGuideChip: Boolean = false,
    /** True for the one moment the goal is reached today; the screen plays the glow and consumes it. */
    val celebrate: Boolean = false,
) {
    val goalDone: Boolean get() = DailyGoal.done(likesToday)
    val current: FeedCard? get() = cards.getOrNull(position)
}

/**
 * The feed's decisions (feed spec §4–§8): what to show next, what counts as seen, a skip or a like,
 * and what that does to the weights, the daily goal and the streak. The screen reports what the
 * reader did; this class decides what it means and writes it down.
 *
 * One lock around every read-modify-write of the weights and the queue: a like and a page settle
 * arrive from separate coroutines, and the weights they both change must not lose either update.
 */
class FeedViewModel(
    private val content: ContentSource,
    private val store: FeedStore,
    private val prefs: FeedPrefs,
    private val onboarding: suspend () -> FeedOnboarding,
    private val widgetPlaced: suspend () -> Boolean? = { null },
    private val analytics: Analytics,
    /** Called once a day counts for the streak, so the day's streak reminder can be taken back. */
    private val onDayCounted: suspend () -> Unit = {},
    private val engine: FeedEngine = FeedEngine(),
    private val clock: () -> Instant = { Clock.System.now() },
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> }),
) {
    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val lock = Mutex()
    private var lang: ContentLang = ContentLang.FALLBACK
    private var catalog: ContentCatalog? = null
    private var weights: Map<String, Double> = emptyMap()
    private var preferShort = false
    private var day: LocalDate? = null
    private var queue: List<String> = emptyList()
    private var cardOfDay: String? = null

    // The card on screen and since when, for the 1.5 s seen and skip rules; and how many cards this
    // session has shown, for the skip grace and the session event.
    private var shownId: String? = null
    private var shownSince = 0L
    private var sessionCards = 0
    private var sessionStart = 0L

    private fun nowMs() = clock().toEpochMilliseconds()
    private fun today(): LocalDate = clock().toLocalDateTime(zone()).date

    /** Opens the feed in [lang], resuming today's queue where it was left. Safe to call again (retry, a new day). */
    fun start(lang: ContentLang) {
        scope.launch { load(lang) }
    }

    private suspend fun load(newLang: ContentLang) = lock.withLock {
        try {
            lang = newLang
            val catalog = content.catalog().also { this.catalog = it }
            val answers = onboarding()
            preferShort = TopicWeights.prefersShort(answers.contentTypes)
            val stored = store.weights()
            weights = if (stored.isEmpty()) {
                TopicWeights.initial(catalog.topics, answers.goals, answers.contentTypes).also { store.saveWeights(it) }
            } else {
                TopicWeights.complete(stored, catalog.topics)
            }
            val today = today().also { day = it }
            val now = nowMs()
            store.pruneSeen(now - (FeedRules.NO_REPEAT_DAYS.first() + 1) * FeedRules.DAY_MS)
            val saved = store.feedState()?.takeIf { it.day == today }
            val position: Int
            if (saved != null && saved.queue.isNotEmpty()) {
                queue = saved.queue.filter { it in catalog.byId }
                cardOfDay = saved.cardOfDay
                position = saved.position.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
            } else {
                val lastSeen = store.lastSeen(now - FeedRules.NO_REPEAT_DAYS.first() * FeedRules.DAY_MS)
                val first = engine.cardOfDay(catalog, weights, lastSeen, now)
                cardOfDay = first?.id
                val rest = engine.batch(catalog, weights, lastSeen, now, setOfNotNull(first?.id), preferShort)
                queue = listOfNotNull(first?.id) + rest
                position = 0
                store.saveFeedState(FeedState(today, queue, 0, cardOfDay))
            }
            val cards = queue.mapNotNull { catalog.byId[it] }.map { resolve(it) }
            val streak = StreakTracker.seeded(prefs.streak(), answers.streakStart).also { prefs.setStreak(it) }
            val firstDay = prefs.firstOpenDay(today)
            val guide = today < firstDay.plusDays(GUIDE_DAYS) && widgetPlaced() != true
            val hintSeen = prefs.swipeHintSeen()
            _state.update {
                it.copy(
                    loading = false,
                    failed = false,
                    day = today,
                    cards = cards,
                    position = position,
                    showSwipeHint = !hintSeen,
                    showCoach = hintSeen && !prefs.coachSeen(),
                    showGuideChip = guide,
                    streak = StreakTracker.current(streak, today),
                    bestStreak = streak.best,
                )
            }
            refreshCounts()
            shownId = cards.getOrNull(position)?.item?.id
            shownSince = nowMs()
            if (sessionStart == 0L) sessionStart = shownSince
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, failed = true) }
        }
    }

    /** The translations again in [newLang], keeping the queue and the place in it. */
    fun setLanguage(newLang: ContentLang) {
        if (newLang == lang) return
        scope.launch {
            lock.withLock {
                lang = newLang
                val catalog = catalog ?: return@withLock
                val cards = queue.mapNotNull { catalog.byId[it] }.map { resolve(it) }
                _state.update { it.copy(cards = cards) }
            }
        }
    }

    /** The pager came to rest on [index]: the card before it is judged, and the queue refilled if short. */
    fun onSettled(index: Int) {
        scope.launch {
            lock.withLock {
                val cards = _state.value.cards
                val card = cards.getOrNull(index) ?: return@withLock
                if (card.item.id == shownId) return@withLock
                leaveShown()
                shownId = card.item.id
                shownSince = nowMs()
                if (_state.value.showSwipeHint) {
                    prefs.setSwipeHintSeen()
                    _state.update { it.copy(showSwipeHint = false, showCoach = true) }
                }
                _state.update { it.copy(position = index) }
                val today = day ?: today()
                store.saveFeedState(FeedState(today, queue, index, cardOfDay))
                if (cards.size - index - 1 < FeedRules.REFILL_BELOW) refill()
            }
        }
    }

    /**
     * Judges the card going off screen (feed spec §4): seen after 1.5 s, a skip under it once the
     * session's first ten cards are past. Called under [lock].
     */
    private suspend fun leaveShown() {
        val id = shownId ?: return
        val item = catalog?.byId?.get(id) ?: return
        val dwell = nowMs() - shownSince
        sessionCards++
        analytics.track(
            FeedEvents.CARD_VIEW,
            mapOf("item_id" to id, "topic" to item.primaryTopic, "position" to _state.value.position, "dwell_ms" to dwell),
        )
        when {
            dwell >= FeedRules.SEEN_AFTER_MS -> markSeen(id)
            sessionCards > FeedRules.SKIP_GRACE_CARDS -> {
                weights = TopicWeights.adjust(weights, item.topics, TopicWeights.SKIP)
                store.saveWeights(weights)
            }
        }
    }

    private suspend fun refill() {
        val catalog = catalog ?: return
        val now = nowMs()
        val lastSeen = store.lastSeen(now - FeedRules.NO_REPEAT_DAYS.first() * FeedRules.DAY_MS)
        val more = engine.batch(catalog, weights, lastSeen, now, queue.toSet(), preferShort)
        if (more.isEmpty()) return
        queue = queue + more
        val added = more.mapNotNull { catalog.byId[it] }.map { resolve(it) }
        _state.update { it.copy(cards = it.cards + added) }
        store.saveFeedState(FeedState(day ?: today(), queue, _state.value.position, cardOfDay))
    }

    /** The heart, or a double tap (which only ever adds, feed spec §5). */
    fun like(itemId: String, via: LikeVia) {
        scope.launch {
            lock.withLock {
                // A like from a single card opened before the feed has loaded (Settings › Favorites)
                // still adjusts the stored weights rather than a blank set that would overwrite them.
                val catalog = catalog ?: runCatching { content.catalog() }.getOrNull()?.also { this@FeedViewModel.catalog = it } ?: return@withLock
                if (weights.isEmpty()) weights = TopicWeights.complete(store.weights(), catalog.topics)
                val item = catalog.byId[itemId] ?: return@withLock
                // The store, not only the state: a single card opened before the feed loaded has no
                // favorites in the state yet, and a second like would raise its topics twice.
                if (itemId in _state.value.favorites || itemId in store.favorites()) return@withLock
                val today = today()
                val before = _state.value.likesToday
                store.addFavorite(itemId, nowMs())
                store.countLike(today, itemId)
                weights = TopicWeights.adjust(weights, item.topics, TopicWeights.LIKE)
                store.saveWeights(weights)
                markSeen(itemId)
                analytics.track(FeedEvents.LIKE, mapOf("item_id" to itemId, "topic" to item.primaryTopic, "via" to via.id))
                if (_state.value.showCoach) {
                    prefs.setCoachSeen()
                    _state.update { it.copy(showCoach = false) }
                }
                _state.update { it.copy(favorites = it.favorites + itemId) }
                val after = _state.value.likesToday
                if (!DailyGoal.done(before) && DailyGoal.done(after) && prefs.celebratedOn() != today) {
                    prefs.setCelebratedOn(today)
                    analytics.track(FeedEvents.GOAL_COMPLETE, mapOf("date" to today.toString()))
                    _state.update { it.copy(celebrate = true) }
                }
            }
        }
    }

    /** Takes the item out of favorites. The weights and today's goal keep the like (feed spec §5). */
    fun unlike(itemId: String) {
        scope.launch {
            lock.withLock {
                store.removeFavorite(itemId)
                analytics.track(FeedEvents.UNLIKE, mapOf("item_id" to itemId))
                _state.update { it.copy(favorites = it.favorites - itemId) }
            }
        }
    }

    fun toggleLike(itemId: String) {
        if (itemId in _state.value.favorites) unlike(itemId) else like(itemId, LikeVia.TAP)
    }

    /** A save or a share counts the item as seen, never as liked (feed spec §9). */
    fun onShared(itemId: String) {
        scope.launch { lock.withLock { markSeen(itemId) } }
    }

    fun consumeCelebration() = _state.update { it.copy(celebrate = false) }

    /** The app went to the background: the card on screen is judged now, and the session reported. */
    fun onBackground() {
        scope.launch {
            lock.withLock {
                leaveShown()
                shownId = null
                // Leaving the feed and the app stopping both land here; only the first ends a session.
                if (sessionStart != 0L) {
                    analytics.track(FeedEvents.SESSION, mapOf("cards_viewed" to sessionCards, "duration_ms" to nowMs() - sessionStart))
                }
                sessionCards = 0
                sessionStart = 0L
            }
        }
    }

    /** The app is back: a new day starts a fresh queue; the same day picks the card on screen up again. */
    fun onForeground() {
        if (day != null && day != today()) {
            start(lang)
            return
        }
        scope.launch {
            lock.withLock {
                if (shownId == null) {
                    shownId = _state.value.current?.item?.id
                    shownSince = nowMs()
                    sessionStart = shownSince
                }
            }
        }
    }

    /** Favorites, today's likes and the streak, read again; for a screen that changed them elsewhere. */
    fun refresh() {
        scope.launch { lock.withLock { refreshCounts() } }
    }

    /** Writes a view, then lets the day count toward the streak. Called under [lock]. */
    private suspend fun markSeen(itemId: String) {
        // The date now, not the queue's: a feed left open past midnight counts the new day.
        val today = today()
        store.markSeen(itemId, nowMs(), today)
        refreshCounts()
    }

    private suspend fun refreshCounts() {
        val today = today()
        val activity = store.activitySince(today.minus(DatePeriod(days = 6)))
        val todays = activity[today] ?: DayActivity()
        var streak = StreakTracker.seeded(prefs.streak(), null)
        val recorded = if (StreakTracker.dayCounts(todays)) StreakTracker.record(streak, today) else streak
        // Only a day that actually moved the streak is an event: with the clock set back, record()
        // leaves it as it was, and that must not reschedule the notifications on every card.
        if (recorded != streak) {
            streak = recorded
            prefs.setStreak(streak)
            analytics.track(FeedEvents.STREAK_DAY, mapOf("count" to streak.count))
            // Outside the lock: a reschedule reads app.db and must not hold the feed's swipes up.
            scope.launch { runCatching { onDayCounted() } }
        }
        val week = StreakTracker.week(today, activity)
        val favorites = store.favorites().toSet()
        _state.update {
            it.copy(
                favorites = favorites,
                likesToday = todays.likes,
                streak = StreakTracker.current(streak, today),
                bestStreak = streak.best,
                streakWeek = week.map { (_, counted) -> counted },
                goalWeek = week.map { (d, _) -> DailyGoal.done(activity[d]?.likes ?: 0) },
            )
        }
    }

    private suspend fun resolve(item: ContentItem): FeedCard {
        val (text, textLang) = content.translationOf(item, lang) ?: (null to lang)
        return FeedCard(item, text, CardReference.of(item), textLang, content.noteOf(item, textLang))
    }

    private fun LocalDate.plusDays(days: Int): LocalDate = this.minus(DatePeriod(days = -days))

    private companion object {
        const val GUIDE_DAYS = 7
    }
}

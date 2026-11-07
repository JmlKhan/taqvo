package com.taqvo.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import com.taqvo.app.feed.ArabicFont
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.feed.Streak
import com.taqvo.app.feed.TextAlignment
import com.taqvo.app.feed.TextColor
import com.taqvo.app.feed.TranslationFont

/**
 * The feed's small settings and one-time flags, in the app's DataStore beside [OnboardingStore]:
 * the text style, the streak, and which first-launch hints have been shown. The lists of items —
 * history, favorites, collections — are in app.db ([com.taqvo.app.feed.FeedStore]).
 */
class FeedPrefsStore(private val store: DataStore<Preferences>) {

    val textStyle: Flow<FeedTextStyle> = store.data.map { prefs ->
        FeedTextStyle(
            translationFont = TranslationFont.of(prefs[FONT]),
            arabicFont = ArabicFont.of(prefs[ARABIC_FONT]),
            sizePct = (prefs[SIZE] ?: 100).coerceIn(FeedTextStyle.MIN_SIZE, FeedTextStyle.MAX_SIZE),
            color = TextColor.of(prefs[COLOR]),
            align = TextAlignment.of(prefs[ALIGN]),
        )
    }

    suspend fun setTextStyle(style: FeedTextStyle) = store.edit { prefs ->
        prefs[FONT] = style.translationFont.id
        prefs[ARABIC_FONT] = style.arabicFont.id
        prefs[SIZE] = style.sizePct
        prefs[COLOR] = style.color.id
        prefs[ALIGN] = style.align.id
    }

    /** Null until the feed has written one; [com.taqvo.app.feed.StreakTracker.seeded] fills the gap. */
    suspend fun streak(): Streak? {
        val prefs = store.data.first()
        val count = prefs[STREAK_COUNT] ?: return null
        val last = prefs[STREAK_LAST]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return Streak(count = count, lastActive = last, best = prefs[STREAK_BEST] ?: count)
    }

    suspend fun setStreak(streak: Streak) = store.edit { prefs ->
        prefs[STREAK_COUNT] = streak.count
        prefs[STREAK_BEST] = streak.best
        val last = streak.lastActive
        if (last == null) prefs.remove(STREAK_LAST) else prefs[STREAK_LAST] = last.toString()
    }

    /** The first day the feed was opened: the help chips show for seven days from it (feed spec §2). */
    suspend fun firstOpenDay(today: LocalDate): LocalDate {
        store.data.first()[FIRST_OPEN]?.let { stored -> runCatching { LocalDate.parse(stored) }.getOrNull()?.let { return it } }
        store.edit { it[FIRST_OPEN] = today.toString() }
        return today
    }

    suspend fun swipeHintSeen(): Boolean = store.data.first()[SWIPE_HINT] ?: false
    suspend fun setSwipeHintSeen() = store.edit { it[SWIPE_HINT] = true }

    suspend fun coachSeen(): Boolean = store.data.first()[COACH] ?: false
    suspend fun setCoachSeen() = store.edit { it[COACH] = true }

    /** The last day the goal's celebration played, so it plays once a day (feed spec §6). */
    suspend fun celebratedOn(): LocalDate? = store.data.first()[CELEBRATED]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    suspend fun setCelebratedOn(day: LocalDate) = store.edit { it[CELEBRATED] = day.toString() }

    /** The feed's background, chosen on the Mavzular screen. */
    val themeId: Flow<String> = store.data.map { it[THEME] ?: com.taqvo.app.feed.FeedThemes.DEFAULT_ID }
    suspend fun setThemeId(id: String) = store.edit { it[THEME] = id }

    /** The home screen's two help chips, each closed for good by its own link ("Tayyor", "Yopish"). */
    val guideChipClosed: Flow<Boolean> = store.data.map { it[GUIDE_CLOSED] ?: false }
    suspend fun closeGuideChip() = store.edit { it[GUIDE_CLOSED] = true }
    val helpChipClosed: Flow<Boolean> = store.data.map { it[HELP_CLOSED] ?: false }
    suspend fun closeHelpChip() = store.edit { it[HELP_CLOSED] = true }

    /** Whether the share sheet's watermark is shown (feed spec §9). */
    val watermark: Flow<Boolean> = store.data.map { it[WATERMARK] ?: true }
    suspend fun setWatermark(shown: Boolean) = store.edit { it[WATERMARK] = shown }

    private companion object {
        val FONT = stringPreferencesKey("feed_font")
        val ARABIC_FONT = stringPreferencesKey("feed_arabic_font")
        val SIZE = intPreferencesKey("feed_size_pct")
        val COLOR = stringPreferencesKey("feed_color")
        val ALIGN = stringPreferencesKey("feed_align")
        val STREAK_COUNT = intPreferencesKey("feed_streak_count")
        val STREAK_BEST = intPreferencesKey("feed_streak_best")
        val STREAK_LAST = stringPreferencesKey("feed_streak_last")
        val FIRST_OPEN = stringPreferencesKey("feed_first_open")
        val SWIPE_HINT = booleanPreferencesKey("feed_swipe_hint_seen")
        val COACH = booleanPreferencesKey("feed_coach_seen")
        val CELEBRATED = stringPreferencesKey("feed_celebrated_on")
        val WATERMARK = booleanPreferencesKey("feed_watermark")
        val THEME = stringPreferencesKey("feed_theme")
        val GUIDE_CLOSED = booleanPreferencesKey("feed_guide_chip_closed")
        val HELP_CLOSED = booleanPreferencesKey("feed_help_chip_closed")
    }
}

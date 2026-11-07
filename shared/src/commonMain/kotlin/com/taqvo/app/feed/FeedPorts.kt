package com.taqvo.app.feed

import kotlinx.datetime.LocalDate
import com.taqvo.app.settings.FeedPrefsStore

/** The part of [FeedPrefsStore] the feed's view model uses, so its tests can hand it a fake. */
interface FeedPrefs {
    suspend fun streak(): Streak?
    suspend fun setStreak(streak: Streak)
    suspend fun firstOpenDay(today: LocalDate): LocalDate
    suspend fun swipeHintSeen(): Boolean
    suspend fun setSwipeHintSeen()
    suspend fun coachSeen(): Boolean
    suspend fun setCoachSeen()
    suspend fun celebratedOn(): LocalDate?
    suspend fun setCelebratedOn(day: LocalDate)
}

fun FeedPrefsStore.asFeedPrefs(): FeedPrefs = object : FeedPrefs {
    override suspend fun streak() = this@asFeedPrefs.streak()
    override suspend fun setStreak(streak: Streak) { this@asFeedPrefs.setStreak(streak) }
    override suspend fun firstOpenDay(today: LocalDate) = this@asFeedPrefs.firstOpenDay(today)
    override suspend fun swipeHintSeen() = this@asFeedPrefs.swipeHintSeen()
    override suspend fun setSwipeHintSeen() { this@asFeedPrefs.setSwipeHintSeen() }
    override suspend fun coachSeen() = this@asFeedPrefs.coachSeen()
    override suspend fun setCoachSeen() { this@asFeedPrefs.setCoachSeen() }
    override suspend fun celebratedOn() = this@asFeedPrefs.celebratedOn()
    override suspend fun setCelebratedOn(day: LocalDate) { this@asFeedPrefs.setCelebratedOn(day) }
}

/** What onboarding told the app, read when the feed first ranks (content spec §4, feed spec §8). */
data class FeedOnboarding(
    val goals: List<String> = emptyList(),
    val contentTypes: List<String> = emptyList(),
    val streakStart: LocalDate? = null,
)

/** The feed's analytics events (feed spec §13). Ids and counts only, never a name or a collection's title. */
object FeedEvents {
    const val CARD_VIEW = "feed_card_view"
    const val LIKE = "feed_like"
    const val UNLIKE = "feed_unlike"
    const val SHARE_OPEN = "feed_share_open"
    const val SHARE = "feed_share"
    const val STYLE_CHANGE = "feed_style_change"
    const val GOAL_COMPLETE = "daily_goal_complete"
    const val STREAK_DAY = "streak_day"
    const val SESSION = "feed_session"
}

/** How a like arrived, for [FeedEvents.LIKE]. */
enum class LikeVia(val id: String) { TAP("tap"), DOUBLE_TAP("double_tap") }

/** What was done from the share sheet, for [FeedEvents.SHARE]. */
enum class ShareAction(val id: String) {
    SHARE("share"),
    SAVE_IMAGE("save_image"),
    COPY("copy"),
    COLLECTION("collection"),
    STYLE("style"),
}

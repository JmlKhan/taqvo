package com.taqvo.app.feature.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.components.drawCheck
import com.taqvo.app.design.components.drawFlame
import com.taqvo.app.feed.DailyGoal
import com.taqvo.app.feed.FeedUiState
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_a11y_day_done
import com.taqvo.app.resources.feed_a11y_day_open
import com.taqvo.app.resources.feed_goal_body
import com.taqvo.app.resources.feed_goal_title
import com.taqvo.app.resources.feed_goal_week
import com.taqvo.app.resources.feed_open_favorites
import com.taqvo.app.resources.feed_streak_best
import com.taqvo.app.resources.feed_streak_body
import com.taqvo.app.resources.feed_streak_title
import com.taqvo.app.resources.feed_streak_week

/** Tapping the goal pill (feed spec §6): today's progress, the week's goals, and the way to Favorites. */
@Composable
fun GoalSheet(state: FeedUiState, today: LocalDate, onFavorites: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalTaqvoColors.current
    TaqvoBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.feed_goal_title), style = TaqvoText.screenTitle, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(16.dp))
            Text(
                "${state.likesToday.coerceAtMost(DailyGoal.TARGET)} / ${DailyGoal.TARGET}",
                style = TaqvoText.countdown,
                color = if (state.goalDone) colors.accent else colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.feed_goal_body), style = TaqvoText.caption, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(Res.string.feed_goal_week), style = TaqvoText.sectionLabel, color = colors.textTertiary)
            Spacer(Modifier.height(10.dp))
            WeekRow(today, state.goalWeek)
            Spacer(Modifier.height(28.dp))
            TaqvoPrimaryButton(stringResource(Res.string.feed_open_favorites), onClick = onFavorites)
        }
    }
}

/** Tapping the flame (feed spec §8): the count, this week, the best run. */
@Composable
fun StreakSheet(state: FeedUiState, today: LocalDate, onDismiss: () -> Unit) {
    val colors = LocalTaqvoColors.current
    TaqvoBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(40.dp).clearAndSetSemantics {}) { drawFlame(colors.accent) }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.feed_streak_title, state.streak), style = TaqvoText.screenTitle, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.feed_streak_body), style = TaqvoText.caption, color = colors.textSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(Res.string.feed_streak_week), style = TaqvoText.sectionLabel, color = colors.textTertiary)
            Spacer(Modifier.height(10.dp))
            WeekRow(today, state.streakWeek)
            Spacer(Modifier.height(20.dp))
            Text(stringResource(Res.string.feed_streak_best, state.bestStreak), style = TaqvoText.rowLabel, color = colors.textSecondary)
        }
    }
}

/** Seven days ending today, oldest first: a filled check for a day done, an empty ring for the rest. */
@Composable
private fun WeekRow(today: LocalDate, done: List<Boolean>) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    val doneLabel = stringResource(Res.string.feed_a11y_day_done)
    val openLabel = stringResource(Res.string.feed_a11y_day_open)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        done.forEachIndexed { i, isDone ->
            val day = today.minus(DatePeriod(days = done.size - 1 - i))
            val dayNumber = format.localizedDigits(day.day)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "$dayNumber, ${if (isDone) doneLabel else openLabel}"
                },
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isDone) colors.accent else colors.background)
                        .border(1.5.dp, if (isDone) colors.accent else colors.hairline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) Canvas(Modifier.size(16.dp)) { drawCheck(colors.background) }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    dayNumber,
                    style = TaqvoText.caption.copy(fontSize = 12.sp),
                    color = if (day == today) colors.textPrimary else colors.textTertiary,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

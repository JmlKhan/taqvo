package com.taqvo.app.feature.feed

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.FeedInk
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.drawBell
import com.taqvo.app.design.components.drawCards
import com.taqvo.app.design.components.drawCheck
import com.taqvo.app.design.components.drawFlame
import com.taqvo.app.design.components.drawGrid
import com.taqvo.app.design.components.drawHeart
import com.taqvo.app.design.components.drawPicture
import com.taqvo.app.design.components.drawQuestion
import com.taqvo.app.design.components.drawShare
import com.taqvo.app.design.components.drawTextStyle
import com.taqvo.app.feed.DailyGoal
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_a11y_goal
import com.taqvo.app.resources.feed_a11y_like
import com.taqvo.app.resources.feed_a11y_reminders
import com.taqvo.app.resources.feed_a11y_share
import com.taqvo.app.resources.feed_a11y_streak
import com.taqvo.app.resources.feed_a11y_style
import com.taqvo.app.resources.feed_a11y_unlike
import com.taqvo.app.resources.feed_chip_close
import com.taqvo.app.resources.feed_chip_done
import com.taqvo.app.resources.feed_chip_help
import com.taqvo.app.resources.feed_chip_how
import com.taqvo.app.resources.feed_coach
import com.taqvo.app.resources.feed_goal_done
import com.taqvo.app.resources.feed_nav_themes
import com.taqvo.app.resources.feed_nav_tools
import com.taqvo.app.resources.feed_swipe_hint
import com.taqvo.app.resources.tab_feed

object FeedChromeSizes {
    val ICON = 22.dp
    /** Every tap target over the photo is at least this (feed spec §2). */
    val TARGET = 48.dp
    /** What the card's words keep clear of above (the bell and chips) and below (actions, nav). */
    val TOP_SPACE = 200.dp
    val BOTTOM_SPACE = 190.dp
}

private val chromeShadow = Shadow(color = FeedInk.shadow, blurRadius = 6f)

/**
 * The home screen's top (the app structure the owner drew): on the left the reminders bell and
 * the two help chips — how to add a widget, and help — each closed for good by the link under it;
 * on the right today's goal and the streak.
 */
@Composable
fun FeedTopBar(
    likesToday: Int,
    streak: Int,
    showGuideChip: Boolean,
    showHelpChip: Boolean,
    celebrating: Boolean,
    actions: FeedTopActions,
    modifier: Modifier = Modifier,
) {
    val ink = ChromeInk
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.Top) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(stringResource(Res.string.feed_a11y_reminders), actions.onReminders) { drawBell(ink) }
            if (showGuideChip) {
                Column {
                    Chip(stringResource(Res.string.feed_chip_how), actions.onGuide) { drawQuestion(ink) }
                    DismissLink(stringResource(Res.string.feed_chip_done), actions.onGuideDone)
                }
            }
            if (showHelpChip) {
                Column {
                    Chip(stringResource(Res.string.feed_chip_help), actions.onHelp) { drawQuestion(ink) }
                    DismissLink(stringResource(Res.string.feed_chip_close), actions.onHelpClose)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StreakBadge(ink, streak, actions.onStreak)
            GoalPill(ink, likesToday, celebrating, actions.onGoal)
        }
    }
}

class FeedTopActions(
    val onReminders: () -> Unit,
    val onGuide: () -> Unit,
    val onGuideDone: () -> Unit,
    val onHelp: () -> Unit,
    val onHelpClose: () -> Unit,
    val onGoal: () -> Unit,
    val onStreak: () -> Unit,
)

/** A square of the deep green glass with one glyph: the bell. */
@Composable
private fun GlassButton(label: String, onClick: () -> Unit, glyph: DrawScope.() -> Unit) {
    Box(
        Modifier
            .size(FeedChromeSizes.TARGET + 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FeedInk.glass)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) { Canvas(Modifier.size(FeedChromeSizes.ICON)) { glyph() } }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit, glyph: DrawScope.() -> Unit) {
    Row(
        Modifier
            .heightIn(min = FeedChromeSizes.TARGET)
            .clip(RoundedCornerShape(14.dp))
            .background(FeedInk.glass)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(20.dp).clearAndSetSemantics {}) { glyph() }
        Spacer(Modifier.width(8.dp))
        Text(label, color = ChromeInk, style = TaqvoText.rowLabel.copy(fontSize = 16.sp))
    }
}

/** The small underlined link under a chip that closes it for good. */
@Composable
private fun DismissLink(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = ChromeInk.copy(alpha = 0.75f),
        style = TaqvoText.caption.copy(textDecoration = TextDecoration.Underline, shadow = chromeShadow),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 2.dp, vertical = 8.dp),
    )
}

@Composable
private fun GoalPill(ink: Color, likesToday: Int, celebrating: Boolean, onClick: () -> Unit) {
    val done = DailyGoal.done(likesToday)
    val label = stringResource(Res.string.feed_a11y_goal, likesToday.coerceAtMost(DailyGoal.TARGET), DailyGoal.TARGET)
    val glow by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
    )
    Row(
        Modifier
            .heightIn(min = FeedChromeSizes.TARGET)
            .clip(RoundedCornerShape(14.dp))
            .background(if (celebrating) FeedInk.amber.copy(alpha = 0.35f + 0.35f * glow) else FeedInk.glass)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp).clearAndSetSemantics {}) { if (done) drawCheck(ink) else drawHeart(ink, filled = likesToday > 0) }
        Spacer(Modifier.width(6.dp))
        if (done) {
            Text(stringResource(Res.string.feed_goal_done), color = ink, style = TaqvoText.caption.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.clearAndSetSemantics {})
        } else {
            Text("$likesToday/${DailyGoal.TARGET}", color = ink, style = TaqvoText.caption.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.clearAndSetSemantics {})
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(32.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ink.copy(alpha = 0.3f))) {
                Box(Modifier.width(32.dp * DailyGoal.progress(likesToday)).height(4.dp).clip(RoundedCornerShape(2.dp)).background(ink))
            }
        }
    }
}

@Composable
private fun StreakBadge(ink: Color, streak: Int, onClick: () -> Unit) {
    val label = stringResource(Res.string.feed_a11y_streak, streak)
    Row(
        Modifier
            .heightIn(min = FeedChromeSizes.TARGET)
            .widthIn(min = FeedChromeSizes.TARGET)
            .clip(RoundedCornerShape(14.dp))
            .background(FeedInk.glass)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label; role = Role.Button }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Canvas(Modifier.size(ICON_SMALL).clearAndSetSemantics {}) { drawFlame(if (streak > 0) FeedInk.amber else ink) }
        Spacer(Modifier.width(4.dp))
        Text("$streak", color = ink, style = TaqvoText.rowLabel, modifier = Modifier.clearAndSetSemantics {})
    }
}

private val ICON_SMALL = 20.dp

/** Share · like · text style, centred over the navigation (feed spec §2). */
@Composable
fun FeedActionRow(
    ink: Color,
    liked: Boolean,
    onShare: () -> Unit,
    onLike: () -> Unit,
    onStyle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally)) {
        IconTarget(stringResource(Res.string.feed_a11y_share), onShare, size = 56.dp, iconSize = 28.dp) { drawShare(ink) }
        IconTarget(
            stringResource(if (liked) Res.string.feed_a11y_unlike else Res.string.feed_a11y_like),
            onLike,
            size = 56.dp,
            iconSize = 32.dp,
        ) { drawHeart(if (liked) FeedInk.heart else ink, filled = liked) }
        IconTarget(stringResource(Res.string.feed_a11y_style), onStyle, size = 56.dp, iconSize = 28.dp) { drawTextStyle(ink) }
    }
}

/**
 * The home screen's navigation (the owner's structure): three floating pills — the feed itself,
 * the themes (Mavzular), and every other part of the app (Ilovalar). There is no tab bar.
 */
@Composable
fun FeedNavBar(onFeed: () -> Unit, onThemes: () -> Unit, onTools: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
        NavPill(stringResource(Res.string.tab_feed), onFeed) { drawCards(ChromeInk) }
        NavPill(stringResource(Res.string.feed_nav_themes), onThemes) { drawPicture(ChromeInk) }
        NavPill(stringResource(Res.string.feed_nav_tools), onTools) { drawGrid(ChromeInk) }
    }
}

@Composable
private fun NavPill(label: String, onClick: () -> Unit, glyph: DrawScope.() -> Unit) {
    Row(
        Modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(FeedInk.glass)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(18.dp).clearAndSetSemantics {}) { glyph() }
        Spacer(Modifier.width(8.dp))
        Text(label, color = ChromeInk, style = TaqvoText.rowLabel.copy(fontSize = 16.sp), maxLines = 1)
    }
}

@Composable
fun IconTarget(
    label: String,
    onClick: () -> Unit,
    size: Dp = FeedChromeSizes.TARGET,
    iconSize: Dp = FeedChromeSizes.ICON,
    glyph: DrawScope.() -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label; role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(iconSize)) { glyph() }
    }
}

/** First launch only: "Swipe up" with a chevron, bobbing, until the first swipe (feed spec §4). */
@Composable
fun SwipeHint(ink: Color, modifier: Modifier = Modifier) {
    val bob by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
    )
    Column(modifier.offset(y = bob.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(20.dp)) {
            val u = size.width / 16f
            drawLine(ink, Offset(3f * u, 11f * u), Offset(8f * u, 5f * u), strokeWidth = 2.dp.toPx())
            drawLine(ink, Offset(8f * u, 5f * u), Offset(13f * u, 11f * u), strokeWidth = 2.dp.toPx())
        }
        Text(stringResource(Res.string.feed_swipe_hint), color = ink, style = TaqvoText.caption.copy(fontWeight = FontWeight.SemiBold, shadow = chromeShadow))
    }
}

/** The tooltip over the heart (feed spec §6), until the first like. */
@Composable
fun CoachMark(modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.feed_coach),
        modifier = modifier
            .widthIn(max = 300.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(FeedInk.cream)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        color = FeedInk.black,
        style = TaqvoText.caption.copy(fontSize = 14.sp, textAlign = TextAlign.Center),
    )
}

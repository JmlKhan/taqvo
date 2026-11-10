package com.taqvo.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.FeedCard
import com.taqvo.app.design.FeedInk
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.feed.FeedRules
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.feed.FeedTheme
import com.taqvo.app.feed.FeedUiState
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_error
import com.taqvo.app.resources.feed_retry

/** What the feed screen can be asked to do; the route turns each into a view model call, a sheet or a screen. */
class FeedActions(
    val onSettled: (Int) -> Unit,
    val onLike: (FeedCard) -> Unit,
    val onDoubleTap: (FeedCard) -> Unit,
    val onShare: (FeedCard) -> Unit,
    val onStyle: () -> Unit,
    val top: FeedTopActions,
    val onThemes: () -> Unit,
    val onTools: () -> Unit,
    val onRetry: () -> Unit,
    val onCelebrated: () -> Unit,
)

/**
 * The home screen (the owner's structure): the feed, full screen over the chosen theme's photo,
 * one card a swipe. The bell and the help chips are top left, the goal and streak top right; the
 * card's actions and the three navigation pills float at the foot. There is no tab bar: the rest
 * of the app is reached from Ilovalar, and the themes from Mavzular.
 */
@Composable
fun FeedScreen(
    state: FeedUiState,
    format: AyahFormat,
    style: FeedTextStyle,
    theme: FeedTheme,
    showGuideChip: Boolean,
    showHelpChip: Boolean,
    actions: FeedActions,
) {
    val ink = ChromeInk
    val fonts = rememberFeedFonts(style)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    // The words keep clear of the bell, and of each help chip while it is shown.
    val topSpace = 84.dp + (if (showGuideChip) 92.dp else 0.dp) + (if (showHelpChip) 92.dp else 0.dp)
    Box(Modifier.fillMaxSize().background(FeedInk.deep)) {
        // The photo holds still behind the pager, so only the words move.
        FeedBackground(theme, Modifier.fillMaxSize())
        // Keyed on the queue's day: a new day's queue starts at its first card, not at the page
        // the old queue was on.
        val pager = if (!state.failed && !state.loading && state.cards.isNotEmpty()) {
            key(state.day) { rememberPagerState(initialPage = state.position) { state.cards.size } }
        } else {
            null
        }
        when {
            state.failed -> FeedError(actions.onRetry, Modifier.align(Alignment.Center))
            pager != null -> {
                LaunchedEffect(pager) {
                    snapshotFlow { pager.settledPage }.distinctUntilChanged().collect(actions.onSettled)
                }
                VerticalPager(
                    state = pager,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = FeedRules.PRELOAD,
                    key = { state.cards[it].item.id },
                ) { page ->
                    val card = state.cards[page]
                    FeedCardView(
                        card = card,
                        format = format,
                        style = style,
                        fonts = fonts,
                        topSpace = topSpace,
                        bottomSpace = FeedChromeSizes.BOTTOM_SPACE,
                        onDoubleTap = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            actions.onDoubleTap(card)
                        },
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                    )
                }
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val current = pager?.let { state.cards.getOrNull(it.currentPage) }
            if (current != null) {
                if (state.showSwipeHint) {
                    SwipeHint(ink)
                    Spacer(Modifier.height(12.dp))
                } else if (state.showCoach) {
                    CoachMark()
                    Spacer(Modifier.height(10.dp))
                }
                FeedActionRow(
                    ink = ink,
                    liked = current.item.id in state.favorites,
                    onShare = { actions.onShare(current) },
                    onLike = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        actions.onLike(current)
                    },
                    onStyle = actions.onStyle,
                )
                Spacer(Modifier.height(28.dp))
            }
            FeedNavBar(
                // Already home: Tasma takes the reader back to the first card of the day.
                onFeed = { pager?.let { scope.launch { it.animateScrollToPage(0) } } },
                onThemes = actions.onThemes,
                onTools = actions.onTools,
            )
        }
        FeedTopBar(
            likesToday = state.likesToday,
            streak = state.streak,
            showGuideChip = showGuideChip,
            showHelpChip = showHelpChip,
            celebrating = state.celebrate,
            actions = actions.top,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(top = 8.dp),
        )
    }
    // The goal's glow plays for a moment, once (feed spec §6).
    LaunchedEffect(state.celebrate) {
        if (state.celebrate) {
            delay(CELEBRATION_MS)
            actions.onCelebrated()
        }
    }
}

@Composable
private fun FeedError(onRetry: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.feed_error), color = ChromeInk, style = TaqvoText.rowLabel)
        TaqvoPrimaryButton(stringResource(Res.string.feed_retry), onClick = onRetry)
    }
}

private const val CELEBRATION_MS = 2_400L

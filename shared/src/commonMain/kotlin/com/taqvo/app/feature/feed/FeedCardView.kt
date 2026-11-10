package com.taqvo.app.feature.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.FeedCard
import com.taqvo.app.design.FeedInk
import com.taqvo.app.design.components.drawHeart
import com.taqvo.app.feed.FeedTextStyle

/**
 * One card's words, centred in the space between the top bar and the action row (feed spec §3).
 *
 * The text starts at the reader's size and steps down until it fits, to no less than 70% of it
 * (feed spec §3.2); an ayah still too long then scrolls inside the card. The pager is the parent
 * of that scroll, so a swipe scrolls the card first and turns the page only once the card's end is
 * reached — Compose's nested scrolling does that without any help here.
 *
 * A double tap likes the card and never un-likes it (feed spec §5); the heart blooms where the
 * card is, whether or not it was liked already, so the gesture always answers.
 */
@Composable
fun FeedCardView(
    card: FeedCard,
    format: AyahFormat,
    style: FeedTextStyle,
    fonts: FeedFonts,
    topSpace: Dp,
    bottomSpace: Dp,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var bloom by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(card.item.id) {
                detectTapGestures(onDoubleTap = {
                    bloom++
                    onDoubleTap()
                })
            },
    ) {
        BoxWithConstraints(
            // The same room above and below, so the words sit in the middle of the photo, not of
            // the gap the buttons leave.
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = maxOf(topSpace, bottomSpace)),
        ) {
            val ink = style.color.ink()
            val align = style.textAlign()
            val showArabic = format.showsArabic()
            // A card with no translation in any language still shows its Arabic, whatever the format.
            val showTranslation = (format.showsTranslation() || !showArabic) && card.translation != null
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val widthPx = constraints.maxWidth
            val heightPx = constraints.maxHeight
            val base = style.sizePct / 100f
            // The largest scale, in FIT_STEP steps from the reader's own, at which the block fits.
            val (scale, fits) = remember(card.item.id, card.translation, style, format, widthPx, heightPx, fonts, density) {
                fun height(s: Float): Int {
                    val c = Constraints(maxWidth = widthPx)
                    var h = 0
                    if (showArabic) h += measurer.measure(card.item.arabic, arabicTextStyle(fonts, FeedTypeScale.ARABIC_SP * s, ink, align), constraints = c, density = density).size.height
                    if (showTranslation) h += measurer.measure(card.translation.orEmpty(), translationTextStyle(fonts, FeedTypeScale.TRANSLATION_SP * s, ink, align), constraints = c, density = density).size.height
                    h += with(density) { (GAP * 2 + 24.dp).roundToPx() }
                    return h
                }
                var s = base
                val floor = base * FeedTypeScale.MIN_FIT
                while (s > floor && height(s) > heightPx) s = (s - FeedTypeScale.FIT_STEP).coerceAtLeast(floor)
                s to (height(s) <= heightPx)
            }
            val shadow = Shadow(color = FeedInk.shadow, blurRadius = 8f)
            val scroll = rememberScrollState()
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .then(if (fits) Modifier else Modifier.verticalScroll(scroll))
                    .semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                if (showArabic) {
                    Text(
                        card.item.arabic,
                        // A screen reader reads the translation, then the reference (feed spec §10);
                        // the Arabic is spoken only when it is all the card shows.
                        modifier = Modifier.fillMaxWidth().then(if (showTranslation) Modifier.clearAndSetSemantics {} else Modifier),
                        style = arabicTextStyle(fonts, FeedTypeScale.ARABIC_SP * scale, ink, align).withShadow(shadow),
                    )
                }
                if (showArabic && showTranslation) Spacer(Modifier.height(GAP))
                if (showTranslation) {
                    Text(
                        card.translation.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        style = translationTextStyle(fonts, FeedTypeScale.TRANSLATION_SP * scale, ink, align).withShadow(shadow),
                    )
                }
                Spacer(Modifier.height(GAP))
                Text(
                    "— ${card.reference}",
                    modifier = Modifier.fillMaxWidth(),
                    style = referenceTextStyle(fonts, FeedTypeScale.REFERENCE_SP, ink, align).withShadow(shadow),
                )
            }
        }
        HeartBloom(bloom, Modifier.align(Alignment.Center))
    }
}

private fun TextStyle.withShadow(shadow: Shadow) = copy(shadow = shadow)

/** The heart that blooms on a double tap: grows and fades over about 400 ms (feed spec §5). */
@Composable
private fun HeartBloom(trigger: Int, modifier: Modifier) {
    if (trigger == 0) return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 450))
    }
    val p = progress.value
    if (p >= 1f) return
    Canvas(
        modifier
            .size(112.dp)
            .scale(0.6f + 0.6f * p)
            .alpha(1f - p)
            .clearAndSetSemantics {},
    ) { drawHeart(FeedInk.heart, filled = true) }
}

private val GAP = 20.dp

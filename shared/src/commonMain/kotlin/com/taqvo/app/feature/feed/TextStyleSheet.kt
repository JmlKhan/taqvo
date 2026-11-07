package com.taqvo.app.feature.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.FeedCard
import com.taqvo.app.design.FeedInk
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.TaqvoSegmented
import com.taqvo.app.feature.settings.SectionLabel
import com.taqvo.app.feed.FeedPremium
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.feed.TextAlignment
import com.taqvo.app.feed.TextColor
import com.taqvo.app.feed.TranslationFont
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.app_name
import com.taqvo.app.resources.feed_color_amber
import com.taqvo.app.resources.feed_color_auto
import com.taqvo.app.resources.feed_color_black
import com.taqvo.app.resources.feed_color_cream
import com.taqvo.app.resources.feed_color_rose
import com.taqvo.app.resources.feed_color_sage
import com.taqvo.app.resources.feed_color_white
import com.taqvo.app.resources.feed_style_align
import com.taqvo.app.resources.feed_style_align_center
import com.taqvo.app.resources.feed_style_align_start
import com.taqvo.app.resources.feed_style_color
import com.taqvo.app.resources.feed_style_font
import com.taqvo.app.resources.feed_style_font_system
import com.taqvo.app.resources.feed_style_format
import com.taqvo.app.resources.feed_style_size
import com.taqvo.app.resources.feed_style_title
import com.taqvo.app.resources.onb_format_arabic
import com.taqvo.app.resources.onb_format_both
import com.taqvo.app.resources.onb_format_translation
import com.taqvo.app.feed.FeedTheme
import org.jetbrains.compose.resources.imageResource
import kotlin.math.roundToInt

/**
 * The text style editor (feed spec §7), over a live preview of the card on screen. Every change
 * applies to every card at once and is saved as it is made; the format is the same setting the
 * widgets and the daily reminders read (onboarding screen 17).
 */
@Composable
fun TextStyleSheet(
    card: FeedCard?,
    format: AyahFormat,
    style: FeedTextStyle,
    theme: FeedTheme,
    onFormat: (AyahFormat) -> Unit,
    onStyle: (FeedTextStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    TaqvoBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
            Text(
                stringResource(Res.string.feed_style_title),
                style = TaqvoText.screenTitle,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(14.dp))
            if (card != null) {
                CardPreview(card, format, style, theme, watermark = false, Modifier.align(Alignment.CenterHorizontally).width(150.dp))
                Spacer(Modifier.height(18.dp))
            }
            Controls(format, style, onFormat, onStyle)
        }
    }
}

@Composable
internal fun Controls(format: AyahFormat, style: FeedTextStyle, onFormat: (AyahFormat) -> Unit, onStyle: (FeedTextStyle) -> Unit) {
    val formats = listOf(AyahFormat.TRANSLATION, AyahFormat.ARABIC_TRANSLATION, AyahFormat.ARABIC)
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(Res.string.feed_style_format), gutter = 0.dp)
        TaqvoSegmented(
            listOf(stringResource(Res.string.onb_format_translation), stringResource(Res.string.onb_format_both), stringResource(Res.string.onb_format_arabic)),
            formats.indexOf(format).coerceAtLeast(0),
        ) { onFormat(formats[it]) }

        Spacer(Modifier.height(8.dp))
        SectionLabel(stringResource(Res.string.feed_style_font), gutter = 0.dp)
        val fonts = TranslationFont.entries.filter(FeedPremium::allowed)
        TaqvoSegmented(
            fonts.map { if (it == TranslationFont.SYSTEM) stringResource(Res.string.feed_style_font_system) else fontName(it) },
            fonts.indexOf(style.translationFont).coerceAtLeast(0),
        ) { onStyle(style.copy(translationFont = fonts[it])) }

        Spacer(Modifier.height(8.dp))
        SizeSlider(style, onStyle)

        Spacer(Modifier.height(4.dp))
        SectionLabel(stringResource(Res.string.feed_style_color), gutter = 0.dp)
        ColorSwatches(style, onStyle)

        Spacer(Modifier.height(8.dp))
        SectionLabel(stringResource(Res.string.feed_style_align), gutter = 0.dp)
        val aligns = listOf(TextAlignment.CENTER, TextAlignment.START)
        TaqvoSegmented(
            listOf(stringResource(Res.string.feed_style_align_center), stringResource(Res.string.feed_style_align_start)),
            aligns.indexOf(style.align),
        ) { onStyle(style.copy(align = aligns[it])) }
    }
}

@Composable
private fun colorName(color: TextColor): String = stringResource(
    when (color) {
        TextColor.AUTO -> Res.string.feed_color_auto
        TextColor.WHITE -> Res.string.feed_color_white
        TextColor.CREAM -> Res.string.feed_color_cream
        TextColor.BLACK -> Res.string.feed_color_black
        TextColor.AMBER -> Res.string.feed_color_amber
        TextColor.SAGE -> Res.string.feed_color_sage
        TextColor.ROSE -> Res.string.feed_color_rose
    },
)

private fun fontName(font: TranslationFont): String = when (font) {
    TranslationFont.LORA -> "Lora"
    TranslationFont.NUNITO -> "Nunito"
    TranslationFont.SYSTEM -> ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeSlider(style: FeedTextStyle, onStyle: (FeedTextStyle) -> Unit) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    var live by remember(style.sizePct) { mutableFloatStateOf(style.sizePct.toFloat()) }
    val interactions = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.accent,
        activeTrackColor = colors.accent,
        inactiveTrackColor = colors.hairline,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        SectionLabel(stringResource(Res.string.feed_style_size), gutter = 0.dp)
        Spacer(Modifier.weight(1f))
        Text("${format.localizedDigits(live.roundToInt())}%", style = TaqvoText.caption, color = colors.textSecondary)
    }
    Slider(
        value = live,
        onValueChange = { live = it },
        onValueChangeFinished = { onStyle(style.copy(sizePct = live.roundToInt())) },
        valueRange = FeedTextStyle.MIN_SIZE.toFloat()..FeedTextStyle.MAX_SIZE.toFloat(),
        steps = (FeedTextStyle.MAX_SIZE - FeedTextStyle.MIN_SIZE) / FeedTextStyle.SIZE_STEP - 1,
        colors = sliderColors,
        interactionSource = interactions,
        thumb = { SliderDefaults.Thumb(interactionSource = interactions, colors = sliderColors, thumbSize = DpSize(20.dp, 20.dp)) },
        track = { SliderDefaults.Track(sliderState = it, colors = sliderColors, drawStopIndicator = null, thumbTrackGapSize = 0.dp) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColorSwatches(style: FeedTextStyle, onStyle: (FeedTextStyle) -> Unit) {
    val colors = LocalTaqvoColors.current
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceBetween) {
        TextColor.entries.filter(FeedPremium::allowed).forEach { color ->
            val selected = style.color == color
            val name = colorName(color)
            // "Auto" is white over the theme's shade; drawn half white, half shade.
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .selectable(selected = selected, role = Role.RadioButton) { onStyle(style.copy(color = color)) }
                    .semantics { contentDescription = name }
                    .border(if (selected) 3.dp else 1.dp, if (selected) colors.accent else colors.hairline, CircleShape)
                    .padding(5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(30.dp)) {
                    if (color == TextColor.AUTO) {
                        drawArc(FeedInk.white, 90f, 180f, useCenter = true)
                        drawArc(FeedInk.deep, 270f, 180f, useCenter = true)
                    } else {
                        drawCircle(color.ink())
                    }
                }
            }
        }
    }
}

/** The card as the share image draws it, small: the same [drawFeedCard], so the preview is the result. */
@Composable
internal fun CardPreview(
    card: FeedCard,
    format: AyahFormat,
    style: FeedTextStyle,
    theme: FeedTheme,
    watermark: Boolean,
    modifier: Modifier = Modifier,
) {
    val photo = imageResource(theme.image)
    val fonts = rememberFeedFonts(style)
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val appName = stringResource(Res.string.app_name)
    Canvas(
        modifier
            .aspectRatio(SHARE_WIDTH_PX.toFloat() / SHARE_HEIGHT_PX)
            .clip(RoundedCornerShape(14.dp))
            .background(FeedInk.deep),
    ) {
        drawFeedCard(card, format, style, photo, theme.scrim, fonts, measurer, watermark, appName)
    }
}

package com.taqvo.app.feature.feed

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.FeedCard
import com.taqvo.app.feed.FeedTextStyle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/** The shared image's size (feed spec §9): 9:16 at 1080 × 1920. */
const val SHARE_WIDTH_PX = 1080
const val SHARE_HEIGHT_PX = 1920

/**
 * Draws one card as it is saved and shared (feed spec §9): the wallpaper's wash, the Arabic, the
 * translation and the reference, and — unless hidden — the watermark just under the text. The
 * share sheet's preview is this same drawing at a smaller size, so what the reader sees is what
 * they send.
 *
 * Laid out on a card [CARD_UNITS] wide whatever its pixels, as the wallpapers are. The text starts
 * at the reader's size and steps down until the block fits [BLOCK_SHARE] of the height, never below
 * [MIN_SCALE] of it; an ayah too long for that is drawn at the floor and may run past the block,
 * which the feed's own curation keeps rare (content spec §1).
 */
fun DrawScope.drawFeedCard(
    card: FeedCard,
    format: AyahFormat,
    style: FeedTextStyle,
    /** The theme's photo, drawn cropped to fill the card; null draws [fallback] alone. */
    background: ImageBitmap?,
    /** The theme's shade over the photo ([com.taqvo.app.feed.FeedTheme.scrim]). */
    scrim: Float,
    fonts: FeedFonts,
    measurer: TextMeasurer,
    watermark: Boolean,
    appName: String,
    fallback: Color = Color.Black,
) {
    val unit = size.width / CARD_UNITS
    val density = Density(unit, fontScale = 1f)
    val textWidth = ((CARD_UNITS - 2 * SIDE) * unit).toInt().coerceAtLeast(1)
    val constraints = Constraints(maxWidth = textWidth)
    drawRect(fallback)
    if (background != null) drawCover(background)
    drawRect(scrimBrush(scrim))

    val ink = style.color.ink()
    val align = style.textAlign()
    val left = SIDE * unit

    fun measure(text: AnnotatedString, textStyle: TextStyle, direction: LayoutDirection): TextLayoutResult =
        measurer.measure(text, textStyle, constraints = constraints, density = density, layoutDirection = direction)

    val arabic = card.item.arabic.takeIf { format.showsArabic() }?.let {
        buildAnnotatedString { withStyle(SpanStyle(localeList = LocaleList("ar"))) { append(it) } }
    }
    val translation = card.translation.takeIf { format.showsTranslation() || arabic == null }?.let(::AnnotatedString)
    val reference = measure(
        AnnotatedString(card.reference),
        referenceTextStyle(fonts, REFERENCE_SP, ink, align),
        LayoutDirection.Ltr,
    )

    var scale = style.sizePct / 100f
    val floor = scale * MIN_SCALE
    var arabicLayout: TextLayoutResult?
    var translationLayout: TextLayoutResult?
    var blockHeight: Float
    while (true) {
        arabicLayout = arabic?.let { measure(it, arabicTextStyle(fonts, ARABIC_SP * scale, ink, align), LayoutDirection.Rtl) }
        translationLayout = translation?.let { measure(it, translationTextStyle(fonts, TRANSLATION_SP * scale, ink, align), LayoutDirection.Ltr) }
        blockHeight = (arabicLayout?.size?.height ?: 0) +
            (if (arabicLayout != null && translationLayout != null) GAP * unit else 0f) +
            (translationLayout?.size?.height ?: 0) +
            GAP * unit + reference.size.height
        if (blockHeight <= size.height * BLOCK_SHARE || scale <= floor) break
        scale = (scale - STEP).coerceAtLeast(floor)
    }

    val markHeight = if (watermark) WATERMARK_GAP * unit + MARK_HEIGHT * unit else 0f
    var y = ((size.height - blockHeight - markHeight) / 2).coerceAtLeast(SIDE * unit)
    fun x(layout: TextLayoutResult): Float = if (align == TextAlign.Center) (size.width - layout.size.width) / 2 else left

    arabicLayout?.let {
        // An Arabic block "aligned to the start" sits on the right: the start of a right-to-left line.
        val ax = if (align == TextAlign.Center) x(it) else size.width - left - it.size.width
        drawText(it, topLeft = Offset(ax, y))
        y += it.size.height
        if (translationLayout != null) y += GAP * unit
    }
    translationLayout?.let {
        drawText(it, topLeft = Offset(x(it), y))
        y += it.size.height
    }
    y += GAP * unit
    drawText(reference, topLeft = Offset(x(reference), y))
    y += reference.size.height

    if (watermark) {
        y += WATERMARK_GAP * unit
        drawWatermark(Offset(size.width / 2, y), unit, ink, fonts, measurer, density, appName)
    }
}

/** [image] scaled to cover the whole scope and centred, the way ContentScale.Crop lays it out. */
private fun DrawScope.drawCover(image: ImageBitmap) {
    val scale = maxOf(size.width / image.width, size.height / image.height)
    val srcW = (size.width / scale).toInt().coerceIn(1, image.width)
    val srcH = (size.height / scale).toInt().coerceIn(1, image.height)
    drawImage(
        image,
        srcOffset = IntOffset((image.width - srcW) / 2, (image.height - srcH) / 2),
        srcSize = IntSize(srcW, srcH),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
    )
}

/**
 * The app's mark and name, side by side at about 60% (feed spec §9), centred on [topCentre]: the
 * mihrab from the launcher icon, drawn small, then "Taqvo".
 */
private fun DrawScope.drawWatermark(
    topCentre: Offset,
    unit: Float,
    ink: Color,
    fonts: FeedFonts,
    measurer: TextMeasurer,
    density: Density,
    appName: String,
) {
    val tint = ink.copy(alpha = WATERMARK_ALPHA)
    val name = measurer.measure(
        AnnotatedString(appName),
        TextStyle(fontFamily = fonts.reference, fontWeight = FontWeight.SemiBold, fontSize = WATERMARK_SP.sp, color = tint),
        density = density,
    )
    val markWidth = MARK_HEIGHT * unit * 0.8f
    val gap = 6f * unit
    val total = markWidth + gap + name.size.width
    val startX = topCentre.x - total / 2
    val height = MARK_HEIGHT * unit
    drawMark(Offset(startX + markWidth / 2, topCentre.y), height, tint)
    drawText(name, topLeft = Offset(startX + markWidth + gap, topCentre.y + (height - name.size.height) / 2))
}

/** The launcher icon's mihrab, from `ic_launcher_foreground.xml`'s own path on its 1024 canvas, [height] tall. */
private fun DrawScope.drawMark(topCentre: Offset, height: Float, tint: Color) {
    val scale = height / (780f - 216f)
    fun x(v: Float) = topCentre.x + (v - 512f) * scale
    fun y(v: Float) = topCentre.y + (v - 216f) * scale
    val arch = Path().apply {
        moveTo(x(292f), y(780f))
        lineTo(x(292f), y(520f))
        cubicTo(x(292f), y(372f), x(380f), y(268f), x(512f), y(216f))
        cubicTo(x(644f), y(268f), x(732f), y(372f), x(732f), y(520f))
        lineTo(x(732f), y(780f))
    }
    drawPath(arch, color = tint, style = Stroke(width = 64f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawCircle(tint, radius = 46f * scale, center = Offset(x(512f), y(392f)))
}

private const val CARD_UNITS = 400f
private const val SIDE = 36f
private const val ARABIC_SP = 25f
private const val TRANSLATION_SP = 16f
private const val REFERENCE_SP = 11f
private const val GAP = 20f
private const val BLOCK_SHARE = 0.7f
private const val MIN_SCALE = 0.7f
private const val STEP = 0.05f
private const val WATERMARK_GAP = 26f
private const val MARK_HEIGHT = 18f
private const val WATERMARK_SP = 12f
private const val WATERMARK_ALPHA = 0.6f

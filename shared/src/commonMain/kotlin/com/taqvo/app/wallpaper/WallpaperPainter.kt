package com.taqvo.app.wallpaper

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.taqvo.app.design.WallpaperColors
import com.taqvo.app.design.WallpaperDawn
import com.taqvo.app.design.WallpaperDusk
import com.taqvo.app.design.WallpaperNight
import com.taqvo.app.design.WallpaperSlate

/** The four looks a wallpaper can take; the colours themselves live in Palette.kt. */
enum class WallpaperStyle(val colors: WallpaperColors) {
    NIGHT(WallpaperNight),
    DAWN(WallpaperDawn),
    DUSK(WallpaperDusk),
    SLATE(WallpaperSlate),
}

/**
 * The three faces a card is set in. Built in composition (fonts from resources need it) and
 * handed to [drawWallpaper], which runs outside it as well, for the full-size picture.
 */
class WallpaperFonts(val quran: FontFamily, val dua: FontFamily, val translation: FontFamily)

/**
 * Draws one wallpaper filling the scope: the wash, the mihrab, the Arabic, the translation and
 * the reference, centred a little below the middle.
 *
 * Everything is laid out on a card [CARD_UNITS] wide whatever its pixels, so the preview in Settings
 * and the picture handed to the phone are the same drawing at two sizes: what the reader chose
 * from is what they get. The block sits below the middle because the top of a phone's screen is
 * the Lock Screen's clock and the status bar, and never lower than [BLOCK_BOTTOM], above the
 * home screen's dock and the Lock Screen's two buttons.
 *
 * The Arabic starts at [ARABIC_SP] and steps down, the translation with it, until the whole block
 * fits [BLOCK_SHARE] of the height; [WallpaperTexts.MAX_DUA_LENGTH] and the pool's curation keep
 * every card above [MIN_ARABIC_SP], so nothing is ever cut.
 */
fun DrawScope.drawWallpaper(
    text: WallpaperText,
    reference: String,
    style: WallpaperStyle,
    fonts: WallpaperFonts,
    measurer: TextMeasurer,
    /** The translation's language is written right to left (Urdu), whatever the interface's. */
    translationRtl: Boolean,
) {
    val colors = style.colors
    val unit = size.width / CARD_UNITS
    // One sp is one unit here, so every size below is in card units.
    val density = Density(unit, fontScale = 1f)
    val textWidth = ((CARD_UNITS - 2 * SIDE) * unit).toInt().coerceAtLeast(1)
    val constraints = Constraints(maxWidth = textWidth)

    drawRect(Brush.verticalGradient(listOf(colors.top, colors.bottom)))

    fun measure(value: AnnotatedString, textStyle: TextStyle, direction: LayoutDirection): TextLayoutResult =
        measurer.measure(value, textStyle, constraints = constraints, density = density, layoutDirection = direction)

    val arabic = buildAnnotatedString { withStyle(SpanStyle(localeList = LocaleList("ar"))) { append(text.arabic) } }
    val translationDirection = if (translationRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val referenceLayout = measure(
        AnnotatedString(reference),
        TextStyle(
            fontFamily = fonts.translation,
            fontWeight = FontWeight.SemiBold,
            fontSize = REFERENCE_SP.sp,
            letterSpacing = 0.6.sp,
            color = colors.accent,
            textAlign = TextAlign.Center,
        ),
        translationDirection,
    )

    var arabicSp = if (text.quran) ARABIC_SP else ARABIC_SP - DUA_STEP
    var translationSp = TRANSLATION_SP
    var arabicLayout: TextLayoutResult
    var translationLayout: TextLayoutResult?
    var blockHeight: Float
    do {
        arabicLayout = measure(arabic, arabicStyle(text.quran, arabicSp, fonts, colors), LayoutDirection.Rtl)
        translationLayout = text.translation?.let {
            measure(
                AnnotatedString(it),
                TextStyle(
                    fontFamily = if (translationRtl) FontFamily.Default else fonts.translation,
                    fontSize = translationSp.sp,
                    lineHeight = (translationSp * 1.5f).sp,
                    color = colors.secondary,
                    textAlign = TextAlign.Center,
                    textDirection = if (translationRtl) TextDirection.Rtl else TextDirection.Content,
                ),
                translationDirection,
            )
        }
        blockHeight = (MIHRAB_HEIGHT + MIHRAB_GAP) * unit +
            arabicLayout.size.height +
            (translationLayout?.let { TRANSLATION_GAP * unit + it.size.height } ?: 0f) +
            REFERENCE_GAP * unit + referenceLayout.size.height
        val fits = blockHeight <= size.height * BLOCK_SHARE || arabicSp <= MIN_ARABIC_SP
        if (!fits) {
            arabicSp -= 1f
            if (translationSp > MIN_TRANSLATION_SP) translationSp -= 0.5f
        }
    } while (!fits)

    val top = (size.height * BLOCK_CENTRE - blockHeight / 2)
        .coerceAtMost(size.height * BLOCK_BOTTOM - blockHeight)
        .coerceAtLeast(0f)
    var y = top
    drawMihrab(Offset(size.width / 2, y), MIHRAB_HEIGHT * unit, colors)
    y += (MIHRAB_HEIGHT + MIHRAB_GAP) * unit
    drawText(arabicLayout, topLeft = Offset((size.width - arabicLayout.size.width) / 2, y))
    y += arabicLayout.size.height
    translationLayout?.let {
        y += TRANSLATION_GAP * unit
        drawText(it, topLeft = Offset((size.width - it.size.width) / 2, y))
        y += it.size.height
    }
    y += REFERENCE_GAP * unit
    drawText(referenceLayout, topLeft = Offset((size.width - referenceLayout.size.width) / 2, y))
}

/**
 * The Quran in the Hafs face at the reader's 2.0 leading, so its stacked marks never touch the line
 * above; a supplication in the OS face at the adhkar page's 1.8. Both centred: a card has no margin
 * to range them against.
 */
private fun arabicStyle(quran: Boolean, sizeSp: Float, fonts: WallpaperFonts, colors: WallpaperColors) = TextStyle(
    fontFamily = if (quran) fonts.quran else fonts.dua,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * if (quran) 2.0f else 1.8f).sp,
    letterSpacing = 0.sp,
    color = colors.ink,
    textAlign = TextAlign.Center,
    textDirection = TextDirection.Rtl,
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
)

/**
 * The app icon's mark, the mihrab with its amber point, from `ic_launcher_foreground.xml`'s own
 * path on its 1024 canvas: the arch spans y 216 to 780 there, and [height] is that span here.
 * [topCentre] is the apex.
 */
private fun DrawScope.drawMihrab(topCentre: Offset, height: Float, colors: WallpaperColors) {
    val scale = height / (ARCH_BOTTOM - ARCH_APEX)
    // The icon's coordinates, moved so its apex (512, 216) lands on [topCentre].
    fun x(v: Float) = topCentre.x + (v - 512f) * scale
    fun y(v: Float) = topCentre.y + (v - ARCH_APEX) * scale
    val arch = Path().apply {
        moveTo(x(292f), y(780f))
        lineTo(x(292f), y(520f))
        cubicTo(x(292f), y(372f), x(380f), y(268f), x(512f), y(216f))
        cubicTo(x(644f), y(268f), x(732f), y(372f), x(732f), y(520f))
        lineTo(x(732f), y(780f))
    }
    drawPath(
        arch,
        color = colors.ink.copy(alpha = 0.85f),
        style = Stroke(width = 64f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawCircle(colors.accent, radius = 46f * scale, center = Offset(x(512f), y(392f)))
}

/**
 * The full-size picture: [draw] into a fresh [widthPx] × [heightPx] bitmap, outside composition.
 * The text inside [drawWallpaper] measures itself in card units, so the density given the scope
 * here plays no part.
 */
fun renderWallpaper(widthPx: Int, heightPx: Int, draw: DrawScope.() -> Unit): ImageBitmap {
    val bitmap = ImageBitmap(widthPx, heightPx)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(bitmap),
        size = Size(widthPx.toFloat(), heightPx.toFloat()),
        block = draw,
    )
    return bitmap
}

/** The card's width in its own units, whatever its pixels. */
private const val CARD_UNITS = 400f
private const val SIDE = 40f
private const val ARABIC_SP = 27f
private const val DUA_STEP = 3f
private const val MIN_ARABIC_SP = 16f
private const val TRANSLATION_SP = 15f
private const val MIN_TRANSLATION_SP = 11f
private const val REFERENCE_SP = 12f
private const val MIHRAB_HEIGHT = 34f
private const val MIHRAB_GAP = 30f
private const val TRANSLATION_GAP = 22f
private const val REFERENCE_GAP = 22f
private const val BLOCK_SHARE = 0.56f
private const val BLOCK_CENTRE = 0.57f
private const val BLOCK_BOTTOM = 0.86f
private const val ARCH_APEX = 216f
private const val ARCH_BOTTOM = 780f

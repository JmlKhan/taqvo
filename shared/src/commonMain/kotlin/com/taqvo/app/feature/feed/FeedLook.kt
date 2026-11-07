package com.taqvo.app.feature.feed

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.design.FeedInk
import com.taqvo.app.feed.ArabicFont
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.feed.TextAlignment
import com.taqvo.app.feed.TextColor
import com.taqvo.app.feed.TranslationFont
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.amiri_quran
import com.taqvo.app.resources.lora_regular
import com.taqvo.app.resources.nunito_regular
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import com.taqvo.app.feed.FeedTheme

/**
 * The faces the feed draws in, built once in composition (fonts from resources need it) and handed
 * to the card and to [drawFeedCard], which also runs outside composition for the shared image.
 */
class FeedFonts(val arabic: FontFamily, val translation: FontFamily, val reference: FontFamily)

@Composable
fun rememberFeedFonts(style: FeedTextStyle): FeedFonts {
    val amiri = Font(Res.font.amiri_quran)
    val lora = Font(Res.font.lora_regular)
    val nunito = Font(Res.font.nunito_regular)
    return remember(style.arabicFont, style.translationFont, amiri, lora, nunito) {
        val arabic = when (style.arabicFont) {
            ArabicFont.AMIRI_QURAN -> FontFamily(amiri)
        }
        val translation = translationFamily(style.translationFont, lora, nunito)
        FeedFonts(arabic = arabic, translation = translation, reference = translation)
    }
}

/** One face per [TranslationFont]; [TranslationFont.SYSTEM] is the phone's own. */
fun translationFamily(font: TranslationFont, lora: androidx.compose.ui.text.font.Font, nunito: androidx.compose.ui.text.font.Font): FontFamily =
    when (font) {
        TranslationFont.LORA -> FontFamily(lora)
        TranslationFont.NUNITO -> FontFamily(nunito)
        TranslationFont.SYSTEM -> FontFamily.Default
    }

/**
 * The words' colour. Every theme is a photo under a shade, so [TextColor.AUTO] is white, the
 * colour that reads on all of them; the others are the reader's own choice.
 */
fun TextColor.ink(): Color = when (this) {
    TextColor.AUTO, TextColor.WHITE -> FeedInk.white
    TextColor.CREAM -> FeedInk.cream
    TextColor.BLACK -> FeedInk.black
    TextColor.AMBER -> FeedInk.amber
    TextColor.SAGE -> FeedInk.sage
    TextColor.ROSE -> FeedInk.rose
}

/** The chrome over a theme (the buttons, the chips): always white, with the shade and a shadow under it. */
val ChromeInk: Color get() = FeedInk.white

/**
 * A theme's photo filling the space, cropped to it, with its shade over it: darker in the middle,
 * where the words are, and by how bright the photo is ([FeedTheme.scrim]).
 */
@Composable
fun FeedBackground(theme: FeedTheme, modifier: Modifier = Modifier) {
    Box(modifier) {
        Image(
            painter = painterResource(theme.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(scrimBrush(theme.scrim)))
    }
}

fun scrimBrush(scrim: Float): Brush = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = scrim * 0.7f),
    0.5f to Color.Black.copy(alpha = scrim),
    1f to Color.Black.copy(alpha = scrim * 0.8f),
)

fun FeedTextStyle.textAlign(): TextAlign = if (align == TextAlignment.START) TextAlign.Start else TextAlign.Center

/** Which lines a card shows for [format] (onboarding screen 17, shared with the widgets). */
fun AyahFormat.showsArabic(): Boolean = this != AyahFormat.TRANSLATION
fun AyahFormat.showsTranslation(): Boolean = this != AyahFormat.ARABIC

/**
 * The sizes in sp at 100% (feed spec §3.2): the Arabic about 1.4 times the translation, the
 * reference small. [scale] is the reader's size and the auto-fit's step down, together.
 */
object FeedTypeScale {
    const val TRANSLATION_SP = 20f
    const val ARABIC_SP = 28f
    const val REFERENCE_SP = 13f
    /** The auto-fit never goes below this share of the reader's chosen size (feed spec §3.2). */
    const val MIN_FIT = 0.7f
    const val FIT_STEP = 0.06f
}

fun arabicTextStyle(fonts: FeedFonts, sizeSp: Float, color: Color, align: TextAlign): TextStyle = TextStyle(
    fontFamily = fonts.arabic,
    fontSize = sizeSp.sp,
    // Amiri Quran's marks stack high; the reader's own 2.0 leading keeps them off the line above.
    lineHeight = (sizeSp * 2.0f).sp,
    letterSpacing = 0.sp,
    color = color,
    textAlign = if (align == TextAlign.Start) TextAlign.Right else align,
    textDirection = TextDirection.Rtl,
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.None),
)

fun translationTextStyle(fonts: FeedFonts, sizeSp: Float, color: Color, align: TextAlign): TextStyle = TextStyle(
    fontFamily = fonts.translation,
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 1.45f).sp,
    color = color,
    textAlign = align,
    textDirection = TextDirection.Content,
)

fun referenceTextStyle(fonts: FeedFonts, sizeSp: Float, color: Color, align: TextAlign): TextStyle = TextStyle(
    fontFamily = fonts.reference,
    fontWeight = FontWeight.SemiBold,
    fontSize = sizeSp.sp,
    letterSpacing = TextUnit.Unspecified,
    color = color.copy(alpha = color.alpha * 0.7f),
    textAlign = align,
)

package com.taqvo.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.painterResource
import com.taqvo.app.feed.FeedThemes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.components.CheckMark
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.mushafFamily
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.i18n.uiFontFamily
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.onb_continue
import com.taqvo.app.resources.onb_loading_item_ayahs
import com.taqvo.app.resources.onb_loading_item_reminders
import com.taqvo.app.resources.onb_loading_item_settings
import com.taqvo.app.resources.onb_loading_item_themes
import com.taqvo.app.resources.onb_loading_percent
import com.taqvo.app.resources.onb_loading_status_1
import com.taqvo.app.resources.onb_loading_status_2
import com.taqvo.app.resources.onb_loading_status_3
import com.taqvo.app.resources.onb_loading_title
import com.taqvo.app.resources.onb_theme_title
import com.taqvo.app.resources.wallpapers_style_dawn
import com.taqvo.app.resources.wallpapers_style_dusk
import com.taqvo.app.resources.wallpapers_style_night
import com.taqvo.app.resources.wallpapers_style_slate
import com.taqvo.app.wallpaper.WallpaperFonts
import com.taqvo.app.wallpaper.WallpaperStyle
import com.taqvo.app.wallpaper.WallpaperText
import com.taqvo.app.wallpaper.drawWallpaper

/** The ayah a wallpaper screen draws, with its reference line and the translation's direction. */
data class WallpaperSample(val text: WallpaperText, val reference: String, val translationRtl: Boolean)

/** About the competitor's six seconds (spec, screen 10). */
private const val LOADING_MS = 5_500

/**
 * Screen 10: the percentage counts to 100 while [work] really runs (the reminders are planned and
 * armed); it never reaches 100 before the work is done. The checklist ticks at each quarter, and
 * the screen moves on by itself 600 ms after it is full.
 */
@Composable
internal fun LoadingScreen(progress: Float?, work: suspend () -> Unit, onDone: () -> Unit) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    val shown = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        coroutineScope {
            val job = async { runCatching { work() } }
            shown.animateTo(0.95f, tween(LOADING_MS, easing = LinearEasing))
            job.await()
            shown.animateTo(1f, tween(250))
        }
        done = true
        delay(600)
        onDone()
    }
    val percent = (shown.value * 100).toInt()
    val statuses = listOf(Res.string.onb_loading_status_1, Res.string.onb_loading_status_2, Res.string.onb_loading_status_3)
    val items = listOf(
        Res.string.onb_loading_item_ayahs,
        Res.string.onb_loading_item_settings,
        Res.string.onb_loading_item_themes,
        Res.string.onb_loading_item_reminders,
    )
    OnbPage(
        progress = progress,
        onSkip = null,
        content = {
            Text(
                stringResource(Res.string.onb_loading_percent, format.localizedDigits(percent)),
                color = colors.textPrimary,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(12.dp))
            OnbTitle(stringResource(Res.string.onb_loading_title))
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(colors.hairline)) {
                Box(Modifier.fillMaxWidth(shown.value).height(6.dp).clip(CircleShape).background(colors.accent))
            }
            Spacer(Modifier.height(10.dp))
            OnbBody(stringResource(statuses[(percent / 34).coerceAtMost(2)]))
            Spacer(Modifier.height(24.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEachIndexed { index, item ->
                    ChecklistRow(item, ticked = percent >= (index + 1) * 25)
                }
            }
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onDone, enabled = done) },
    )
}

@Composable
private fun ChecklistRow(label: StringResource, ticked: Boolean) {
    val colors = LocalTaqvoColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, if (ticked) colors.accent else colors.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (ticked) CheckMark(Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(label),
            color = if (ticked) colors.textPrimary else colors.textTertiary,
            fontSize = 15.sp,
            fontWeight = if (ticked) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** The fonts a wallpaper is set in, as Settings › Wallpapers builds them. */
@Composable
internal fun wallpaperFonts() = WallpaperFonts(quran = mushafFamily(), dua = FontFamily.Default, translation = uiFontFamily())

/** Screen 11: the four looks, each drawn with the sample ayah, one always chosen. */
@Composable
internal fun ThemeScreen(
    progress: Float?,
    selectedId: String,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = LocalTaqvoColors.current
    OnbPage(
        progress = progress,
        onSkip = null,
        centred = false,
        content = {
            Spacer(Modifier.height(8.dp))
            OnbTitle(stringResource(Res.string.onb_theme_title))
            Spacer(Modifier.height(24.dp))
            ONBOARDING_THEME_IDS.map(FeedThemes::of).chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { theme ->
                        val chosen = theme.id == selectedId
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(18.dp))
                                .border(if (chosen) 3.dp else 1.dp, if (chosen) colors.accent else colors.hairline, RoundedCornerShape(18.dp))
                                .selectable(selected = chosen, role = Role.RadioButton) { onSelect(theme.id) },
                        ) {
                            Image(painterResource(theme.thumb), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            if (chosen) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(26.dp).clip(CircleShape).background(colors.background),
                                    contentAlignment = Alignment.Center,
                                ) { CheckMark(Modifier.size(18.dp)) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

/** The four feed backgrounds onboarding offers (of the Mavzular screen's full set): the mosque under a crescent, green hills, camels at dusk, misty forest. */
internal val ONBOARDING_THEME_IDS = listOf("theme_06", "theme_08", "theme_16", "theme_02")

/** Screen 12: the chosen background full screen, the ayah fading in a moment after the picture. */
@Composable
internal fun PreviewScreen(progress: Float?, sample: WallpaperSample?, themeId: String, onContinue: () -> Unit) {
    val theme = FeedThemes.of(themeId)
    val fonts = wallpaperFonts()
    val picture = remember { Animatable(0f) }
    LaunchedEffect(Unit) { picture.animateTo(1f, tween(500)) }
    OnbPage(
        progress = progress,
        onSkip = null,
        background = {
            Box(Modifier.fillMaxSize().alpha(picture.value)) {
                Image(painterResource(theme.image), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = theme.scrim)))
                if (sample != null) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(sample.text.arabic, color = Color.White, fontFamily = fonts.quran, fontSize = 28.sp, textAlign = TextAlign.Center)
                        sample.text.translation?.let {
                            Spacer(Modifier.height(16.dp))
                            Text(it, color = Color.White, fontFamily = fonts.translation, fontSize = 16.sp, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(sample.reference, color = Color.White.copy(alpha = 0.8f), fontFamily = fonts.translation, fontSize = 13.sp)
                    }
                }
            }
        },
        content = {},
        footer = { LightButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

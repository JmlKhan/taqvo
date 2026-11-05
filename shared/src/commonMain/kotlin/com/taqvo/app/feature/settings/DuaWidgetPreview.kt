package com.taqvo.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.PreviewWallpaper
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.domain.WidgetBackground
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.wallpapers_dua_reference
import com.taqvo.app.widget.AyahPoolMirror
import com.taqvo.app.widget.AyahRotation
import com.taqvo.app.widget.DuaPoolEntry
import com.taqvo.app.widget.DuaPoolMirror
import com.taqvo.app.widget.WidgetPalette
import com.taqvo.app.widget.createWidgetKeyValueStore
import kotlin.time.Clock

/**
 * The dua widget's card in Settings › Widgets: today's supplication from the same mirror and the
 * same rotation the real widgets read (`TaqvoDuaGlanceWidget`, `DuaTimelineProvider`), so the
 * preview is the card on the home screen. Before the app has written the mirror, Hisn al-Muslim 91
 * stands in, Arabic only, which every language can show truthfully.
 */
@Composable
fun DuaWidgetPreview(
    background: WidgetBackground,
    systemIsDark: Boolean,
    modifier: Modifier = Modifier,
    /** The swatch's corners, as for [WidgetPreview]. */
    corner: Dp = 18.dp,
) {
    val colors = WidgetPalette.colorsFor(background, systemIsDark)
    // Read once per composition, as the ayah preview does: only [background] changes while it shows.
    val (mirror, seed) = remember {
        val store = createWidgetKeyValueStore()
        DuaPoolMirror.read(store) to (AyahPoolMirror.seed(store) ?: 0L)
    }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val entry = mirror?.entryFor(AyahRotation.epochDay(today.year, today.monthNumber, today.dayOfMonth), seed)
        ?: SampleDua
    val textColor = Color(colors.textArgb)
    val reference = stringResource(Res.string.wallpapers_dua_reference, LocalPlatformFormat.current.localizedDigits(entry.hisn))

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(PreviewWallpaper)
            .padding(14.dp)
            // A picture of a sample card: its name and line below say what it is.
            .clearAndSetSemantics {},
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .aspectRatio(DuaCardAspectRatio)
                .clip(RoundedCornerShape(19.dp))
                .background(Color(colors.backgroundArgb).copy(alpha = colors.backgroundAlpha))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    entry.arabic,
                    style = TextStyle(fontFamily = FontFamily.Default, fontSize = arabicSize(entry.arabic.length)),
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (entry.translation.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                val direction = if (mirror?.translationRtl == true) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Text(
                        entry.translation,
                        style = TaqvoText.caption.copy(fontSize = 12.sp),
                        color = textColor.copy(alpha = 0.66f),
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                reference,
                style = TaqvoText.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = Color(colors.accentArgb),
                maxLines = 1,
            )
        }
    }
}

/** The widget's own steps (`TaqvoDuaGlanceWidget.arabicSize`), at the roomy size. */
private fun arabicSize(length: Int) = when {
    length <= 60 -> 22.sp
    length <= 130 -> 18.sp
    else -> 15.sp
}

/** A 4×2 cell, the dua widget's default. */
private const val DuaCardAspectRatio = 2.1f

private val SampleDua = DuaPoolEntry(hisn = 91, arabic = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", translation = "")

package com.taqvo.app.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.CheckMark
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.design.components.TaqvoSegmented
import com.taqvo.app.design.components.TaqvoTextLink
import com.taqvo.app.design.mushafFamily
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.i18n.uiFontFamily
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.settings_wallpapers
import com.taqvo.app.resources.wallpapers_a11y_preview
import com.taqvo.app.resources.wallpapers_done_saved
import com.taqvo.app.resources.wallpapers_done_set
import com.taqvo.app.resources.wallpapers_dua_reference
import com.taqvo.app.resources.wallpapers_failed
import com.taqvo.app.resources.wallpapers_next
import com.taqvo.app.resources.wallpapers_position
import com.taqvo.app.resources.wallpapers_previous
import com.taqvo.app.resources.wallpapers_refused_android
import com.taqvo.app.resources.wallpapers_refused_ios
import com.taqvo.app.resources.wallpapers_save
import com.taqvo.app.resources.wallpapers_save_note
import com.taqvo.app.resources.wallpapers_set
import com.taqvo.app.resources.wallpapers_source_ayahs
import com.taqvo.app.resources.wallpapers_source_duas
import com.taqvo.app.resources.wallpapers_style_dawn
import com.taqvo.app.resources.wallpapers_style_dusk
import com.taqvo.app.resources.wallpapers_style_label
import com.taqvo.app.resources.wallpapers_style_night
import com.taqvo.app.resources.wallpapers_style_slate
import com.taqvo.app.resources.wallpapers_target_both
import com.taqvo.app.resources.wallpapers_target_home
import com.taqvo.app.resources.wallpapers_target_label
import com.taqvo.app.resources.wallpapers_target_lock
import com.taqvo.app.resources.wallpapers_words_label
import com.taqvo.app.wallpaper.WallpaperFonts
import com.taqvo.app.wallpaper.WallpaperOutcome
import com.taqvo.app.wallpaper.WallpaperSetter
import com.taqvo.app.wallpaper.WallpaperSource
import com.taqvo.app.wallpaper.WallpaperStyle
import com.taqvo.app.wallpaper.WallpaperTarget
import com.taqvo.app.wallpaper.WallpaperText
import com.taqvo.app.wallpaper.drawWallpaper
import com.taqvo.app.wallpaper.renderWallpaper

/** What a [WallpaperSource] is called on its segment. */
private val WallpaperSource.label: StringResource
    get() = when (this) {
        WallpaperSource.AYAH -> Res.string.wallpapers_source_ayahs
        WallpaperSource.DUA -> Res.string.wallpapers_source_duas
    }

private val WallpaperStyle.label: StringResource
    get() = when (this) {
        WallpaperStyle.NIGHT -> Res.string.wallpapers_style_night
        WallpaperStyle.DAWN -> Res.string.wallpapers_style_dawn
        WallpaperStyle.DUSK -> Res.string.wallpapers_style_dusk
        WallpaperStyle.SLATE -> Res.string.wallpapers_style_slate
    }

private val WallpaperTarget.label: StringResource
    get() = when (this) {
        WallpaperTarget.HOME -> Res.string.wallpapers_target_home
        WallpaperTarget.LOCK -> Res.string.wallpapers_target_lock
        WallpaperTarget.BOTH -> Res.string.wallpapers_target_both
    }

/**
 * Settings › Wallpapers: one of the widget pool's hundred ayahs or one of the adhkar's short
 * supplications, drawn as a phone wallpaper in one of four styles. The preview is the picture
 * itself, drawn by the same [drawWallpaper] at a smaller size, so what is chosen is what is set.
 *
 * Android puts it on the home screen, the Lock Screen or both; iOS lets no app change the
 * wallpaper, so there it goes to Photos with the steps to set it from there.
 *
 * [texts] is empty for a source until the Quran database or the adhkar file has answered.
 * [surahNames] names each surah in the interface's script for the reference line.
 */
@Composable
fun WallpapersScreen(
    texts: Map<WallpaperSource, List<WallpaperText>>,
    surahNames: Map<Int, String>,
    /** The reader's translation is written right to left (Urdu); a supplication's follows the interface. */
    ayahTranslationRtl: Boolean,
    duaTranslationRtl: Boolean,
    setter: WallpaperSetter,
    onBack: () -> Unit,
) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    val scope = rememberCoroutineScope()
    val measurer = rememberTextMeasurer()
    val fonts = WallpaperFonts(quran = mushafFamily(), dua = FontFamily.Default, translation = uiFontFamily())
    val screen = remember(setter) { setter.screenSize() }

    var source by remember { mutableStateOf(WallpaperSource.AYAH) }
    // One position per source, so a look at the duas does not lose the ayah the reader was on.
    val positions = remember { mutableStateOf(mapOf<WallpaperSource, Int>()) }
    var style by remember { mutableStateOf(WallpaperStyle.NIGHT) }
    var target by remember { mutableStateOf(WallpaperTarget.BOTH) }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<WallpaperOutcome?>(null) }

    val list = texts[source].orEmpty()
    val index = (positions.value[source] ?: 0).let { if (list.isEmpty()) 0 else it.mod(list.size) }
    val current = list.getOrNull(index)
    fun step(by: Int) {
        if (list.isEmpty()) return
        positions.value = positions.value + (source to (index + by).mod(list.size))
        outcome = null
    }

    val reference = when {
        current == null -> ""
        current.source == WallpaperSource.AYAH -> buildString {
            append(surahNames[current.surah].orEmpty()).append(' ')
            append(format.localizedDigits(current.surah)).append(':').append(format.localizedDigits(current.ayah))
        }
        else -> stringResource(Res.string.wallpapers_dua_reference, format.localizedDigits(current.hisn))
    }
    val translationRtl = if (source == WallpaperSource.AYAH) ayahTranslationRtl else duaTranslationRtl
    // The Quran's Arabic is never given to a screen reader (the app's rule): the picture says its
    // reference and its translation.
    val previewDescription = stringResource(
        Res.string.wallpapers_a11y_preview,
        listOfNotNull(reference, current?.translation).joinToString(". "),
    )

    SettingsScaffold(stringResource(Res.string.settings_wallpapers), onBack) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .width(PreviewWidth)
                    .aspectRatio(screen.width.toFloat() / screen.height.coerceAtLeast(1))
                    .clip(PreviewShape)
                    .border(1.dp, colors.hairline, PreviewShape)
                    .semantics { contentDescription = previewDescription },
            ) {
                if (current != null) drawWallpaper(current, reference, style, fonts, measurer, translationRtl)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = SettingsGutter), verticalAlignment = Alignment.CenterVertically) {
            TaqvoTextLink(stringResource(Res.string.wallpapers_previous), { step(-1) }, Modifier.weight(1f))
            Text(
                if (list.isEmpty()) "" else stringResource(
                    Res.string.wallpapers_position,
                    format.localizedDigits(index + 1),
                    format.localizedDigits(list.size),
                ),
                style = TaqvoText.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            TaqvoTextLink(stringResource(Res.string.wallpapers_next), { step(1) }, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(Res.string.wallpapers_words_label))
        Box(Modifier.fillMaxWidth().padding(horizontal = SettingsGutter)) {
            TaqvoSegmented(
                options = WallpaperSource.entries.map { stringResource(it.label) },
                selectedIndex = source.ordinal,
                onSelect = { source = WallpaperSource.entries[it]; outcome = null },
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(Res.string.wallpapers_style_label))
        SettingsCard(Modifier.selectableGroup()) {
            WallpaperStyle.entries.forEachIndexed { i, option ->
                if (i > 0) CardDivider()
                TaqvoRow(
                    label = stringResource(option.label),
                    onClick = { style = option; outcome = null },
                    selectable = true,
                    selected = option == style,
                    trailing = { if (option == style) CheckMark() },
                )
            }
        }

        if (setter.setsDirectly) {
            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(Res.string.wallpapers_target_label))
            SettingsCard(Modifier.selectableGroup()) {
                WallpaperTarget.entries.forEachIndexed { i, option ->
                    if (i > 0) CardDivider()
                    TaqvoRow(
                        label = stringResource(option.label),
                        onClick = { target = option; outcome = null },
                        selectable = true,
                        selected = option == target,
                        trailing = { if (option == target) CheckMark() },
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        TaqvoPrimaryButton(
            stringResource(if (setter.setsDirectly) Res.string.wallpapers_set else Res.string.wallpapers_save),
            onClick = {
                val text = current ?: return@TaqvoPrimaryButton
                val chosenStyle = style
                val chosenTarget = target
                val ref = reference
                val rtl = translationRtl
                busy = true
                scope.launch {
                    val image = renderWallpaper(screen.width, screen.height) {
                        drawWallpaper(text, ref, chosenStyle, fonts, measurer, rtl)
                    }
                    outcome = setter.apply(image, chosenTarget)
                    busy = false
                }
            },
            modifier = Modifier.padding(horizontal = SettingsGutter),
            enabled = current != null && !busy,
        )
        if (!setter.setsDirectly) {
            Spacer(Modifier.height(14.dp))
            SettingsNote(stringResource(Res.string.wallpapers_save_note))
        }
        outcome?.let { result ->
            Spacer(Modifier.height(14.dp))
            val message = when (result) {
                WallpaperOutcome.SET -> Res.string.wallpapers_done_set
                WallpaperOutcome.SAVED -> Res.string.wallpapers_done_saved
                WallpaperOutcome.REFUSED ->
                    if (setter.setsDirectly) Res.string.wallpapers_refused_android else Res.string.wallpapers_refused_ios
                WallpaperOutcome.FAILED -> Res.string.wallpapers_failed
            }
            // Polite: the answer is read once it arrives, without cutting off what is being read.
            Box(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                SettingsNote(stringResource(message))
            }
        }
    }
}

private val PreviewWidth = 200.dp
private val PreviewShape = RoundedCornerShape(20.dp)

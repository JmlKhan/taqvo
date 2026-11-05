package com.taqvo.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import com.taqvo.app.audio.notificationSoundsMuted
import com.taqvo.app.resources.sound_sheet_muted_hint
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.TaqvoCard
import com.taqvo.app.domain.AdhanVoice
import com.taqvo.app.domain.PrayerSound
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.i18n.soundDisplayName
import com.taqvo.app.notifications.SoundAssets
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.sound_a11y_preview
import com.taqvo.app.resources.sound_adhan_detail
import com.taqvo.app.widget.isIosPlatform
import com.taqvo.app.resources.sound_adhan_detail_full
import com.taqvo.app.resources.sound_notification_detail
import com.taqvo.app.resources.sound_sheet_footnote
import com.taqvo.app.resources.sound_sheet_title
import com.taqvo.app.resources.sound_silent_detail
import com.taqvo.app.resources.sound_takbir_detail

/** How often the open sheet asks whether the phone is silenced. */
private const val MUTED_POLL_MS = 1_500L

/** The platform cap on a notification sound, which the footnote quotes. */
private const val SOUND_CAP_SECONDS = 30

/**
 * Every length comes from [SoundAssets] and rounds to whole seconds for display — the spec's
 * original "~6s" assumed a plainly recited takbir; the original recording's first complete
 * "Allahu akbar, Allahu akbar" pair actually runs 15.8s, so the subtitle says ~16s rather than
 * repeat that assumption. Reading the table rather than a constant is also what lets the subtitle
 * follow the chosen voice, whose clips are cut to their own lengths. The numbers themselves go
 * through the locale's digits, like every other numeral in the app.
 */
@Composable
private fun subtitle(sound: PrayerSound, voice: AdhanVoice): String {
    val format = LocalPlatformFormat.current
    val seconds = SoundAssets.duration(sound, voice)?.let { format.localizedSeconds(it) }
    return when (sound) {
        PrayerSound.SILENT -> stringResource(Res.string.sound_silent_detail)
        PrayerSound.NOTIFICATION -> stringResource(Res.string.sound_notification_detail, seconds!!)
        PrayerSound.TAKBIR -> stringResource(Res.string.sound_takbir_detail, seconds!!)
        // Android plays the whole adhan; only iOS stops a notification sound at 30 seconds.
        PrayerSound.ADHAN -> if (isIosPlatform) stringResource(Res.string.sound_adhan_detail, seconds!!) else stringResource(Res.string.sound_adhan_detail_full)
    }
}

/**
 * Four levels, each auditionable before committing. Silent has nothing to preview, so it is the
 * one row with no play button. [voice] is the adhan the user has chosen: the two levels that are
 * a recording preview *that* recording and quote *its* length, so the sheet never auditions a
 * voice the notification would not actually play.
 */
@Composable
fun SoundSheet(
    current: PrayerSound,
    voice: AdhanVoice,
    onPick: (PrayerSound) -> Unit,
    onPreview: (PrayerSound) -> Unit,
    /** The levels on offer: all four for a prayer; Tahajjud leaves the adhan out (spec §17.6). */
    options: List<PrayerSound> = PrayerSound.entries,
    /** The sheet's title. Null is "Notification sound"; a prayer's sheet names the prayer while
     * Iqama is on, since the sheet then holds more than its sound (Duha and Iqama spec §4.2). */
    title: String? = null,
    /** A section label over the sound card, given when [extra] adds a second section. */
    sectionLabel: String? = null,
    /** A second section after the footnote: a prayer's iqama minutes. Given, the sheet scrolls,
     * since the two together outgrow a short phone. */
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = LocalTaqvoColors.current
    // Asked while the sheet is open rather than once: the reader who hears nothing reaches for
    // the volume keys, and the hint should go the moment the phone can be heard again.
    var muted by remember { mutableStateOf(notificationSoundsMuted()) }
    LaunchedEffect(Unit) {
        while (true) {
            muted = notificationSoundsMuted()
            delay(MUTED_POLL_MS)
        }
    }
    val scroll = if (extra != null) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(scroll.padding(horizontal = 24.dp)) {
        Text(
            title ?: stringResource(Res.string.sound_sheet_title),
            style = TaqvoText.screenTitle,
            color = colors.textPrimary,
        )
        if (sectionLabel != null) {
            Spacer(Modifier.height(16.dp))
            SectionLabel(sectionLabel, gutter = 0.dp)
        }
        TaqvoCard(Modifier.padding(top = if (sectionLabel != null) 0.dp else 16.dp).selectableGroup()) {
            options.forEachIndexed { i, sound ->
                if (i > 0) CardDivider()
                AudioOptionRow(
                    label = soundDisplayName(sound),
                    caption = subtitle(sound, voice),
                    selected = sound == current,
                    onSelect = { onPick(sound) },
                    onPreview = if (sound == PrayerSound.SILENT) null else ({ onPreview(sound) }),
                    previewDescription = stringResource(Res.string.sound_a11y_preview, soundDisplayName(sound)),
                )
            }
        }
        if (muted) {
            Text(
                stringResource(Res.string.sound_sheet_muted_hint),
                style = TaqvoText.caption,
                color = colors.accent,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        // Only iOS stops a notification sound at 30 seconds, so only iOS says so; Android plays the
        // whole adhan and needs no note (the owner's call, 4 Nov 2026).
        if (isIosPlatform) {
            Text(
                stringResource(
                    Res.string.sound_sheet_footnote,
                    LocalPlatformFormat.current.localizedDigits(SOUND_CAP_SECONDS),
                ),
                style = TaqvoText.caption,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            )
        } else {
            Spacer(Modifier.height(24.dp))
        }
        extra?.invoke(this)
    }
}

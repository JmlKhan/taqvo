package com.taqvo.app.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.notifications.currentCustomAdhan
import com.taqvo.app.notifications.customAdhanSupported
import com.taqvo.app.notifications.rememberCustomAdhanPicker
import com.taqvo.app.notifications.removeCustomAdhan
import kotlin.time.Duration.Companion.milliseconds
import com.taqvo.app.resources.adhan_voice_taqvo_label
import com.taqvo.app.resources.adhan_voice_phone_label
import com.taqvo.app.resources.adhan_voice_choose
import com.taqvo.app.resources.adhan_voice_choose_detail
import com.taqvo.app.resources.adhan_voice_custom
import com.taqvo.app.resources.adhan_voice_custom_detail
import com.taqvo.app.resources.adhan_voice_choose_another
import com.taqvo.app.resources.adhan_voice_remove
import com.taqvo.app.resources.adhan_voice_pick_failed
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.TaqvoCard
import com.taqvo.app.domain.AdhanVoice
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.i18n.adhanVoiceDisplayName
import com.taqvo.app.notifications.SoundAssets
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.adhan_voice_azeez_detail
import com.taqvo.app.resources.adhan_voice_azemi_detail
import com.taqvo.app.resources.adhan_voice_madinah_detail
import com.taqvo.app.resources.adhan_voice_makkah_detail
import com.taqvo.app.resources.adhan_voice_original_detail
import com.taqvo.app.resources.adhan_voice_sheet_title
import com.taqvo.app.resources.sound_a11y_preview

/**
 * Who the recording is and how long it runs — the two things a listener needs before pressing
 * play. The length is the *complete* adhan, because that is what the play button here auditions;
 * the 30-second clip a notification actually carries is the sound sheet's business.
 */
@Composable
private fun caption(voice: AdhanVoice): String {
    val length = LocalPlatformFormat.current.localizedClipLength(SoundAssets.fullAdhanDuration(voice))
    return when (voice) {
        AdhanVoice.ORIGINAL -> stringResource(Res.string.adhan_voice_original_detail, length)
        AdhanVoice.AZEEZ -> stringResource(Res.string.adhan_voice_azeez_detail, length)
        AdhanVoice.AZEMI -> stringResource(Res.string.adhan_voice_azemi_detail, length)
        AdhanVoice.MAKKAH -> stringResource(Res.string.adhan_voice_makkah_detail, length)
        AdhanVoice.MADINAH -> stringResource(Res.string.adhan_voice_madinah_detail, length)
        AdhanVoice.CUSTOM -> ""
    }
}

/**
 * One voice for all five prayers. The same row idiom as the sound sheet — and literally the same
 * composable — because the two sheets are the same question asked twice, and a listener moving
 * between them should not have to learn a second layout.
 *
 * The play button plays the **complete** adhan rather than the notification clip: a voice is
 * chosen by how it sounds over a couple of minutes, not by its first thirty seconds.
 */
@Composable
fun AdhanVoiceSheet(
    current: AdhanVoice,
    onPick: (AdhanVoice) -> Unit,
    onPreview: (AdhanVoice) -> Unit,
) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Text(
            stringResource(Res.string.adhan_voice_sheet_title),
            style = TaqvoText.screenTitle,
            color = colors.textPrimary,
        )
        if (customAdhanSupported) {
            Spacer(Modifier.height(16.dp))
            SectionLabel(stringResource(Res.string.adhan_voice_taqvo_label), gutter = 0.dp)
        }
        TaqvoCard(Modifier.padding(top = if (customAdhanSupported) 0.dp else 16.dp, bottom = 24.dp).selectableGroup()) {
            AdhanVoice.entries.filter { it != AdhanVoice.CUSTOM }.forEachIndexed { i, voice ->
                if (i > 0) CardDivider()
                AudioOptionRow(
                    label = adhanVoiceDisplayName(voice),
                    caption = caption(voice),
                    selected = voice == current,
                    onSelect = { onPick(voice) },
                    onPreview = { onPreview(voice) },
                    previewDescription = stringResource(Res.string.sound_a11y_preview, adhanVoiceDisplayName(voice)),
                )
            }
        }
        if (customAdhanSupported) {
            // "From your phone", last (the owner's design of 3 Nov 2026): Taqvo's own adhans stay
            // the default, and a recording chosen here plays only on this phone.
            var custom by remember { mutableStateOf(currentCustomAdhan()) }
            var failed by remember { mutableStateOf(false) }
            val pick = rememberCustomAdhanPicker { info ->
                failed = info == null
                if (info != null) {
                    custom = info
                    onPick(AdhanVoice.CUSTOM)
                }
            }
            SectionLabel(stringResource(Res.string.adhan_voice_phone_label), gutter = 0.dp)
            TaqvoCard(Modifier.selectableGroup()) {
                val chosen = custom
                if (chosen == null) {
                    TaqvoRow(
                        stringResource(Res.string.adhan_voice_choose),
                        subtitle = stringResource(Res.string.adhan_voice_choose_detail),
                        onClick = pick,
                    )
                } else {
                    AudioOptionRow(
                        label = stringResource(Res.string.adhan_voice_custom),
                        caption = stringResource(
                            Res.string.adhan_voice_custom_detail,
                            chosen.name,
                            format.localizedClipLength(chosen.durationMs.milliseconds),
                        ),
                        selected = current == AdhanVoice.CUSTOM,
                        onSelect = { onPick(AdhanVoice.CUSTOM) },
                        onPreview = { onPreview(AdhanVoice.CUSTOM) },
                        previewDescription = stringResource(Res.string.sound_a11y_preview, stringResource(Res.string.adhan_voice_custom)),
                    )
                    CardDivider()
                    Row(Modifier.fillMaxWidth()) {
                        TextButton(onClick = pick, modifier = Modifier.weight(1f)) {
                            Text(stringResource(Res.string.adhan_voice_choose_another), color = colors.accent)
                        }
                        TextButton(
                            onClick = {
                                removeCustomAdhan()
                                custom = null
                                if (current == AdhanVoice.CUSTOM) onPick(AdhanVoice.ORIGINAL)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(Res.string.adhan_voice_remove), color = colors.textSecondary)
                        }
                    }
                }
            }
            if (failed) {
                Spacer(Modifier.height(10.dp))
                SettingsNote(stringResource(Res.string.adhan_voice_pick_failed), gutter = 0.dp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

package com.taqvo.app.feature.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.RowToggle
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.design.components.drawChevron
import com.taqvo.app.feature.recitation.ReciterMonogram
import com.taqvo.app.feature.recitation.RecitationState
import com.taqvo.app.feature.recitation.RecitationStorage
import com.taqvo.app.feature.recitation.WholeQuran
import com.taqvo.app.feature.recitation.dataSize
import com.taqvo.app.feature.recitation.downloadFailureSentence
import com.taqvo.app.feature.recitation.reciterName
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.quran.TextKind
import com.taqvo.app.recitation.Reciter
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.recitation_cancel
import com.taqvo.app.resources.recitation_reciter
import com.taqvo.app.resources.recitation_read_aloud_detail
import com.taqvo.app.resources.recitation_read_aloud_get_voice
import com.taqvo.app.resources.recitation_read_aloud_heading
import com.taqvo.app.resources.recitation_read_aloud_missing
import com.taqvo.app.resources.recitation_read_aloud_tafsir
import com.taqvo.app.resources.recitation_read_aloud_translation
import com.taqvo.app.resources.recitation_settings_auto_download
import com.taqvo.app.resources.recitation_settings_batch
import com.taqvo.app.resources.recitation_settings_credit
import com.taqvo.app.resources.recitation_settings_downloads
import com.taqvo.app.resources.recitation_settings_mobile_data
import com.taqvo.app.resources.recitation_settings_surahs
import com.taqvo.app.resources.recitation_settings_surahs_one
import com.taqvo.app.resources.recitation_settings_whole_quran
import com.taqvo.app.resources.settings_recitation

/**
 * Settings › Quran › **Recitation** (spec §5.6): the voice, whether a download may leave Wi-Fi,
 * what is on the phone and what the rest of the Quran would cost.
 *
 * It owns nothing. Every value comes from [RecitationState], which is the same value the reader's
 * header button and the player bar are drawn from, so this screen can never disagree with them
 * about which voice is chosen or what is downloading; [storage] is the one thing state cannot
 * carry, because it is measured off the disk and the caller refreshes it when the registry moves.
 */
@Composable
fun RecitationSettingsScreen(
    state: RecitationState,
    storage: RecitationStorage,
    onBack: () -> Unit,
    /** Opening this screen is engagement with recitation (privacy spec §2.2). */
    onOpened: () -> Unit,
    onOpenPicker: () -> Unit,
    onSetMobileData: (Boolean) -> Unit,
    onSetAutoDownload: (Boolean) -> Unit,
    onOpenDownloads: (String) -> Unit,
    onDownloadWholeQuran: () -> Unit,
    onCancelWholeQuran: () -> Unit,
    onSetReadAloud: (Boolean) -> Unit = {},
    onGetVoice: () -> Unit = {},
) {
    LaunchedEffect(Unit) { onOpened() }
    SettingsScaffold(stringResource(Res.string.settings_recitation), onBack) {
        SettingsCard {
            TaqvoRow(
                stringResource(Res.string.recitation_reciter),
                // Blank rather than a placeholder before the catalogue has loaded: it fills in a
                // frame later, and "Not set" would be a lie about a setting that always has a value.
                value = state.reciter?.let { reciterName(it) }.orEmpty(),
                onClick = onOpenPicker,
            )
            CardDivider()
            TaqvoRow(
                stringResource(Res.string.recitation_settings_mobile_data),
                onClick = { onSetMobileData(!state.downloadOnMobileData) },
                ripple = false,
                toggle = RowToggle(state.downloadOnMobileData, onSetMobileData),
            )
            CardDivider()
            // Spec §15.3: the sheet's switch, here so it can be turned off again.
            TaqvoRow(
                stringResource(Res.string.recitation_settings_auto_download),
                onClick = { onSetAutoDownload(!state.autoDownload) },
                ripple = false,
                toggle = RowToggle(state.autoDownload, onSetAutoDownload),
            )
        }

        // Read-aloud (spec §6): its own card, shown only where the phone can read the language.
        val readAloud = state.readAloud
        if (readAloud != null) {
            val format = LocalPlatformFormat.current
            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(Res.string.recitation_read_aloud_heading))
            SettingsCard {
                TaqvoRow(
                    stringResource(
                        if (readAloud.kind == TextKind.TAFSIR) Res.string.recitation_read_aloud_tafsir
                        else Res.string.recitation_read_aloud_translation,
                    ),
                    onClick = { onSetReadAloud(!readAloud.enabled) },
                    ripple = false,
                    toggle = RowToggle(readAloud.enabled, onSetReadAloud),
                )
                // In-sentence casing (spec §6 fix round 1): both captions below read the language
                // name mid-sentence, so French and Indonesian must keep their own lower case.
                val language = format.languageNameInSentence(readAloud.language)
                Text(
                    if (readAloud.missingEngine != null) {
                        stringResource(Res.string.recitation_read_aloud_missing, language)
                    } else {
                        stringResource(Res.string.recitation_read_aloud_detail, readAloud.translationName, language)
                    },
                    style = TaqvoText.caption,
                    color = LocalTaqvoColors.current.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                )
                if (readAloud.missingEngine != null) {
                    CardDivider()
                    QuietRow(stringResource(Res.string.recitation_read_aloud_get_voice), LocalTaqvoColors.current.accent, onGetVoice)
                }
            }
        }

        val withDownloads = state.reciterDownloads
        if (withDownloads.isNotEmpty() && storage.total > 0L) {
            Spacer(Modifier.height(28.dp))
            // The heading carries the total, so the one number a reader came here for is the
            // first thing on the screen rather than a sum they have to do across the rows.
            SectionLabel(stringResource(Res.string.recitation_settings_downloads, dataSize(storage.total)))
            SettingsCard {
                withDownloads.forEachIndexed { index, reciter ->
                    if (index > 0) CardDivider()
                    ReciterStorageRow(
                        reciter = reciter,
                        surahs = state.downloadedByReciter[reciter.id].orEmpty().size,
                        bytes = storage.of(reciter.id),
                        onClick = { onOpenDownloads(reciter.id) },
                    )
                }
            }
        }

        val whole = state.wholeQuran
        if (whole != null) {
            Spacer(Modifier.height(if (withDownloads.isEmpty()) 28.dp else 14.dp))
            WholeQuranAction(whole, onDownloadWholeQuran, onCancelWholeQuran)
        }

        Spacer(Modifier.height(20.dp))
        // A sentence, not a link: the credits row is one tap away in the same tab, and a second
        // way into the same screen would be two answers to one question.
        SettingsNote(stringResource(Res.string.recitation_settings_credit))
    }
}

/** One reciter with something on the phone: monogram, name, "12 surahs · 298 MB", chevron. */
@Composable
private fun ReciterStorageRow(reciter: Reciter, surahs: Int, bytes: Long, onClick: () -> Unit) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    val forward = LocalLayoutDirection.current == LayoutDirection.Ltr
    val caption = stringResource(
        if (surahs == 1) Res.string.recitation_settings_surahs_one else Res.string.recitation_settings_surahs,
        format.localizedDigits(surahs),
        dataSize(bytes),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 32 dp, not the picker's 44: this is a list of storage, and a disc the size of the one
        // that auditions a voice would make it read as a second place to choose one.
        ReciterMonogram(reciter, 32.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(reciterName(reciter), style = TaqvoText.rowLabel, color = colors.textPrimary, maxLines = 1)
            Text(caption, style = TaqvoText.caption, color = colors.textSecondary, maxLines = 1)
        }
        Canvas(Modifier.size(14.dp)) { drawChevron(colors.textTertiary, pointsForward = forward) }
    }
}

/**
 * "Download the whole Quran · 0.87 GB" (spec §12.8), and what it becomes once it is running: the
 * count, a thin line under it and a Cancel.
 *
 * Quiet on purpose — accent text on the card's own surface rather than a filled button. It is an
 * 0.9 GB commitment offered on a settings screen nobody came to to spend a gigabyte, and it must
 * not out-shout the reciter row above it.
 */
@Composable
private fun WholeQuranAction(whole: WholeQuran, onDownload: () -> Unit, onCancel: () -> Unit) {
    val colors = LocalTaqvoColors.current
    val format = LocalPlatformFormat.current
    SettingsCard {
        when (whole) {
            is WholeQuran.Offer -> QuietRow(
                text = stringResource(Res.string.recitation_settings_whole_quran, dataSize(whole.bytes)),
                tint = colors.accent,
                onClick = onDownload,
            )
            is WholeQuran.Running -> {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        stringResource(
                            Res.string.recitation_settings_batch,
                            format.localizedDigits(whole.done),
                            format.localizedDigits(whole.total),
                        ),
                        style = TaqvoText.rowLabel,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(10.dp))
                    BatchProgressLine(whole.fraction)
                }
                CardDivider()
                QuietRow(stringResource(Res.string.recitation_cancel), colors.textSecondary, onCancel)
            }
            is WholeQuran.Failed -> {
                Text(
                    downloadFailureSentence(whole.reason),
                    style = TaqvoText.caption,
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                )
                CardDivider()
                // The offer again rather than a "Retry": the reader is being asked for the same
                // gigabyte, and the price is the part of that sentence that matters.
                QuietRow(
                    text = stringResource(Res.string.recitation_settings_whole_quran, dataSize(whole.bytes)),
                    tint = colors.accent,
                    onClick = onDownload,
                )
            }
        }
    }
}

/**
 * The batch's progress line: 3 dp, the accent over the hairline, filling from the reading edge —
 * which under an Arabic UI is the right-hand one, hence the start alignment rather than a plain
 * `fillMaxWidth(fraction)` that would grow from the left in both languages.
 */
@Composable
private fun BatchProgressLine(fraction: Float) {
    val colors = LocalTaqvoColors.current
    val width by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(400), label = "batchProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(colors.hairline),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth(width).height(3.dp).background(colors.accent))
    }
}

/** A row of the card whose whole content is one coloured word or phrase: an action, not a value. */
@Composable
private fun QuietRow(text: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = TaqvoText.rowLabel.copy(fontWeight = FontWeight.SemiBold), color = tint, maxLines = 2)
    }
}

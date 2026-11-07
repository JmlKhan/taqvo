package com.taqvo.app.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.ayah.AyahReminderSettings
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.CheckMark
import com.taqvo.app.design.components.RowToggle
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.feature.onboarding.LAST_MINUTE
import com.taqvo.app.feature.onboarding.StepperRow
import com.taqvo.app.feature.onboarding.TIME_STEP
import com.taqvo.app.feature.onboarding.windowed
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.notifications.openAppNotificationSettings
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.daily_ayahs_denied
import com.taqvo.app.resources.daily_ayahs_format_label
import com.taqvo.app.resources.daily_ayahs_note
import com.taqvo.app.resources.daily_ayahs_open_system
import com.taqvo.app.resources.daily_ayahs_toggle
import com.taqvo.app.resources.onb_format_arabic
import com.taqvo.app.resources.onb_format_both
import com.taqvo.app.resources.onb_format_translation
import com.taqvo.app.resources.onb_reminders_count
import com.taqvo.app.resources.onb_reminders_count_value
import com.taqvo.app.resources.onb_reminders_end
import com.taqvo.app.resources.onb_reminders_fewer
import com.taqvo.app.resources.onb_reminders_later
import com.taqvo.app.resources.onb_reminders_more
import com.taqvo.app.resources.onb_reminders_sooner
import com.taqvo.app.resources.onb_reminders_start
import com.taqvo.app.resources.settings_daily_ayahs

/**
 * Settings › Daily ayahs: the reminders onboarding set up (spec, screen 09) and how an ayah reads
 * (screen 17), changed or switched off. Every change is saved and the plan armed again by the
 * caller. [denied] is the system's refusal from onboarding: switching on then asks again, and
 * where the system will not ask twice the note points to its own settings.
 */
@Composable
fun DailyAyahsSettingsScreen(
    settings: AyahReminderSettings,
    format: AyahFormat,
    onSettings: (AyahReminderSettings) -> Unit,
    onEnable: () -> Unit,
    onFormat: (AyahFormat) -> Unit,
    onBack: () -> Unit,
) {
    val platform = LocalPlatformFormat.current
    fun clock(minute: Int) = platform.clockTime(minute / 60, minute % 60)
    val max = AyahReminderSettings.maxCountFor(settings.startMinute, settings.endMinute)
    SettingsScaffold(stringResource(Res.string.settings_daily_ayahs), onBack) {
        SettingsCard {
            TaqvoRow(
                stringResource(Res.string.daily_ayahs_toggle),
                toggle = RowToggle(settings.enabled) { on -> if (on) onEnable() else onSettings(settings.copy(enabled = false)) },
            )
            if (settings.enabled) {
                CardDivider()
                StepperRow(
                    label = stringResource(Res.string.onb_reminders_count),
                    value = stringResource(Res.string.onb_reminders_count_value, platform.localizedDigits(settings.count.coerceAtMost(max))),
                    minusLabel = stringResource(Res.string.onb_reminders_fewer),
                    plusLabel = stringResource(Res.string.onb_reminders_more),
                    canMinus = settings.count > AyahReminderSettings.MIN_COUNT,
                    canPlus = settings.count < max,
                    onMinus = { onSettings(settings.copy(count = settings.count - 1)) },
                    onPlus = { onSettings(settings.copy(count = settings.count + 1)) },
                )
                CardDivider()
                StepperRow(
                    label = stringResource(Res.string.onb_reminders_start),
                    value = clock(settings.startMinute),
                    minusLabel = stringResource(Res.string.onb_reminders_sooner),
                    plusLabel = stringResource(Res.string.onb_reminders_later),
                    canMinus = settings.startMinute - TIME_STEP >= 0,
                    canPlus = settings.startMinute + TIME_STEP < settings.endMinute,
                    onMinus = { onSettings(windowed(settings, settings.startMinute - TIME_STEP, settings.endMinute)) },
                    onPlus = { onSettings(windowed(settings, settings.startMinute + TIME_STEP, settings.endMinute)) },
                )
                CardDivider()
                StepperRow(
                    label = stringResource(Res.string.onb_reminders_end),
                    value = clock(settings.endMinute),
                    minusLabel = stringResource(Res.string.onb_reminders_sooner),
                    plusLabel = stringResource(Res.string.onb_reminders_later),
                    canMinus = settings.endMinute - TIME_STEP > settings.startMinute,
                    canPlus = settings.endMinute + TIME_STEP <= LAST_MINUTE,
                    onMinus = { onSettings(windowed(settings, settings.startMinute, settings.endMinute - TIME_STEP)) },
                    onPlus = { onSettings(windowed(settings, settings.startMinute, settings.endMinute + TIME_STEP)) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (settings.denied && !settings.enabled) {
            SettingsNote(stringResource(Res.string.daily_ayahs_denied))
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                TaqvoRow(stringResource(Res.string.daily_ayahs_open_system), onClick = { openAppNotificationSettings() })
            }
        } else {
            SettingsNote(stringResource(Res.string.daily_ayahs_note))
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(Res.string.daily_ayahs_format_label))
        SettingsCard(Modifier.selectableGroup()) {
            listOf(
                AyahFormat.TRANSLATION to Res.string.onb_format_translation,
                AyahFormat.ARABIC_TRANSLATION to Res.string.onb_format_both,
                AyahFormat.ARABIC to Res.string.onb_format_arabic,
            ).forEachIndexed { index, (option, label) ->
                if (index > 0) CardDivider()
                TaqvoRow(
                    label = stringResource(label),
                    onClick = { onFormat(option) },
                    selectable = true,
                    selected = option == format,
                    trailing = { if (option == format) CheckMark() },
                )
            }
        }
    }
}

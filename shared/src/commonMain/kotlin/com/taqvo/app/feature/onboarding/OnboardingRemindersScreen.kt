package com.taqvo.app.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahReminderSettings
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.app_name
import com.taqvo.app.resources.onb_reminders_count
import com.taqvo.app.resources.onb_reminders_count_value
import com.taqvo.app.resources.onb_reminders_cta
import com.taqvo.app.resources.onb_reminders_end
import com.taqvo.app.resources.onb_reminders_fewer
import com.taqvo.app.resources.onb_reminders_later
import com.taqvo.app.resources.onb_reminders_more
import com.taqvo.app.resources.onb_reminders_now
import com.taqvo.app.resources.onb_reminders_sooner
import com.taqvo.app.resources.onb_reminders_start
import com.taqvo.app.resources.onb_reminders_subtitle
import com.taqvo.app.resources.onb_reminders_title

/** An ayah as a notification shows it: the reference over the words. */
data class AyahPreviewText(val reference: String, val text: String)

/** How far one tap moves the start or the end of the window. */
internal const val TIME_STEP = 30

/**
 * Screen 09: the reader sets the daily ayahs up, then the system asks about notifications.
 *
 * The button saves and opens the system prompt; it is not labelled "Allow", since a custom button
 * that reads like the system's own is what App Review turns down (guideline 5.1.1(iv), the rule
 * the previous onboarding was built to). While the prompt is up the button shows a spinner.
 */
@Composable
internal fun RemindersScreen(
    progress: Float?,
    settings: AyahReminderSettings,
    onSettings: (AyahReminderSettings) -> Unit,
    preview: AyahPreviewText?,
    asking: Boolean,
    onSave: () -> Unit,
) {
    val format = LocalPlatformFormat.current
    fun clock(minute: Int) = format.clockTime(minute / 60, minute % 60)
    val max = AyahReminderSettings.maxCountFor(settings.startMinute, settings.endMinute)
    OnbPage(
        progress = progress,
        onSkip = null,
        centred = false,
        content = {
            Spacer(Modifier.height(8.dp))
            OnbTitle(stringResource(Res.string.onb_reminders_title))
            Spacer(Modifier.height(10.dp))
            OnbBody(stringResource(Res.string.onb_reminders_subtitle))
            Spacer(Modifier.height(24.dp))
            NotificationPreview(preview)
            Spacer(Modifier.height(16.dp))
            SettingsCard {
                StepperRow(
                    label = stringResource(Res.string.onb_reminders_count),
                    value = stringResource(Res.string.onb_reminders_count_value, format.localizedDigits(settings.count.coerceAtMost(max))),
                    minusLabel = stringResource(Res.string.onb_reminders_fewer),
                    plusLabel = stringResource(Res.string.onb_reminders_more),
                    canMinus = settings.count > AyahReminderSettings.MIN_COUNT,
                    canPlus = settings.count < max,
                    onMinus = { onSettings(settings.copy(count = (settings.count - 1).coerceAtLeast(AyahReminderSettings.MIN_COUNT))) },
                    onPlus = { onSettings(settings.copy(count = (settings.count + 1).coerceAtMost(max))) },
                )
                CardDivider()
                StepperRow(
                    label = stringResource(Res.string.onb_reminders_start),
                    value = clock(settings.startMinute),
                    minusLabel = stringResource(Res.string.onb_reminders_sooner),
                    plusLabel = stringResource(Res.string.onb_reminders_later),
                    canMinus = settings.startMinute - TIME_STEP >= 0,
                    // The window always keeps room for at least one gap.
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
        },
        footer = {
            Box(contentAlignment = Alignment.Center) {
                TaqvoPrimaryButton(
                    if (asking) "" else stringResource(Res.string.onb_reminders_cta),
                    onSave,
                    enabled = !asking,
                )
                if (asking) CircularProgressIndicator(Modifier.size(22.dp), color = LocalTaqvoColors.current.background, strokeWidth = 2.dp)
            }
        },
    )
}

/** The last start of a half hour in the day, 23:30: a window never crosses midnight. */
internal const val LAST_MINUTE = 23 * 60 + 30

/** The window moved, with the count cut to what it now holds. */
internal fun windowed(settings: AyahReminderSettings, start: Int, end: Int): AyahReminderSettings =
    settings.copy(
        startMinute = start,
        endMinute = end,
        count = settings.count.coerceAtMost(AyahReminderSettings.maxCountFor(start, end)),
    )

/** A notification as the phone would show it: the app's mark and name, "now", the ayah. */
@Composable
internal fun NotificationPreview(preview: AyahPreviewText?, modifier: Modifier = Modifier) {
    val colors = LocalTaqvoColors.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.hairline, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(colors.textPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(28.dp)) { drawMihrab(colors.background, colors.ring) }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.app_name), color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(stringResource(Res.string.onb_reminders_now), color = colors.textTertiary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text(preview?.text.orEmpty(), color = colors.textPrimary, fontSize = 14.sp, lineHeight = 19.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
            if (preview != null) {
                Spacer(Modifier.height(4.dp))
                Text("— ${preview.reference}", color = colors.textSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    val colors = LocalTaqvoColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .border(1.dp, colors.hairline, RoundedCornerShape(20.dp)),
    ) { content() }
}

/** A label, and a value between − and + buttons that a screen reader names in full. */
@Composable
internal fun StepperRow(
    label: String,
    value: String,
    minusLabel: String,
    plusLabel: String,
    canMinus: Boolean,
    canPlus: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val colors = LocalTaqvoColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepButton("−", "$minusLabel, $label", canMinus, onMinus)
            Text(value, color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            StepButton("+", "$plusLabel, $label", canPlus, onPlus)
        }
    }
}

@Composable
private fun StepButton(glyph: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalTaqvoColors.current
    Box(
        Modifier
            .size(36.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .background(colors.background)
            .border(1.dp, colors.hairline, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}


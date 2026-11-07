package com.taqvo.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.components.RadioMark
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.components.TaqvoSegmented
import com.taqvo.app.design.components.TaqvoTextLink
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.notifications_allow_exact_alarms
import com.taqvo.app.resources.onb_continue
import com.taqvo.app.resources.onb_format_arabic
import com.taqvo.app.resources.onb_format_both
import com.taqvo.app.resources.onb_format_recommended
import com.taqvo.app.resources.onb_format_subtitle
import com.taqvo.app.resources.onb_format_title
import com.taqvo.app.resources.onb_format_translation
import com.taqvo.app.resources.onb_guide_done
import com.taqvo.app.resources.onb_guide_home_tab
import com.taqvo.app.resources.onb_guide_lock_1
import com.taqvo.app.resources.onb_guide_lock_2
import com.taqvo.app.resources.onb_guide_lock_3
import com.taqvo.app.resources.onb_guide_lock_4
import com.taqvo.app.resources.onb_guide_lock_5
import com.taqvo.app.resources.onb_guide_lock_6
import com.taqvo.app.resources.onb_guide_lock_tab
import com.taqvo.app.resources.onb_guide_lock_title
import com.taqvo.app.resources.onb_guide_subtitle
import com.taqvo.app.resources.onb_welcome
import com.taqvo.app.resources.onb_welcome_name
import com.taqvo.app.resources.onboarding_continue
import com.taqvo.app.resources.onboarding_exact_body
import com.taqvo.app.resources.onboarding_exact_title
import com.taqvo.app.resources.onboarding_location_body
import com.taqvo.app.resources.onboarding_location_title
import com.taqvo.app.resources.onboarding_notifications_secondary
import com.taqvo.app.resources.onboarding_widget_title
import com.taqvo.app.wallpaper.WallpaperStyle
import com.taqvo.app.widget.WidgetAddPath

/** Screen 16: the reader's wallpaper and a welcome by name; it moves on by itself or on a tap. */
@Composable
internal fun WelcomeScreen(style: WallpaperStyle, name: String?, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(1_500)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(style.colors.top, style.colors.bottom)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone)
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        OnbTitle(
            if (name != null) stringResource(Res.string.onb_welcome_name, name) else stringResource(Res.string.onb_welcome),
            color = style.colors.ink,
        )
    }
}

/**
 * Screen 17: how an ayah reads on the widgets and in the reminders. Arabic with the translation
 * is chosen at first, and the preview above the choices follows the pick.
 */
@Composable
internal fun AyahFormatScreen(
    progress: Float?,
    format: AyahFormat,
    onFormat: (AyahFormat) -> Unit,
    preview: (AyahFormat) -> AyahPreviewText?,
    onContinue: () -> Unit,
) {
    OnbPage(
        progress = progress,
        onSkip = null,
        centred = false,
        content = {
            Spacer(Modifier.height(8.dp))
            OnbTitle(stringResource(Res.string.onb_format_title))
            Spacer(Modifier.height(10.dp))
            OnbBody(stringResource(Res.string.onb_format_subtitle))
            Spacer(Modifier.height(20.dp))
            NotificationPreview(preview(format))
            Spacer(Modifier.height(16.dp))
            SettingsCard {
                Column(Modifier.selectableGroup()) {
                    FormatRow(Res.string.onb_format_translation, recommended = true, format == AyahFormat.TRANSLATION) { onFormat(AyahFormat.TRANSLATION) }
                    FormatRow(Res.string.onb_format_both, recommended = false, format == AyahFormat.ARABIC_TRANSLATION) { onFormat(AyahFormat.ARABIC_TRANSLATION) }
                    FormatRow(Res.string.onb_format_arabic, recommended = false, format == AyahFormat.ARABIC) { onFormat(AyahFormat.ARABIC) }
                }
            }
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

@Composable
private fun FormatRow(label: StringResource, recommended: Boolean, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalTaqvoColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(label), color = colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (recommended) {
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.onb_format_recommended),
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, colors.accent, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        RadioMark(selected)
    }
}

/**
 * The place for the prayer times: one button, and it opens the system prompt (App Review
 * guideline 5.1.1(iv); 1.0.0 (34) was rejected for a second choice beside it). A refusal opens
 * the city search instead ([locationAnswer]).
 */
@Composable
internal fun LocationScreen(progress: Float?, onContinue: () -> Unit) {
    OnbPage(
        progress = progress,
        onSkip = null,
        content = {
            PinMark()
            Spacer(Modifier.height(28.dp))
            OnbTitle(stringResource(Res.string.onboarding_location_title))
            Spacer(Modifier.height(12.dp))
            OnbBody(stringResource(Res.string.onboarding_location_body))
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onboarding_continue), onContinue) },
    )
}

/** Android 14+: "Alarms & reminders", so an adhan is not up to an hour late. */
@Composable
internal fun ExactAlarmsScreen(progress: Float?, onAllow: () -> Unit, onLater: () -> Unit) {
    OnbPage(
        progress = progress,
        onSkip = null,
        content = {
            ClockMark()
            Spacer(Modifier.height(28.dp))
            OnbTitle(stringResource(Res.string.onboarding_exact_title))
            Spacer(Modifier.height(12.dp))
            OnbBody(stringResource(Res.string.onboarding_exact_body))
        },
        footer = {
            TaqvoPrimaryButton(stringResource(Res.string.notifications_allow_exact_alarms), onAllow)
            TaqvoTextLink(stringResource(Res.string.onboarding_notifications_secondary), onClick = onLater)
        },
    )
}

/**
 * Screen 18 on iOS: the Lock Screen steps and the home screen's, a tab each. The home screen's
 * are the previous onboarding's own words for this phone's iOS ([widgetStepBodyKey]).
 */
@Composable
internal fun IosWidgetGuideScreen(progress: Float?, addPath: WidgetAddPath, onDone: () -> Unit) {
    val colors = LocalTaqvoColors.current
    var tab by remember { mutableIntStateOf(0) }
    OnbPage(
        progress = progress,
        onSkip = null,
        centred = false,
        content = {
            Spacer(Modifier.height(8.dp))
            OnbTitle(stringResource(if (tab == 0) Res.string.onb_guide_lock_title else Res.string.onboarding_widget_title))
            Spacer(Modifier.height(10.dp))
            OnbBody(stringResource(Res.string.onb_guide_subtitle))
            Spacer(Modifier.height(18.dp))
            TaqvoSegmented(
                listOf(stringResource(Res.string.onb_guide_lock_tab), stringResource(Res.string.onb_guide_home_tab)),
                tab,
                { tab = it },
            )
            Spacer(Modifier.height(18.dp))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (tab == 0) {
                        listOf(
                            Res.string.onb_guide_lock_1, Res.string.onb_guide_lock_2, Res.string.onb_guide_lock_3,
                            Res.string.onb_guide_lock_4, Res.string.onb_guide_lock_5, Res.string.onb_guide_lock_6,
                        ).forEachIndexed { index, step -> NumberedStep(index + 1, stringResource(step)) }
                    } else {
                        Text(stringResource(widgetStepBodyKey(addPath)), color = colors.textPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            }
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_guide_done), onDone) },
    )
}

@Composable
private fun NumberedStep(number: Int, text: String) {
    val colors = LocalTaqvoColors.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(colors.textPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(com.taqvo.app.i18n.LocalPlatformFormat.current.localizedDigits(number), color = colors.background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = colors.textPrimary, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
    }
}

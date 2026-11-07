package com.taqvo.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.domain.WidgetBackground
import com.taqvo.app.feature.settings.AyahWidgetPreview
import com.taqvo.app.feature.settings.WidgetPreview
import com.taqvo.app.feature.settings.sampleWidgetContent
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.onb_continue
import com.taqvo.app.resources.onb_showcase_home_title
import com.taqvo.app.resources.onb_showcase_lock_title
import com.taqvo.app.resources.onb_showcase_notifications_title
import com.taqvo.app.wallpaper.WallpaperStyle

/**
 * Screen 13. On iOS: the Lock Screen with the ayah card and the next prayer under the clock, and
 * a daily ayah arriving, over the reader's own wallpaper (spec §6.2). Android phones have no Lock
 * Screen widgets, so there the same phone shows the day's ayahs arriving as notifications.
 */
@Composable
internal fun ShowcaseLockScreen(
    progress: Float?,
    style: WallpaperStyle,
    ios: Boolean,
    preview: AyahPreviewText?,
    onContinue: () -> Unit,
) {
    val format = LocalPlatformFormat.current
    val ink = style.colors.ink
    OnbPage(
        progress = progress,
        onSkip = null,
        centred = false,
        content = {
            Spacer(Modifier.height(8.dp))
            OnbTitle(stringResource(if (ios) Res.string.onb_showcase_lock_title else Res.string.onb_showcase_notifications_title))
            Spacer(Modifier.height(24.dp))
            PhoneFrame(style) {
                Spacer(Modifier.height(28.dp))
                Text(format.clockTime(9, 41), color = ink, fontSize = 64.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                if (ios) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassCard(Modifier.weight(1.4f)) {
                            Text(preview?.reference.orEmpty(), color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(preview?.text.orEmpty(), color = ink, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        GlassCard(Modifier.weight(1f)) {
                            val next = sampleWidgetContent().rows.firstOrNull { it.isCurrent }
                            Text(next?.displayName.orEmpty(), color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(next?.clockTime.orEmpty(), color = ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Box(Modifier.clearAndSetSemantics {}) { NotificationPreview(preview) }
                if (!ios) {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.clearAndSetSemantics {}) { NotificationPreview(preview) }
                }
                Spacer(Modifier.height(28.dp))
            }
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

/** Screen 14: the home screen's ayah card and prayer times, the reader's own picks first. */
@Composable
internal fun ShowcaseHomeScreen(
    progress: Float?,
    style: WallpaperStyle,
    prayerFirst: Boolean,
    onContinue: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    OnbPage(
        progress = progress,
        onSkip = null,
        centred = false,
        content = {
            Spacer(Modifier.height(8.dp))
            OnbTitle(stringResource(Res.string.onb_showcase_home_title))
            Spacer(Modifier.height(24.dp))
            PhoneFrame(style) {
                Spacer(Modifier.height(16.dp))
                val ayah: @Composable () -> Unit = {
                    AyahWidgetPreview(background = WidgetBackground.FOLLOW_THEME, systemIsDark = dark, corner = 22.dp)
                }
                val prayer: @Composable () -> Unit = {
                    WidgetPreview(background = WidgetBackground.FOLLOW_THEME, systemIsDark = dark, content = null, corner = 22.dp)
                }
                if (prayerFirst) prayer() else ayah()
                Spacer(Modifier.height(12.dp))
                if (prayerFirst) ayah() else prayer()
                Spacer(Modifier.height(16.dp))
            }
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

/** A phone's screen in outline, its ground the reader's wallpaper look. Drawn, so not read out. */
@Composable
private fun PhoneFrame(style: WallpaperStyle, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = LocalTaqvoColors.current
    Column(
        Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(36.dp))
            .border(6.dp, colors.textPrimary.copy(alpha = 0.85f), RoundedCornerShape(36.dp))
            .background(Brush.verticalGradient(listOf(style.colors.top, style.colors.bottom)))
            .padding(horizontal = 16.dp),
        content = content,
    )
}

@Composable
private fun GlassCard(modifier: Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(10.dp),
        content = content,
    )
}

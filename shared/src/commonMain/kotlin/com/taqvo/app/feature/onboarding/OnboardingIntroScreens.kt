package com.taqvo.app.feature.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.WallpaperDusk
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.app_name
import com.taqvo.app.resources.onb_continue
import com.taqvo.app.resources.onb_intro_body
import com.taqvo.app.resources.onb_intro_title
import com.taqvo.app.resources.onb_journey_body
import com.taqvo.app.resources.onb_journey_title
import com.taqvo.app.resources.onb_name_placeholder
import com.taqvo.app.resources.onb_name_title
import com.taqvo.app.resources.onb_splash_cta
import com.taqvo.app.resources.onb_splash_tagline
import com.taqvo.app.resources.onb_streak_caption
import com.taqvo.app.resources.onb_streak_title
import com.taqvo.app.resources.onb_weekday_fri
import com.taqvo.app.resources.onb_weekday_mon
import com.taqvo.app.resources.onb_weekday_sat
import com.taqvo.app.resources.onb_weekday_sun
import com.taqvo.app.resources.onb_weekday_thu
import com.taqvo.app.resources.onb_weekday_tue
import com.taqvo.app.resources.onb_weekday_wed

/** Screen 01: the brand and its promise over the dusk wallpaper, the arch drawn large. */
@Composable
internal fun SplashScreen(onStart: () -> Unit) {
    val dusk = WallpaperDusk
    OnbPage(
        progress = null,
        onSkip = null,
        background = {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(dusk.top, dusk.bottom))))
            Canvas(Modifier.fillMaxSize()) {
                // A soft glow where the arch stands, as the sky behind a mosque at dusk.
                drawCircle(
                    Brush.radialGradient(listOf(dusk.accent.copy(alpha = 0.28f), Color.Transparent), center = Offset(size.width / 2, size.height * 0.36f), radius = size.width * 0.7f),
                    radius = size.width * 0.7f,
                    center = Offset(size.width / 2, size.height * 0.36f),
                )
            }
        },
        content = {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawMihrab(dusk.ink, dusk.accent) }
            }
            Spacer(Modifier.height(36.dp))
            OnbTitle(stringResource(Res.string.app_name), color = dusk.ink)
            Spacer(Modifier.height(10.dp))
            OnbBody(stringResource(Res.string.onb_splash_tagline), color = dusk.secondary)
        },
        footer = { LightButton(stringResource(Res.string.onb_splash_cta), onStart) },
    )
}

/** The CTA on a dark picture: the picture's own ink, whatever the app's theme. */
@Composable
internal fun LightButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalTaqvoColors provides LocalTaqvoColors.current.copy(textPrimary = Color(0xFFF7EEDC), background = Color(0xFF16160F)),
    ) {
        TaqvoPrimaryButton(text, onClick, enabled = enabled)
    }
}

/** Screen 02: what the questions are for, and that the answers stay on the phone. */
@Composable
internal fun IntroScreen(progress: Float?, onContinue: () -> Unit) {
    OnbPage(
        progress = progress,
        onSkip = null,
        content = {
            MihrabMark()
            Spacer(Modifier.height(28.dp))
            OnbTitle(stringResource(Res.string.onb_intro_title))
            Spacer(Modifier.height(12.dp))
            OnbBody(stringResource(Res.string.onb_intro_body))
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

/** Screen 03: one field, the keyboard up at once. Continue waits for a name; Skip does not. */
@Composable
internal fun NameScreen(
    progress: Float?,
    name: String,
    onName: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    val colors = LocalTaqvoColors.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val ready = cleanName(name) != null
    OnbPage(
        progress = progress,
        onSkip = onSkip,
        centred = false,
        content = {
            Spacer(Modifier.height(32.dp))
            OnbTitle(stringResource(Res.string.onb_name_title))
            Spacer(Modifier.height(32.dp))
            val placeholder = stringResource(Res.string.onb_name_placeholder)
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= NAME_MAX_LENGTH) onName(it) },
                singleLine = true,
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (ready) onContinue() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = placeholder },
                decorationBox = { field ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.hairline, RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (name.isEmpty()) {
                            Text(placeholder, color = colors.textTertiary, fontSize = 20.sp, textAlign = TextAlign.Center)
                        }
                        field()
                    }
                },
            )
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue, enabled = ready) },
    )
}

/** Screens 04, 05 and 08: a title over option cards. Continue waits for one pick (spec §0). */
@Composable
internal fun QuestionScreen(
    progress: Float?,
    title: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    OnbPage(
        progress = progress,
        onSkip = onSkip,
        centred = false,
        content = {
            Spacer(Modifier.height(16.dp))
            OnbTitle(title)
            Spacer(Modifier.height(28.dp))
            OptionList(options, selected, onToggle)
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue, enabled = selected.isNotEmpty()) },
    )
}

/** Screen 06: a breather between the questions, the arch lit from behind. */
@Composable
internal fun JourneyScreen(progress: Float?, onContinue: () -> Unit) {
    val colors = LocalTaqvoColors.current
    OnbPage(
        progress = progress,
        onSkip = null,
        content = {
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Brush.radialGradient(listOf(colors.ring.copy(alpha = 0.35f), Color.Transparent)), radius = size.minDimension / 2)
                }
                Canvas(Modifier.size(150.dp)) { drawMihrab(colors.textPrimary, colors.ring) }
            }
            Spacer(Modifier.height(28.dp))
            OnbTitle(stringResource(Res.string.onb_journey_title))
            Spacer(Modifier.height(12.dp))
            OnbBody(stringResource(Res.string.onb_journey_body))
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

/**
 * Screen 07: the streak, already on day one. The week starts today, so today's check is always
 * first, and it fills in as the screen appears.
 */
@Composable
internal fun StreakScreen(progress: Float?, today: DayOfWeek, dayOne: String, onContinue: () -> Unit) {
    val colors = LocalTaqvoColors.current
    val fill = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fill.animateTo(1f, tween(400)) }
    val initials = weekdayInitials()
    OnbPage(
        progress = progress,
        onSkip = null,
        content = {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawCrescent(colors.ring) }
                Text(dayOne, color = colors.textPrimary, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(24.dp))
            OnbTitle(stringResource(Res.string.onb_streak_title))
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.hairline, RoundedCornerShape(20.dp))
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    (0 until 7).forEach { offset ->
                        val day = DayOfWeek.entries[(today.ordinal + offset) % 7]
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(initials.getValue(day), color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            DayDot(done = offset == 0, fill = fill.value)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(Res.string.onb_streak_caption), color = colors.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        },
        footer = { TaqvoPrimaryButton(stringResource(Res.string.onb_continue), onContinue) },
    )
}

@Composable
private fun DayDot(done: Boolean, fill: Float) {
    val colors = LocalTaqvoColors.current
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (done) colors.ring.copy(alpha = fill) else colors.background)
            .border(1.5.dp, if (done) colors.ring else colors.hairline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done && fill > 0.5f) {
            Canvas(Modifier.size(14.dp)) {
                val w = size.width
                val tick = Path().apply { moveTo(w * 0.1f, w * 0.55f); lineTo(w * 0.4f, w * 0.85f); lineTo(w * 0.92f, w * 0.18f) }
                drawPath(tick, colors.background, style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.18f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }
        }
    }
}

/** A crescent: the moon's disc with a second disc taken out of it, and a few sparks around. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCrescent(tint: Color) {
    val r = size.minDimension * 0.42f
    val c = Offset(size.width / 2, size.height / 2)
    val moon = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(c, r))
    }
    val bite = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.42f, c.y - r * 0.22f), r * 0.86f))
    }
    val crescent = Path().apply { op(moon, bite, androidx.compose.ui.graphics.PathOperation.Difference) }
    drawPath(crescent, tint.copy(alpha = 0.28f))
    listOf(Offset(0.86f, 0.18f), Offset(0.95f, 0.5f), Offset(0.78f, 0.86f), Offset(0.12f, 0.12f)).forEach { (x, y) ->
        drawCircle(tint, radius = size.minDimension * 0.022f, center = Offset(size.width * x, size.height * y))
    }
}

@Composable
private fun weekdayInitials(): Map<DayOfWeek, String> = mapOf(
    DayOfWeek.MONDAY to Res.string.onb_weekday_mon,
    DayOfWeek.TUESDAY to Res.string.onb_weekday_tue,
    DayOfWeek.WEDNESDAY to Res.string.onb_weekday_wed,
    DayOfWeek.THURSDAY to Res.string.onb_weekday_thu,
    DayOfWeek.FRIDAY to Res.string.onb_weekday_fri,
    DayOfWeek.SATURDAY to Res.string.onb_weekday_sat,
    DayOfWeek.SUNDAY to Res.string.onb_weekday_sun,
).mapValues { (_, key: StringResource) -> stringResource(key) }

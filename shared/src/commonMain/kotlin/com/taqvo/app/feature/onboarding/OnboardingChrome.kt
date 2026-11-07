package com.taqvo.app.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CheckMark
import com.taqvo.app.design.contentWidth
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.onb_skip

/**
 * One onboarding screen (spec §0): a thin progress bar with Skip beside it on the questions, the
 * content in the middle, and the one call to action pinned to the foot.
 *
 * The content scrolls and the foot does not, so the way on is always on screen even with the
 * largest text on a short phone (the previous onboarding's rule, kept). When the content fits,
 * the weighted spacers centre it and there is nothing to scroll.
 *
 * [background] draws behind everything, edge to edge (the wallpaper screens); without one the
 * screen is the app's own ground.
 */
@Composable
internal fun OnbPage(
    progress: Float?,
    onSkip: (() -> Unit)?,
    modifier: Modifier = Modifier,
    background: (@Composable () -> Unit)? = null,
    centred: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalTaqvoColors.current
    Box(modifier.fillMaxSize().background(colors.background)) {
        background?.invoke()
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .contentWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnbTopBar(progress, onSkip)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val scroll = rememberScrollState()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll, enabled = scroll.canScrollForward || scroll.canScrollBackward)
                        .heightIn(min = maxHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (centred) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(24.dp))
                    content()
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(24.dp))
                }
            }
            footer()
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** The bar about 60 % of the width and Skip at the end, or empty room of the same height. */
@Composable
private fun OnbTopBar(progress: Float?, onSkip: (() -> Unit)?) {
    val colors = LocalTaqvoColors.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (progress != null) {
            val shown by animateFloatAsState(progress)
            Box(
                Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.hairline),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(shown.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
            }
        } else {
            Spacer(Modifier.size(0.dp))
        }
        if (onSkip != null) {
            Text(
                stringResource(Res.string.onb_skip),
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onSkip)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
internal fun OnbTitle(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color? = null) {
    Text(
        text,
        modifier = modifier,
        style = TaqvoText.screenTitle.copy(fontSize = 28.sp, lineHeight = 34.sp),
        color = color ?: LocalTaqvoColors.current.textPrimary,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun OnbBody(text: String, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color? = null) {
    Text(
        text,
        modifier = modifier,
        style = TaqvoText.caption.copy(fontSize = 16.sp, lineHeight = 23.sp),
        color = color ?: LocalTaqvoColors.current.textSecondary,
        textAlign = TextAlign.Center,
    )
}

/**
 * A multi-select option (screens 04, 05, 08): full width, the label at the start, a check box at
 * the end. Selected, the card takes the accent's edge and the box its check. A light tick of
 * haptics on every change (spec §0).
 */
@Composable
internal fun OptionCard(label: String, selected: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalTaqvoColors.current
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(colors.surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.hairline, shape)
            .toggleable(value = selected, role = Role.Checkbox) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggle()
            }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
        Spacer(Modifier.size(12.dp))
        Box(
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(colors.background)
                .border(if (selected) 2.dp else 1.5.dp, if (selected) colors.accent else colors.hairline, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) CheckMark(Modifier.size(18.dp))
        }
    }
}

/** The options of one question, stacked with even gaps. */
@Composable
internal fun OptionList(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (id, label) ->
            OptionCard(label, id in selected, onToggle = { onToggle(id) })
        }
    }
}

package com.taqvo.app.feature.feed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.content.FeedCard
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.drawBookmark
import com.taqvo.app.design.components.drawClose
import com.taqvo.app.design.components.drawCopy
import com.taqvo.app.design.components.drawDownload
import com.taqvo.app.design.components.drawEye
import com.taqvo.app.design.components.drawShare
import com.taqvo.app.design.components.drawTextStyle
import com.taqvo.app.feed.FeedTextStyle
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_share_close
import com.taqvo.app.resources.feed_share_collection
import com.taqvo.app.resources.feed_share_copy
import com.taqvo.app.resources.feed_share_save
import com.taqvo.app.resources.feed_share_send
import com.taqvo.app.resources.feed_share_style
import com.taqvo.app.resources.feed_share_watermark_hide
import com.taqvo.app.resources.feed_share_watermark_show
import com.taqvo.app.feed.FeedTheme

/** What the share sheet's six buttons do (feed spec §9). */
class ShareActions(
    val onSave: () -> Unit,
    val onToggleWatermark: () -> Unit,
    val onCollection: () -> Unit,
    val onCopy: () -> Unit,
    val onStyle: () -> Unit,
    val onShare: () -> Unit,
    val onDismiss: () -> Unit,
)

/**
 * The share sheet (feed spec §9): a close button, the 9:16 preview of exactly the picture that will
 * be saved or sent, and six buttons in two columns. [message] is the brief confirmation shown over
 * the preview ("Saved", "Copied"), cleared by [onMessageShown].
 */
@Composable
fun ShareSheet(
    card: FeedCard,
    format: AyahFormat,
    style: FeedTextStyle,
    theme: FeedTheme,
    watermark: Boolean,
    message: String?,
    onMessageShown: () -> Unit,
    actions: ShareActions,
) {
    val colors = LocalTaqvoColors.current
    TaqvoBottomSheet(onDismissRequest = actions.onDismiss, skipPartiallyExpanded = true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(colors.surface)
                    .clickable(onClickLabel = stringResource(Res.string.feed_share_close), onClick = actions.onDismiss)
                    .semantics { role = Role.Button },
                contentAlignment = Alignment.Center,
            ) { Canvas(Modifier.size(16.dp).clearAndSetSemantics {}) { drawClose(colors.textPrimary) } }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CardPreview(card, format, style, theme, watermark, Modifier.width(220.dp))
                if (message != null) {
                    Text(
                        message.orEmpty(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.textPrimary.copy(alpha = 0.85f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        color = colors.background,
                        style = TaqvoText.caption.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            LaunchedEffect(message) {
                if (message != null) {
                    delay(MESSAGE_MS)
                    onMessageShown()
                }
            }
            Spacer(Modifier.height(20.dp))
            val buttons: List<Triple<String, () -> Unit, DrawScope.(androidx.compose.ui.graphics.Color) -> Unit>> = listOf(
                Triple(stringResource(Res.string.feed_share_save), actions.onSave) { drawDownload(it) },
                Triple(
                    stringResource(if (watermark) Res.string.feed_share_watermark_hide else Res.string.feed_share_watermark_show),
                    actions.onToggleWatermark,
                ) { drawEye(it, struck = watermark) },
                Triple(stringResource(Res.string.feed_share_collection), actions.onCollection) { drawBookmark(it, filled = false) },
                Triple(stringResource(Res.string.feed_share_copy), actions.onCopy) { drawCopy(it) },
                Triple(stringResource(Res.string.feed_share_style), actions.onStyle) { drawTextStyle(it) },
                Triple(stringResource(Res.string.feed_share_send), actions.onShare) { drawShare(it) },
            )
            buttons.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (label, onClick, glyph) -> ShareButton(label, onClick, glyph, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun ShareButton(
    label: String,
    onClick: () -> Unit,
    glyph: DrawScope.(androidx.compose.ui.graphics.Color) -> Unit,
    modifier: Modifier,
) {
    val colors = LocalTaqvoColors.current
    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(20.dp).clearAndSetSemantics {}) { glyph(colors.textPrimary) }
        Spacer(Modifier.width(10.dp))
        Text(label, style = TaqvoText.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary)
    }
}

private const val MESSAGE_MS = 1_600L

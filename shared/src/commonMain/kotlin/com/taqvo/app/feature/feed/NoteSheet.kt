package com.taqvo.app.feature.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.content.FeedCard
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_note_chip
import com.taqvo.app.resources.feed_note_translator

/**
 * The translator's commentary on one ayah (feed spec §3.1): its reference, the commentary as
 * written, and whose it is. Opened from the Izoh chip; the commentary is never part of the card.
 */
@Composable
fun NoteSheet(card: FeedCard, onDismiss: () -> Unit) {
    val colors = LocalTaqvoColors.current
    TaqvoBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text(stringResource(Res.string.feed_note_chip), style = TaqvoText.screenTitle, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(4.dp))
            Text(card.reference, style = TaqvoText.caption, color = colors.textSecondary)
            Spacer(Modifier.height(16.dp))
            Text(card.note.orEmpty(), style = TaqvoText.rowLabel, color = colors.textPrimary)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.feed_note_translator), style = TaqvoText.caption, color = colors.textSecondary)
        }
    }
}

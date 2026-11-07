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
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.TaqvoCard
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.feed_chip_help
import com.taqvo.app.resources.feed_help_a1
import com.taqvo.app.resources.feed_help_a2
import com.taqvo.app.resources.feed_help_a3
import com.taqvo.app.resources.feed_help_a4
import com.taqvo.app.resources.feed_help_about
import com.taqvo.app.resources.feed_help_q1
import com.taqvo.app.resources.feed_help_q2
import com.taqvo.app.resources.feed_help_q3
import com.taqvo.app.resources.feed_help_q4

/**
 * The Yordam chip (feed spec §2, [DECIDE] settled by the owner's layout): how the feed works, in
 * four short answers, and the way to About for anything else.
 */
@Composable
fun HelpSheet(onAbout: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalTaqvoColors.current
    val pairs = listOf(
        Res.string.feed_help_q1 to Res.string.feed_help_a1,
        Res.string.feed_help_q2 to Res.string.feed_help_a2,
        Res.string.feed_help_q3 to Res.string.feed_help_a3,
        Res.string.feed_help_q4 to Res.string.feed_help_a4,
    )
    TaqvoBottomSheet(onDismissRequest = onDismiss, skipPartiallyExpanded = true) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 28.dp)) {
            Text(stringResource(Res.string.feed_chip_help), style = TaqvoText.screenTitle, modifier = Modifier.semantics { heading() })
            pairs.forEach { (q, a) ->
                Spacer(Modifier.height(18.dp))
                Text(stringResource(q), style = TaqvoText.rowLabel, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(4.dp))
                Text(stringResource(a), style = TaqvoText.caption, color = colors.textSecondary)
            }
            Spacer(Modifier.height(24.dp))
            TaqvoCard { TaqvoRow(label = stringResource(Res.string.feed_help_about), onClick = onAbout) }
        }
    }
}

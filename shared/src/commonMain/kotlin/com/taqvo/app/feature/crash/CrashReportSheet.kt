package com.taqvo.app.feature.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.components.TaqvoTextLink
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.crash_not_now
import com.taqvo.app.resources.crash_send
import com.taqvo.app.resources.crash_sheet_body
import com.taqvo.app.resources.crash_sheet_title

/**
 * Shown once per new crash report, on the launch after the crash (crash spec §3.1). Same chrome
 * as every other sheet; the primary pill sends, the text link is the only other way out besides
 * swiping it away, and both count as the report having been offered.
 */
@Composable
fun CrashReportSheet(onSend: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalTaqvoColors.current
    TaqvoBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                stringResource(Res.string.crash_sheet_title),
                style = TaqvoText.screenTitle.copy(fontSize = 20.sp),
                color = colors.textPrimary,
            )
            Text(
                stringResource(Res.string.crash_sheet_body),
                style = TaqvoText.caption.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = colors.textSecondary,
            )
            TaqvoPrimaryButton(
                stringResource(Res.string.crash_send),
                onClick = onSend,
                modifier = Modifier.padding(top = 6.dp),
            )
            TaqvoTextLink(stringResource(Res.string.crash_not_now), onClick = onDismiss)
        }
    }
}

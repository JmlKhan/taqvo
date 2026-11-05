package com.taqvo.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.about.AboutLinks
import com.taqvo.app.crash.ReportMail
import com.taqvo.app.crash.crashLogStore
import com.taqvo.app.crash.deviceInfoOrUnknown
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.di.AppContainer
import com.taqvo.app.feature.crash.CrashReportSheet
import com.taqvo.app.feature.recitation.DownloadSheet
import com.taqvo.app.feature.recitation.RecitationController
import com.taqvo.app.feature.recitation.RecitationState
import com.taqvo.app.feature.recitation.ReciterPicker
import com.taqvo.app.feature.recitation.rememberSurahName
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.crash_mail_no_report

@Composable
internal fun CrashReportSheetHost() {
    // The crash sheet (crash spec §3.1): read once per process, shown until answered.
    // Send and Not now both mark the report offered, so it never comes back for the
    // same crash; the report itself stays for the About screen's row.
    var crashOffer by remember { mutableStateOf(crashLogStore.pendingOffer()) }
    val uriHandler = LocalUriHandler.current
    val noReportLine = stringResource(Res.string.crash_mail_no_report)
    crashOffer?.let { report ->
        CrashReportSheet(
            onSend = {
                crashLogStore.markOffered(report)
                crashOffer = null
                val info = deviceInfoOrUnknown()
                val mail = ReportMail.mailto(
                    AboutLinks.SUPPORT_EMAIL,
                    ReportMail.subject(info),
                    ReportMail.body(info, report, noReportLine),
                )
                // A phone with no mail app fails silently rather than crashing again.
                runCatching { uriHandler.openUri(mail) }
            },
            onDismiss = {
                crashLogStore.markOffered(report)
                crashOffer = null
            },
        )
    }
}

@Composable
internal fun RecitationSheets(
    recitationStateState: State<RecitationState>,
    container: AppContainer,
    recitation: RecitationController,
) {
    val recitationState by recitationStateState
    // The two recitation sheets are hosted here, outside the scaffold, for the same
    // reason the bar is inside it: they can be opened from the reader's header, from
    // an ayah row, or from the player bar on any Quran screen, and a sheet owned by
    // one of those screens would go with it.
    val sheet = recitationState.sheet
    if (sheet != null) {
        val sheetSurahName = rememberSurahName(sheet.surah) { container.quranRepository.surah(it) }
        TaqvoBottomSheet(onDismissRequest = recitation::dismissSheet) {
            DownloadSheet(
                sheet = sheet,
                surahName = sheetSurahName,
                wholeQuran = recitationState.wholeQuran,
                onConfirm = recitation::confirmDownload,
                onWholeQuran = recitation::downloadWholeQuran,
                onCancel = recitation::cancelDownload,
                onRetry = recitation::retryDownload,
                onChangeReciter = recitation::openPicker,
            )
        }
    }
    if (recitationState.pickerOpen) {
        TaqvoBottomSheet(onDismissRequest = recitation::closePicker) {
            ReciterPicker(
                reciters = recitationState.reciters,
                currentId = recitationState.reciter?.id,
                downloadedCounts = recitationState.downloadedCounts,
                previewable = recitationState.previewable,
                onPick = recitation::pickReciter,
                onPreview = recitation::previewReciter,
            )
        }
    }
}

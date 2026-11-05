package com.taqvo.app.feature.adhkar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.RowToggle
import com.taqvo.app.design.components.TaqvoBottomSheet
import com.taqvo.app.design.components.TaqvoCard
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.feature.quran.ReadingSizeSlider
import com.taqvo.app.i18n.UiLanguage
import com.taqvo.app.i18n.isLatinScriptUi
import com.taqvo.app.i18n.uiLanguage
import com.taqvo.app.quran.ReadingSettings
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.adhkar_reading
import com.taqvo.app.resources.adhkar_translation
import com.taqvo.app.resources.quran_sheet_transliteration

/**
 * The adhkar's "Aa" sheet (spec §3.6), "Reading": the Arabic size, and the two switches the pages
 * read — built from the reader's [com.taqvo.app.feature.quran.ReadingSheet] parts, not the reader's
 * sheet itself. There is no translation picker, no reading mode and no voice row here: the Quran
 * pages take the reader's translation and the reciter chosen in Settings, and a du'a has no audio.
 *
 * The size and the transliteration are the reader's own keys ([settings], written whole through
 * [onChange]) — one knob for the whole app; the translation is the adhkar's own switch
 * ([showTranslation], [onTranslation]). [preview] is the du'a fragment the size is shown on
 * ([AdhkarUiState.readingPreview]).
 */
@Composable
fun AdhkarReadingSheet(
    settings: ReadingSettings,
    showTranslation: Boolean,
    preview: String,
    onChange: (ReadingSettings) -> Unit,
    onTranslation: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    TaqvoBottomSheet(onDismissRequest = onDismiss) {
        AdhkarReadingContent(settings, showTranslation, preview, onChange, onTranslation)
    }
}

/**
 * The sheet's contents, apart from its chrome so that a UI test can compose them.
 *
 * - The size row and slider are the reader's own [ReadingSizeSlider], always enabled here: the
 *   Mushaf's fixed-size note has no place in the adhkar. As in the reader, the drag moves only the
 *   preview and the number; the size is written once, when the drag ends.
 * - The preview is the du'a face at the live size, drawn by [DuaArabic] exactly as the page draws
 *   it. It is a picture of the size, not words to read, so a screen reader skips it — as the
 *   header's "Aa" letters are skipped in favour of the button's name.
 * - Transliteration only under a Latin interface, where the pages show it; the translation switch
 *   under every interface but Arabic, where the pages never show a translation (spec §3.2). Under
 *   the Arabic interface the sheet is the size alone.
 */
@Composable
internal fun AdhkarReadingContent(
    settings: ReadingSettings,
    showTranslation: Boolean,
    preview: String,
    onChange: (ReadingSettings) -> Unit,
    onTranslation: (Boolean) -> Unit,
) {
    val colors = LocalTaqvoColors.current
    val transliterationRow = isLatinScriptUi()
    val translationRow = uiLanguage() != UiLanguage.ARABIC

    // The size the preview is drawn at: the slider's while it is dragged, the stored one otherwise
    // — re-keyed to it as the slider's own value is.
    var liveSize by remember(settings.arabicSizeSp) { mutableIntStateOf(settings.arabicSizeSp) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(Res.string.adhkar_reading),
            style = TaqvoText.screenTitle.copy(fontSize = 20.sp),
            color = colors.textPrimary,
        )

        ReadingSizeSlider(
            settings = settings,
            enabled = true,
            onLiveChange = { liveSize = it },
            onChange = onChange,
        )

        if (preview.isNotEmpty()) {
            Column {
                CardDivider()
                Column(Modifier.padding(vertical = 10.dp)) {
                    DuaArabic(preview, liveSize, voiced = false, textAlign = TextAlign.Center)
                }
                CardDivider()
            }
        }

        if (transliterationRow || translationRow) {
            TaqvoCard {
                if (transliterationRow) {
                    TaqvoRow(
                        label = stringResource(Res.string.quran_sheet_transliteration),
                        toggle = RowToggle(settings.transliteration) { onChange(settings.copy(transliteration = it)) },
                    )
                }
                if (transliterationRow && translationRow) CardDivider()
                if (translationRow) {
                    TaqvoRow(
                        label = stringResource(Res.string.adhkar_translation),
                        toggle = RowToggle(showTranslation, onTranslation),
                    )
                }
            }
        }
    }
}

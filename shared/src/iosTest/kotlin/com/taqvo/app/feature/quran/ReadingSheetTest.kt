package com.taqvo.app.feature.quran

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import org.jetbrains.compose.resources.getString
import com.taqvo.app.design.LightColors
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.i18n.EnglishPlatformFormat
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.quran.ReadingMode
import com.taqvo.app.quran.ReadingSettings
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.quran_size_mushaf_note
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The reader's sheet around its size slider, which the adhkar's sheet shares
 * ([ReadingSizeSlider]): a commit writes the size and keeps every other setting, under the Mushaf
 * the slider is still and the note says why, and the Arabic preview is drawn but never spoken.
 * Words come from the resources; digits from the English format the scene is given.
 */
@OptIn(ExperimentalTestApi::class)
class ReadingSheetTest {

    private val settings = ReadingSettings(
        mode = ReadingMode.TRANSLATION,
        arabicSizeSp = 28,
        transliteration = true,
        translationId = "fr.hamidullah",
    )

    private val slider = SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)

    /** Not TaqvoTheme: its system-bar and keep-screen-on effects crash the simulator test process. */
    @Composable
    private fun Sheet(mushafMode: Boolean, changes: MutableList<ReadingSettings>) {
        CompositionLocalProvider(
            LocalTaqvoColors provides LightColors,
            LocalPlatformFormat provides EnglishPlatformFormat,
        ) {
            ReadingSheet(
                settings = settings,
                translations = emptyList(),
                previewAyah = "بِسْمِ ٱللَّهِ",
                mushafMode = mushafMode,
                onChange = { changes += it },
                onDismiss = {},
            )
        }
    }

    @Test
    fun theSizeCommitsOnceWithEveryOtherSettingKept() = runComposeUiTest {
        val changes = mutableListOf<ReadingSettings>()
        setContent { Sheet(mushafMode = false, changes = changes) }
        onNodeWithText("28").assertIsDisplayed()
        onNode(slider).assertIsEnabled().performSemanticsAction(SemanticsActions.SetProgress) { it(32f) }
        assertEquals(listOf(settings.copy(arabicSizeSp = 32)), changes)
        // The row's number follows the slider before the store does.
        onNodeWithText("32").assertIsDisplayed()
    }

    /** The size preview is Quran text, and the Quran is for the reciters alone (design §5). */
    @Test
    fun thePreviewAyahIsNotGivenToAScreenReader() = runComposeUiTest {
        setContent { Sheet(mushafMode = false, changes = mutableListOf()) }
        onAllNodesWithText("ٱللَّهِ", substring = true).assertCountEquals(0)
    }

    @Test
    fun underTheMushafTheSliderIsStillAndSaysWhy() = runComposeUiTest {
        setContent { Sheet(mushafMode = true, changes = mutableListOf()) }
        onNode(slider).assertIsNotEnabled()
        onNodeWithText(getString(Res.string.quran_size_mushaf_note)).assertIsDisplayed()
    }
}

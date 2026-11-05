package com.taqvo.app.design

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TypeTest {
    @Test
    fun countdownUsesLightWeightForTheBigNumeral() {
        assertEquals(FontWeight.Light, TaqvoText.Latin.countdown.fontWeight)
    }

    @Test
    fun rowLabelAndRowTimeShareASize() {
        assertEquals(TaqvoText.Latin.rowLabel.fontSize, TaqvoText.Latin.rowTime.fontSize)
    }

    /**
     * Not a style preference. Android does not support letter spacing for complex scripts, and
     * leaving the spec's Latin tracking on an Arabic run makes Compose measure it several times
     * too wide — which wrapped "تقوى" in the middle of the word until this rule existed.
     */
    @Test
    fun arabicDropsTheLatinTracking() {
        listOf(TaqvoText.Latin.countdown, TaqvoText.Latin.screenTitle, TaqvoText.Latin.sectionLabel)
            .forEach { tracked ->
                assertNotEquals(TextUnit.Unspecified, tracked.letterSpacing)
                assertEquals(
                    TextUnit.Unspecified,
                    TaqvoText.forScript(tracked, arabic = true).letterSpacing,
                )
            }
    }

    @Test
    fun latinKeepsEveryOtherPropertyExactlyAsTheSpecSetsIt() {
        val title = TaqvoText.Latin.screenTitle
        assertEquals(title, TaqvoText.forScript(title, arabic = false))
        assertEquals(title.fontSize, TaqvoText.forScript(title, arabic = true).fontSize)
        assertEquals(title.fontWeight, TaqvoText.forScript(title, arabic = true).fontWeight)
    }
}

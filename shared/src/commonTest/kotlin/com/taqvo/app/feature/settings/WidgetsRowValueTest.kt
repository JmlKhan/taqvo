package com.taqvo.app.feature.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import com.taqvo.app.widget.WidgetPlacement
import com.taqvo.app.widget.WidgetPlacementSource
import com.taqvo.app.widget.reread

/**
 * The value beside Settings › Widgets, decided as the pure function it is.
 *
 * The cases that matter most are the first two: before placement has been read, and when the
 * platform cannot tell, the row says nothing at all, so it never shows "Not added" for a frame on a
 * phone that has every widget, nor "All added" on one that has neither.
 */
class WidgetsRowValueTest {

    @Test
    fun `placement not read yet shows no value`() {
        assertEquals(WidgetsRowValue.Hidden, widgetsRowValue(null))
    }

    @Test
    fun `all three widgets placed read as all added`() {
        assertEquals(WidgetsRowValue.AllAdded, widgetsRowValue(WidgetPlacement(prayer = true, ayah = true, dua = true)))
    }

    @Test
    fun `the prayer and ayah widgets without the dua widget count two of three`() {
        assertEquals(WidgetsRowValue.Some(2, 3), widgetsRowValue(WidgetPlacement(prayer = true, ayah = true)))
    }

    @Test
    fun `only the dua widget placed counts one of three`() {
        assertEquals(WidgetsRowValue.Some(1, 3), widgetsRowValue(WidgetPlacement(prayer = false, ayah = false, dua = true)))
    }

    @Test
    fun `placement the source cannot tell shows no value`() = runTest {
        val cannotTell = object : WidgetPlacementSource {
            override suspend fun current(): WidgetPlacement? = null
        }
        assertEquals(WidgetsRowValue.Hidden, widgetsRowValue(cannotTell.reread(shown = null)))
    }

    @Test
    fun `a Lock Screen widget alone counts nothing on the home screen`() {
        assertEquals(
            WidgetsRowValue.NoneAdded,
            widgetsRowValue(WidgetPlacement(prayer = false, ayah = false, lockScreen = true)),
        )
    }

    @Test
    fun `a Lock Screen widget beside the ayah widget counts one of three`() {
        assertEquals(
            WidgetsRowValue.Some(1, 3),
            widgetsRowValue(WidgetPlacement(prayer = false, ayah = true, lockScreen = true)),
        )
    }

    @Test
    fun `neither widget placed reads as not added`() {
        assertEquals(WidgetsRowValue.NoneAdded, widgetsRowValue(WidgetPlacement(prayer = false, ayah = false)))
    }

    @Test
    fun `only the prayer widget placed counts one of three`() {
        assertEquals(WidgetsRowValue.Some(1, 3), widgetsRowValue(WidgetPlacement(prayer = true, ayah = false)))
    }

    @Test
    fun `only the ayah widget placed counts one of three`() {
        assertEquals(WidgetsRowValue.Some(1, 3), widgetsRowValue(WidgetPlacement(prayer = false, ayah = true)))
    }
}

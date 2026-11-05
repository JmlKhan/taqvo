package com.taqvo.app.feature.qibla

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import org.jetbrains.compose.resources.getString
import com.taqvo.app.design.LightColors
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.i18n.EnglishPlatformFormat
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.qibla.CompassLowReason
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.qibla_bearing
import com.taqvo.app.resources.qibla_distance
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The Qibla screen in the states the compass cannot be trusted in. Words come from the resources,
 * in whatever language the simulator runs; digits from the English format the scene is given.
 */
@OptIn(ExperimentalTestApi::class)
class QiblaScreenTest {

    /** Not TaqvoTheme: its system-bar and keep-screen-on effects crash the simulator test process. */
    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalTaqvoColors provides LightColors,
            LocalPlatformFormat provides EnglishPlatformFormat,
            content = content,
        )
    }

    /** "Use a physical compass together with the bearing below", in all seven languages. */
    @Test
    fun theNoSensorScreenShowsTheBearingAndDistanceItSendsTheReaderTo() = runComposeUiTest {
        setContent { Themed { QiblaScreen(QiblaUiState.NoSensor(119.4, 4_790.4), onBack = {}) } }
        onNodeWithText(getString(Res.string.qibla_bearing, "119")).assertIsDisplayed()
        onNodeWithText(
            getString(Res.string.qibla_distance, localizedGroupedKm(4_790.4, EnglishPlatformFormat)),
        ).assertIsDisplayed()
    }

    /**
     * Low accuracy has no heading the app trusts, so nothing on screen may point anywhere (spec
     * §4.3). The screen shows no readout, no marker and no cardinals, and its dial sits at heading
     * 0, so the bearing could only move a needle: two bearings half a turn apart must draw the
     * same pixels.
     */
    @Test
    fun theLowAccuracyScreenDrawsNothingThatTurnsWithTheBearing() = runComposeUiTest {
        var state by mutableStateOf<QiblaUiState>(
            QiblaUiState.LowAccuracy(30.0, 4_790.4, CompassLowReason.CALIBRATION),
        )
        setContent { Themed { QiblaScreen(state, onBack = {}) } }
        val one = onRoot().captureToImage().toPixelMap()
        state = QiblaUiState.LowAccuracy(210.0, 4_790.4, CompassLowReason.CALIBRATION)
        waitForIdle()
        val other = onRoot().captureToImage().toPixelMap()
        assertEquals(
            0,
            one.buffer.indices.count { one.buffer[it] != other.buffer[it] },
            "pixels that move with the bearing",
        )
    }
}

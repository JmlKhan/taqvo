package com.taqvo.app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import org.jetbrains.compose.resources.getString
import com.taqvo.app.design.LightColors
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.domain.NotificationSettings
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.notifications_adhkar
import com.taqvo.app.resources.notifications_adhkar_subtitle
import com.taqvo.app.resources.notifications_tahajjud_sound
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Settings › Notifications, the adhkar card (adhkar spec §5), built as the Duha and Iqama cards
 * are: off, one quiet row, its title and its switch; on, its subtitle and its sound row appear.
 * Expected words come from the resources: the simulator's language decides them.
 */
@OptIn(ExperimentalTestApi::class)
class AdhkarReminderCardTest {

    @Composable
    private fun Screen(settings: NotificationSettings, onToggleAdhkar: (Boolean) -> Unit = {}) {
        // Not TaqvoTheme: its system-bar and keep-screen-on effects crash the simulator test
        // process. The colours are all the screen reads from it.
        CompositionLocalProvider(LocalTaqvoColors provides LightColors) {
            NotificationSettingsScreen(
                settings = settings,
                onBack = {},
                onToggleEnabled = {},
                onPickLead = {},
                onPickSound = { _, _ -> },
                onPreviewSound = {},
                onToggleAdhkar = onToggleAdhkar,
            )
        }
    }

    @Test
    fun offTheAdhkarCardIsOnlyItsRow() = runComposeUiTest {
        val toggled = mutableListOf<Boolean>()
        setContent { Screen(NotificationSettings(), onToggleAdhkar = { toggled += it }) }
        // The row is the switch, found by its name and activated as a screen reader's double-tap
        // does: only the drawn switch at its end takes a touch.
        val adhkar = onNode(isToggleable() and hasText(getString(Res.string.notifications_adhkar)))
        adhkar.performScrollTo().assertIsDisplayed().assertIsOff()
        onAllNodesWithText(getString(Res.string.notifications_adhkar_subtitle)).assertCountEquals(0)
        // Off on a fresh install, Tahajjud and Duha too: no sound row anywhere.
        onAllNodesWithText(getString(Res.string.notifications_tahajjud_sound)).assertCountEquals(0)

        adhkar.performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(true), toggled)
    }

    @Test
    fun onTheAdhkarCardShowsItsSubtitleAndSound() = runComposeUiTest {
        var settings by mutableStateOf(NotificationSettings())
        setContent { Screen(settings) }
        val subtitle = getString(Res.string.notifications_adhkar_subtitle)
        onAllNodesWithText(subtitle).assertCountEquals(0)

        settings = settings.copy(adhkar = true)
        waitForIdle()
        onNode(isToggleable() and hasText(getString(Res.string.notifications_adhkar))).performScrollTo().assertIsOn()
        onNodeWithText(subtitle).performScrollTo().assertIsDisplayed()
        onNodeWithText(getString(Res.string.notifications_tahajjud_sound)).performScrollTo().assertIsDisplayed()
    }
}

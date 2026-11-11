package com.taqvo.app.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import org.jetbrains.compose.resources.getString
import com.taqvo.app.design.LightColors
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.ThemeMode
import com.taqvo.app.domain.NotificationSettings
import com.taqvo.app.i18n.AppLanguage
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.appearance_language_label
import com.taqvo.app.resources.language_name
import com.taqvo.app.resources.settings_appearance_value
import com.taqvo.app.resources.settings_language
import com.taqvo.app.resources.theme_system
import kotlin.test.Test

/**
 * The language is set once, if ever (Tazkir follows the phone's), so it lives inside Appearance
 * with the theme, and the root's Appearance row names both: a reader who picked a language they
 * cannot read still finds their own language's name on the first screen.
 */
@OptIn(ExperimentalTestApi::class)
class LanguageInAppearanceTest {

    /** Not TaqvoTheme: its system-bar effects crash the simulator test process. */
    @Composable
    private fun Colours(content: @Composable () -> Unit) =
        CompositionLocalProvider(LocalTaqvoColors provides LightColors, content = content)

    @Composable
    private fun Root(themeName: String) = Colours {
        SettingsRootScreen(
            cityName = "Cape Town",
            methodName = "Cautious",
            themeName = themeName,
            notificationSettings = NotificationSettings(),
            onOpenLocation = {},
            onOpenPrayerTimes = {},
            onOpenNotifications = {},
            onOpenAppearance = {},
            widgetsValue = null,
            onOpenWidgets = {},
            onOpenWallpapers = {},
            onOpenAbout = {},
            reciterName = "Alafasy",
            onOpenRecitation = {},
        )
    }

    @Test
    fun theRootHasNoLanguageRowAndItsAppearanceRowNamesTheThemeAndTheLanguage() = runComposeUiTest {
        val system = getString(Res.string.theme_system)
        setContent { Root(themeName = system) }
        waitForIdle()
        onAllNodesWithText(getString(Res.string.settings_language)).assertCountEquals(0)
        val value = getString(Res.string.settings_appearance_value, system, getString(Res.string.language_name))
        onNodeWithText(value).assertIsDisplayed()
    }

    @Test
    fun appearanceHoldsTheLanguageUnderTheTheme() = runComposeUiTest {
        setContent { Colours { AppearanceSettingsScreen(current = ThemeMode.SYSTEM, onPick = {}, onBack = {}) } }
        waitForIdle()
        onNodeWithText(getString(Res.string.appearance_language_label)).assertIsDisplayed()
        onNodeWithText(AppLanguage.DEFAULT.endonym).assertIsDisplayed()
    }
}

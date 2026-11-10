package com.taqvo.app.nav

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `openReading` is how anything outside the Quran — the ayah widget, the media notification —
 * lands the reader on a reading screen. The rule that matters: the Quran root is the nearest tab
 * root under the reader, because [Navigator.currentTab] is that root and the player bar is shown
 * only while it is the Quran. The feed is home and always at the bottom, so Back ends there.
 */
class OpenReadingTest {

    @Test
    fun aWidgetTapFromThePrayerTabPutsTheQuranRootUnderTheReader() {
        val n = Navigator(Screen.Prayer)

        n.openReading(Screen.Reader(2, 255, selectAyah = true))

        assertEquals(listOf(Screen.Feed, Screen.Quran, Screen.Reader(2, 255, selectAyah = true)), n.backStack.value)
        assertEquals(Tab.QURAN, n.currentTab)
    }

    @Test
    fun fromASettingsSubScreenTheWholeStackIsReplaced() {
        val n = Navigator(Screen.Prayer)
        n.selectTab(Tab.SETTINGS)
        n.push(Screen.Appearance)

        n.openReading(Screen.Mushaf(42, 2, 255))

        assertEquals(listOf(Screen.Feed, Screen.Quran, Screen.Mushaf(42, 2, 255)), n.backStack.value)
    }

    @Test
    fun fromTheQuranRootOnlyTheReaderIsPushed() {
        val n = Navigator(Screen.Quran)

        n.openReading(Screen.Reader(36, 1))

        assertEquals(listOf(Screen.Quran, Screen.Reader(36, 1)), n.backStack.value)
    }

    @Test
    fun aReadingScreenAlreadyOpenIsReplacedNotStacked() {
        val n = Navigator(Screen.Quran)
        n.push(Screen.Reader(2, 1))

        n.openReading(Screen.Reader(2, 255, selectAyah = true))

        assertEquals(listOf(Screen.Quran, Screen.Reader(2, 255, selectAyah = true)), n.backStack.value)
    }

    @Test
    fun aReaderOpenedFromIlovalarIsReplacedInPlace() {
        // The Quran opened over the feed's Ilovalar screen: the Quran root is the current tab, so a
        // second tap replaces the reader and Back still walks to the surah list, Ilovalar, home.
        val n = Navigator(Screen.Feed)
        n.push(Screen.Tools)
        n.push(Screen.Quran)
        n.push(Screen.Reader(2, 1))

        n.openReading(Screen.Reader(3, 7, selectAyah = true))

        assertEquals(listOf(Screen.Feed, Screen.Tools, Screen.Quran, Screen.Reader(3, 7, selectAyah = true)), n.backStack.value)
        assertEquals(Tab.QURAN, n.currentTab)
    }

    @Test
    fun fromAQuranSubScreenTheRootIsPushedAgainSoBackReturnsToIt() {
        // Existing behaviour of the notification path, kept: a reader opened over the Quran
        // tab's own sub-screens gets the root under it, and one Back reaches the surah list.
        val n = Navigator(Screen.Quran)
        n.push(Screen.RecitationSettings)

        n.openReading(Screen.Reader(1, 1))

        assertEquals(listOf(Screen.Quran, Screen.RecitationSettings, Screen.Quran, Screen.Reader(1, 1)), n.backStack.value)
    }
}

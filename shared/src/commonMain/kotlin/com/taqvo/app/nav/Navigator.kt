package com.taqvo.app.nav

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A list in a StateFlow is the whole requirement: a couple of tab roots and a handful of pushed
 * children. The tab bar arrived early, on the owner's call after seeing Today on a phone, but it
 * brings no per-tab history with it — [selectTab] replaces the stack rather than juggling three.
 */
class Navigator(start: Screen) {

    private val _backStack = MutableStateFlow(listOf(start))
    val backStack: StateFlow<List<Screen>> = _backStack.asStateFlow()

    val current: Screen get() = _backStack.value.last()

    fun push(screen: Screen) {
        _backStack.value = _backStack.value + screen
    }

    /** Returns false at the root so the caller can let the platform handle the back gesture. */
    fun pop(): Boolean {
        if (_backStack.value.size <= 1) return false
        _backStack.value = _backStack.value.dropLast(1)
        return true
    }

    fun replaceAll(screen: Screen) {
        _backStack.value = listOf(screen)
    }

    /**
     * Pop then push: [screen] takes the current screen's place, with whatever was behind it
     * untouched. Used by the reader's mode toggle (spec §2.2) — translation and Mushaf must not
     * stack on top of each other, or the back button would bounce between the two modes of the
     * same reading position instead of leaving the surah.
     */
    fun replace(screen: Screen) {
        _backStack.value = _backStack.value.dropLast(1) + screen
    }

    /**
     * The part of the app showing: the nearest tab root at or below the top of the stack, so a
     * pushed sub-screen still reports the part it was opened from. The feed (always at the bottom
     * once onboarding is done) is the answer when nothing else is open. Null during onboarding.
     */
    val currentTab: Tab? get() = _backStack.value.lastOrNull { tabOf(it) != null }?.let(::tabOf)

    /**
     * Opens [tab]'s root over the feed (the owner's structure: the feed is home, everything else
     * opens from it and goes back to it), with nothing else between: a widget or notification
     * request lands on its screen with Back leading home. [Tab.FEED] is home alone.
     */
    fun selectTab(tab: Tab) {
        _backStack.value = if (tab == Tab.FEED) listOf(Screen.Feed) else listOf(Screen.Feed, tab.root)
    }
}

package com.taqvo.app.feature.onboarding

import com.taqvo.app.location.LocationPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Onboarding's order (spec §1) and where its location screen goes (App Review 5.1.1(iv)). */
class OnboardingFlowTest {

    private val plain = OnboardingContext(paywallEnabled = false)

    @Test
    fun `allowing location moves on to the next screen`() {
        assertEquals(LocationAnswer.CONTINUE, locationAnswer(LocationPermission.GRANTED))
    }

    @Test
    fun `declining location opens the city search rather than moving on without one`() {
        assertEquals(LocationAnswer.CHOOSE_CITY, locationAnswer(LocationPermission.DENIED))
    }

    @Test
    fun `a prompt still unanswered leaves the screen where it is`() {
        assertEquals(LocationAnswer.STAY, locationAnswer(LocationPermission.NOT_REQUESTED))
    }

    @Test
    fun `the plain flow runs from the splash to the place with no optional screen`() {
        val steps = OnboardingFlow.steps(plain)
        assertEquals(OnboardingStep.SPLASH, steps.first())
        assertEquals(OnboardingStep.LOCATION, steps.last())
        assertFalse(OnboardingStep.EXACT_ALARMS in steps)
        assertFalse(OnboardingStep.PAYWALL in steps)
        assertFalse(OnboardingStep.WIDGET_GUIDE in steps)
        assertNull(OnboardingFlow.next(OnboardingStep.LOCATION, plain))
    }

    @Test
    fun `asking for widgets ends on the widget guide`() {
        val context = plain.copy(wantsWidgets = true)
        assertEquals(OnboardingStep.WIDGET_GUIDE, OnboardingFlow.next(OnboardingStep.LOCATION, context))
        assertNull(OnboardingFlow.next(OnboardingStep.WIDGET_GUIDE, context))
    }

    @Test
    fun `exact alarms are asked only on an Android that needs them and once notifications are allowed`() {
        val needs = plain.copy(exactAlarmsNeeded = true)
        assertEquals(OnboardingStep.LOADING, OnboardingFlow.next(OnboardingStep.REMINDERS, needs))
        assertEquals(OnboardingStep.EXACT_ALARMS, OnboardingFlow.next(OnboardingStep.REMINDERS, needs.copy(notificationsGranted = true)))
        assertEquals(OnboardingStep.LOADING, OnboardingFlow.next(OnboardingStep.REMINDERS, plain.copy(notificationsGranted = true)))
    }

    @Test
    fun `the paywall stays out while it is switched off`() {
        assertFalse(OnboardingFeatures.PAYWALL_ENABLED)
        assertEquals(OnboardingStep.WELCOME, OnboardingFlow.next(OnboardingStep.SHOWCASE_HOME, OnboardingContext()))
        assertEquals(OnboardingStep.PAYWALL, OnboardingFlow.next(OnboardingStep.SHOWCASE_HOME, plain.copy(paywallEnabled = true)))
    }

    @Test
    fun `only the questions can be skipped`() {
        val skippable = OnboardingStep.entries.filter(OnboardingFlow::skippable)
        assertEquals(listOf(OnboardingStep.NAME, OnboardingStep.GOALS, OnboardingStep.CONTENT, OnboardingStep.USE_CASES), skippable)
    }

    @Test
    fun `the progress bar fills step by step and is hidden on the splash the paywall and the welcome`() {
        assertNull(OnboardingFlow.progress(OnboardingStep.SPLASH, plain))
        assertNull(OnboardingFlow.progress(OnboardingStep.WELCOME, plain))
        assertNull(OnboardingFlow.progress(OnboardingStep.PAYWALL, plain.copy(paywallEnabled = true)))
        val shown = OnboardingFlow.steps(plain).mapNotNull { OnboardingFlow.progress(it, plain) }
        assertEquals(shown.sorted(), shown)
        assertEquals(1f, shown.last())
        assertTrue(shown.first() > 0f)
    }

    @Test
    fun `a stored step comes back and anything unknown starts over`() {
        assertEquals(OnboardingStep.THEME, OnboardingFlow.stepOf("THEME"))
        assertEquals(OnboardingStep.SPLASH, OnboardingFlow.stepOf("WELCOME_OLD"))
        assertEquals(OnboardingStep.SPLASH, OnboardingFlow.stepOf(null))
    }

    @Test
    fun `a name is trimmed and cut and nothing is no name`() {
        assertEquals("Jamol", cleanName("  Jamol "))
        assertNull(cleanName("   "))
        assertEquals(NAME_MAX_LENGTH, cleanName("x".repeat(50))!!.length)
    }

    @Test
    fun `the yearly discount is computed from the prices`() {
        // 19.99 a year against 6.49 a month: the competitor's 74 %.
        assertEquals(74, discountPercent(19_990_000, 6_490_000))
        assertEquals(0, discountPercent(100_000_000, 1_000_000))
        assertEquals(0, discountPercent(1, 0))
    }
}

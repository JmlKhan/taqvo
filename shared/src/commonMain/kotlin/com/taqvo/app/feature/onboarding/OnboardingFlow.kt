package com.taqvo.app.feature.onboarding

import com.taqvo.app.location.LocationPermission

/**
 * Onboarding's screens, in order (onboarding spec §1). Hoisted out of the screen because a
 * declined location navigates away to the city search: the step must survive the round trip.
 * Stored by name to resume a killed onboarding, so a rename needs a migration; appending is fine.
 */
enum class OnboardingStep {
    SPLASH,
    /** The reader picks the app's language; Uzbek Cyrillic is preselected. */
    LANGUAGE,
    INTRO,
    NAME,
    GOALS,
    CONTENT,
    JOURNEY,
    STREAK,
    USE_CASES,
    REMINDERS,
    /** Android 14+ without "Alarms & reminders": asked right after notifications are allowed. */
    EXACT_ALARMS,
    LOADING,
    THEME,
    PREVIEW,
    SHOWCASE_LOCK,
    SHOWCASE_HOME,
    WELCOME,
    AYAH_FORMAT,
    LOCATION,
    WIDGET_GUIDE,
}

/** What decides which optional screens a reader sees. */
data class OnboardingContext(
    /** True on an Android that has not granted "Alarms & reminders"; always false on iOS. */
    val exactAlarmsNeeded: Boolean = false,
    /** The answer the notification prompt gave on screen 09; false until it has been asked. */
    val notificationsGranted: Boolean = false,
    val wantsWidgets: Boolean = false,
)

object OnboardingFlow {

    /** The screens this reader will see, in order. */
    fun steps(context: OnboardingContext): List<OnboardingStep> = OnboardingStep.entries.filter { step ->
        when (step) {
            OnboardingStep.EXACT_ALARMS -> context.exactAlarmsNeeded && context.notificationsGranted
            // The guide follows the reader's own pick (spec, screen 08); Settings › Widgets keeps it.
            OnboardingStep.WIDGET_GUIDE -> context.wantsWidgets
            // Always asked, unlike the spec's screen 17b: the app opens on the Prayer screen, which
            // has nothing to show without a place.
            else -> true
        }
    }

    /** The screen after [step], or null when onboarding is done. */
    fun next(step: OnboardingStep, context: OnboardingContext): OnboardingStep? =
        OnboardingStep.entries.drop(step.ordinal + 1).firstOrNull { it in steps(context) }

    /** The questions that can be skipped (spec §0): their answer is stored as none. */
    fun skippable(step: OnboardingStep): Boolean = step in SKIPPABLE

    /** How full the progress bar is on [step], or null where the spec hides it. */
    fun progress(step: OnboardingStep, context: OnboardingContext): Float? {
        if (step in NO_PROGRESS) return null
        val shown = steps(context).filterNot { it in NO_PROGRESS }
        val index = shown.indexOf(step).takeIf { it >= 0 } ?: return null
        return (index + 1f) / shown.size
    }

    private val SKIPPABLE = setOf(
        OnboardingStep.NAME,
        OnboardingStep.GOALS,
        OnboardingStep.CONTENT,
        OnboardingStep.USE_CASES,
    )

    /** The splash and the full-screen welcome have no progress bar (spec §0). The app is free: no paywall. */
    private val NO_PROGRESS = setOf(OnboardingStep.SPLASH, OnboardingStep.WELCOME)

    /** A stored step's name back to the step; anything unknown starts over. */
    fun stepOf(name: String?): OnboardingStep =
        OnboardingStep.entries.firstOrNull { it.name == name } ?: OnboardingStep.SPLASH
}

/** What onboarding's location screen does once the system prompt has been answered. */
enum class LocationAnswer {
    /** Allowed: the phone's location is used and onboarding moves on. */
    CONTINUE,

    /**
     * Declined: the city search opens straight away. App Review asks that the alternative come
     * after the system prompt rather than beside it (guideline 5.1.1(iv)), and a prayer app with
     * no place has nothing to show, so a city is what a refusal leads to, not an empty Prayer
     * screen.
     */
    CHOOSE_CITY,

    /**
     * Not answered yet: iOS stops waiting after a minute while the prompt may still be up. Nothing
     * moves, and Continue asks again.
     */
    STAY,
}

fun locationAnswer(permission: LocationPermission): LocationAnswer = when (permission) {
    LocationPermission.GRANTED -> LocationAnswer.CONTINUE
    LocationPermission.DENIED -> LocationAnswer.CHOOSE_CITY
    LocationPermission.NOT_REQUESTED -> LocationAnswer.STAY
}

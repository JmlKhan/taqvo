package com.taqvo.app.analytics

/**
 * Where the onboarding funnel's events go (onboarding spec §4). The app calls only this; the
 * provider behind it is chosen in [com.taqvo.app.di.AppContainer].
 *
 * Until a provider is chosen the app uses [NoAnalytics] and nothing leaves the phone, which is
 * what PRIVACY.md promises today. Plugging a real provider in means rewriting PRIVACY.md (all
 * languages), the site's privacy page and both stores' privacy labels first.
 *
 * Properties carry option ids, counts and flags only: never the reader's name or anything they
 * typed (spec §0).
 */
interface Analytics {
    fun track(event: String, properties: Map<String, Any?> = emptyMap())
}

/** Sends nothing anywhere. */
object NoAnalytics : Analytics {
    override fun track(event: String, properties: Map<String, Any?>) = Unit
}

/** The onboarding events by name (spec §2 and §4), so a typo cannot split a funnel step. */
object OnboardingEvents {
    const val STEP_VIEW = "onb_step_view"
    const val SPLASH_VIEW = "onb_splash_view"
    const val SPLASH_CTA = "onb_splash_cta"
    const val ANSWER = "onb_answer"
    const val SKIP = "onb_skip"
    const val NOTIF_CONFIG = "onb_notif_config"
    const val NOTIF_PERMISSION = "onb_notif_permission"
    const val LOCATION = "onb_location"
    const val THEME = "onb_theme"
    const val AYAH_FORMAT = "onb_ayah_format"
    const val COMPLETE = "onb_complete"
}

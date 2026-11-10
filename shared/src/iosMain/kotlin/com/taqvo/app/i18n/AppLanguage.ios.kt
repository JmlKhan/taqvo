package com.taqvo.app.i18n

import platform.Foundation.NSUserDefaults

/** The language the formatter below writes in, set by [applyAppLanguage]; null until the app has chosen one. */
internal var appLanguageTag: String? = null
    private set

internal actual fun applyAppLanguage(tag: String) {
    appLanguageTag = tag
    // The system's own list too, so what iOS draws itself (the share sheet, permission prompts)
    // is in this language from the next launch.
    NSUserDefaults.standardUserDefaults.setObject(listOf(tag), forKey = "AppleLanguages")
}

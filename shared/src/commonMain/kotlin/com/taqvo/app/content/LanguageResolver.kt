package com.taqvo.app.content

import com.taqvo.app.i18n.AppLanguage

/**
 * The app's language to the feed's translation language (content spec §2). The feed follows the
 * reader's choice in the app, never the phone's locale. content.db carries Sodiq's translation in
 * Uzbek only, Cyrillic and Latin, so those two languages get their own script and every other app
 * language reads the Cyrillic text, [ContentLang.FALLBACK].
 */
object LanguageResolver {

    fun forApp(language: AppLanguage): ContentLang = when (language) {
        AppLanguage.UZBEK_CYRILLIC -> ContentLang.UZ_CYRL
        AppLanguage.UZBEK_LATIN -> ContentLang.UZ_LATN
        else -> ContentLang.FALLBACK
    }
}

package com.taqvo.app.content

/**
 * The phone's locale to the feed's translation language (content spec §2): an exact match first,
 * then `uz` with no script as Latin (the script Uzbekistan writes officially), and English for
 * every other language.
 *
 * Read from the locale tag, not the interface language: the app has no Russian interface, and a
 * Russian phone still reads the feed in Russian.
 */
object LanguageResolver {

    fun resolve(localeTag: String): ContentLang {
        // Android writes "uz-Cyrl-UZ", iOS can hand over "uz_Cyrl_UZ"; both are the same parts.
        val parts = localeTag.replace('_', '-').split('-').filter { it.isNotEmpty() }
        val language = parts.firstOrNull()?.lowercase() ?: return ContentLang.FALLBACK
        val script = parts.drop(1).firstOrNull { it.length == 4 }?.lowercase()
        return when (language) {
            "uz" -> if (script == "cyrl") ContentLang.UZ_CYRL else ContentLang.UZ_LATN
            "ru" -> ContentLang.RU
            "en" -> ContentLang.EN
            else -> ContentLang.FALLBACK
        }
    }
}

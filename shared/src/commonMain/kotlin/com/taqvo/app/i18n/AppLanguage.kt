package com.taqvo.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember

/**
 * The languages the reader can put the app in, each named in itself. The first is the default:
 * Uzbek in Cyrillic, whatever language the phone is in. The choice is the app's own, kept in
 * Settings; it does not follow the phone, and the phone's per-app language page no longer decides.
 */
enum class AppLanguage(val tag: String, val endonym: String) {
    UZBEK_CYRILLIC("uz-Cyrl", "Ўзбекча"),
    UZBEK_LATIN("uz", "Oʻzbekcha"),
    ENGLISH("en", "English"),
    ARABIC("ar", "العربية"),
    FRENCH("fr", "Français"),
    TURKISH("tr", "Türkçe"),
    INDONESIAN("id", "Bahasa Indonesia"),
    URDU("ur", "اردو"),
    BENGALI("bn", "বাংলা"),
    ;

    companion object {
        val DEFAULT = UZBEK_CYRILLIC

        /** The stored tag; the default for none or one this build does not know. */
        fun of(tag: String?): AppLanguage = entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: DEFAULT
    }
}

/**
 * Makes the platform's own formatting (clock digits, month names, the locale the Android resources
 * and widgets are drawn in) follow [tag]. Called before anything reads the language, and again
 * when the reader picks another.
 */
internal expect fun applyAppLanguage(tag: String)

/**
 * Puts everything inside in [language]: the platform locale first, then the subtree is rebuilt
 * under a new key, so every string, format and layout direction is looked up afresh. State held
 * above this call — the navigator — is untouched, so the reader lands on the screen they were on.
 */
@Composable
fun ProvideAppLanguage(language: AppLanguage, content: @Composable () -> Unit) {
    remember(language) { applyAppLanguage(language.tag) }
    key(language) { content() }
}

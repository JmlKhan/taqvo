package com.taqvo.app.i18n

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {

    @Test
    fun uzbekInCyrillicIsTheDefaultWhenNothingIsStoredOrTheTagIsUnknown() {
        assertEquals(AppLanguage.UZBEK_CYRILLIC, AppLanguage.DEFAULT)
        assertEquals(AppLanguage.UZBEK_CYRILLIC, AppLanguage.of(null))
        assertEquals(AppLanguage.UZBEK_CYRILLIC, AppLanguage.of("de"))
    }

    @Test
    fun everyLanguageRoundTripsThroughItsTag() {
        for (language in AppLanguage.entries) assertEquals(language, AppLanguage.of(language.tag))
    }

    @Test
    fun everyChoiceIsAnInterfaceLanguageTheAppCanSpeak() {
        for (language in AppLanguage.entries) {
            assertEquals(language.tag, UiLanguage.of(language.tag).code, language.tag)
        }
    }
}

package com.taqvo.app.i18n

import java.util.Locale

internal actual fun applyAppLanguage(tag: String) {
    Locale.setDefault(Locale.forLanguageTag(tag))
}

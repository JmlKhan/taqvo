package com.taqvo.app.widget

import com.taqvo.app.adhkar.AdhkarItem
import com.taqvo.app.adhkar.AdhkarText
import com.taqvo.app.i18n.PlatformFormat
import com.taqvo.app.i18n.UiLanguage
import com.taqvo.app.wallpaper.WallpaperTexts

/**
 * Fills the dua widget's mirror from the adhkar: the supplications a wallpaper may carry (said at
 * any hour, short enough to stay whole; [WallpaperTexts.duas]) with their translation in
 * [languageTag]'s language. In `shared` for the same reason [AyahPoolMirrorWriter] is: the adhkar
 * file is a Compose resource, which neither widget process reads.
 */
object DuaPoolMirrorWriter {
    fun write(
        store: KeyValueStore,
        items: List<AdhkarItem>,
        text: AdhkarText?,
        languageTag: String,
        format: PlatformFormat,
    ): DuaPoolMirror {
        val mirror = DuaPoolMirror(
            languageTag = languageTag,
            translationRtl = UiLanguage.of(languageTag).rtl,
            arabicIndicDigits = format.usesNativeDigits(),
            entries = WallpaperTexts.duas(items, text).map { DuaPoolEntry(it.hisn, it.arabic, it.translation.orEmpty()) },
        )
        DuaPoolMirror.write(store, mirror)
        return mirror
    }
}

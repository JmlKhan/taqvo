package com.taqvo.app.widget

import com.taqvo.app.resources.Res
import com.taqvo.app.resources.widget_background_frosted_subtitle
import com.taqvo.app.resources.widget_background_frosted_title
import com.taqvo.app.resources.widget_background_translucent_subtitle
import com.taqvo.app.resources.widget_background_translucent_title
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The spec's fourth widget background option is worded per platform — Android describes real
 * translucency, iOS describes its own system material — and neither user should ever see the
 * other's wording. [translucentOrFrostedTitleKey] and [translucentOrFrostedSubtitleKey] are pure
 * functions of a boolean precisely so both branches can be checked here without a
 * platform-specific test source set; [isIosPlatform] itself is a one-line `expect`/`actual` fact
 * that a build for each platform already proves.
 */
class WidgetBackgroundLabelsTest {

    @Test
    fun androidTitleIsTranslucent() {
        assertEquals(Res.string.widget_background_translucent_title, translucentOrFrostedTitleKey(isIos = false))
    }

    @Test
    fun iosTitleIsFrosted() {
        assertEquals(Res.string.widget_background_frosted_title, translucentOrFrostedTitleKey(isIos = true))
    }

    @Test
    fun theTwoTitlesAreNeverTheSame() {
        assertEquals(false, translucentOrFrostedTitleKey(true) == translucentOrFrostedTitleKey(false))
    }

    @Test
    fun androidSubtitleIsTranslucent() {
        assertEquals(
            Res.string.widget_background_translucent_subtitle,
            translucentOrFrostedSubtitleKey(isIos = false),
        )
    }

    @Test
    fun iosSubtitleIsFrosted() {
        assertEquals(Res.string.widget_background_frosted_subtitle, translucentOrFrostedSubtitleKey(isIos = true))
    }

    @Test
    fun theTwoSubtitlesAreNeverTheSame() {
        assertEquals(false, translucentOrFrostedSubtitleKey(true) == translucentOrFrostedSubtitleKey(false))
    }
}

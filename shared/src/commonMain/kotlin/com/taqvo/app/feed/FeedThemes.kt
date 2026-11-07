package com.taqvo.app.feed

import org.jetbrains.compose.resources.DrawableResource
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.theme_01
import com.taqvo.app.resources.theme_01_thumb
import com.taqvo.app.resources.theme_02
import com.taqvo.app.resources.theme_02_thumb
import com.taqvo.app.resources.theme_03
import com.taqvo.app.resources.theme_03_thumb
import com.taqvo.app.resources.theme_04
import com.taqvo.app.resources.theme_04_thumb
import com.taqvo.app.resources.theme_05
import com.taqvo.app.resources.theme_05_thumb
import com.taqvo.app.resources.theme_06
import com.taqvo.app.resources.theme_06_thumb
import com.taqvo.app.resources.theme_07
import com.taqvo.app.resources.theme_07_thumb
import com.taqvo.app.resources.theme_08
import com.taqvo.app.resources.theme_08_thumb
import com.taqvo.app.resources.theme_09
import com.taqvo.app.resources.theme_09_thumb
import com.taqvo.app.resources.theme_10
import com.taqvo.app.resources.theme_10_thumb
import com.taqvo.app.resources.theme_11
import com.taqvo.app.resources.theme_11_thumb
import com.taqvo.app.resources.theme_12
import com.taqvo.app.resources.theme_12_thumb
import com.taqvo.app.resources.theme_13
import com.taqvo.app.resources.theme_13_thumb
import com.taqvo.app.resources.theme_14
import com.taqvo.app.resources.theme_14_thumb
import com.taqvo.app.resources.theme_15
import com.taqvo.app.resources.theme_15_thumb
import com.taqvo.app.resources.theme_16
import com.taqvo.app.resources.theme_16_thumb
import com.taqvo.app.resources.theme_17
import com.taqvo.app.resources.theme_17_thumb
import com.taqvo.app.resources.theme_18
import com.taqvo.app.resources.theme_18_thumb
import com.taqvo.app.resources.theme_19
import com.taqvo.app.resources.theme_19_thumb
import com.taqvo.app.resources.theme_20
import com.taqvo.app.resources.theme_20_thumb
import com.taqvo.app.resources.theme_21
import com.taqvo.app.resources.theme_21_thumb
import com.taqvo.app.resources.theme_22
import com.taqvo.app.resources.theme_22_thumb
import com.taqvo.app.resources.theme_23
import com.taqvo.app.resources.theme_23_thumb
import com.taqvo.app.resources.theme_24
import com.taqvo.app.resources.theme_24_thumb
import com.taqvo.app.resources.theme_25
import com.taqvo.app.resources.theme_25_thumb
import com.taqvo.app.resources.theme_26
import com.taqvo.app.resources.theme_26_thumb

/**
 * One background for the feed's cards (the Mavzular screen): a photo, its small copy for the grid,
 * and how bright the band the words sit on is (0–255, measured once when the photos were bundled),
 * which decides how much shade goes behind the words.
 */
class FeedTheme(
    val id: String,
    val image: DrawableResource,
    val thumb: DrawableResource,
    val brightness: Int,
) {
    /** A bright photo needs a darker shade behind white words than a night sky does. */
    val scrim: Float get() = (0.18f + (brightness / 255f) * 0.42f).coerceIn(0.18f, 0.6f)
}

/**
 * The bundled themes, from the repository's `themes/` folder in file order, renamed theme_NN.
 *
 * LICENCE: where these photos come from, and on what terms, is not recorded yet. They must be
 * cleared (or replaced) before release, and credited on the Attribution screen if required.
 */
object FeedThemes {
    val ALL: List<FeedTheme> = listOf(
        FeedTheme("theme_01", Res.drawable.theme_01, Res.drawable.theme_01_thumb, brightness = 105), // themes/0fd73ae2…jpg
        FeedTheme("theme_02", Res.drawable.theme_02, Res.drawable.theme_02_thumb, brightness = 156), // themes/1d6d69c1…jpg
        FeedTheme("theme_03", Res.drawable.theme_03, Res.drawable.theme_03_thumb, brightness = 50), // themes/2264c3c3…jpg
        FeedTheme("theme_04", Res.drawable.theme_04, Res.drawable.theme_04_thumb, brightness = 145), // themes/2bcfa9cb…jpg
        FeedTheme("theme_05", Res.drawable.theme_05, Res.drawable.theme_05_thumb, brightness = 75), // themes/2d25e832…jpg
        FeedTheme("theme_06", Res.drawable.theme_06, Res.drawable.theme_06_thumb, brightness = 92), // themes/33201b2a…jpg
        FeedTheme("theme_07", Res.drawable.theme_07, Res.drawable.theme_07_thumb, brightness = 18), // themes/3c2a44f2…jpg
        FeedTheme("theme_08", Res.drawable.theme_08, Res.drawable.theme_08_thumb, brightness = 85), // themes/3f4fa7e8…jpg
        FeedTheme("theme_09", Res.drawable.theme_09, Res.drawable.theme_09_thumb, brightness = 147), // themes/4519ba15…jpg
        FeedTheme("theme_10", Res.drawable.theme_10, Res.drawable.theme_10_thumb, brightness = 207), // themes/498de7b6…jpg
        FeedTheme("theme_11", Res.drawable.theme_11, Res.drawable.theme_11_thumb, brightness = 44), // themes/4a44c99b…jpg
        FeedTheme("theme_12", Res.drawable.theme_12, Res.drawable.theme_12_thumb, brightness = 227), // themes/4fc02f41…jpg
        FeedTheme("theme_13", Res.drawable.theme_13, Res.drawable.theme_13_thumb, brightness = 40), // themes/64ac6e67…jpg
        FeedTheme("theme_14", Res.drawable.theme_14, Res.drawable.theme_14_thumb, brightness = 75), // themes/6fe2c198…jpg
        FeedTheme("theme_15", Res.drawable.theme_15, Res.drawable.theme_15_thumb, brightness = 62), // themes/764f64ed…jpg
        FeedTheme("theme_16", Res.drawable.theme_16, Res.drawable.theme_16_thumb, brightness = 102), // themes/7d55f302…jpg
        FeedTheme("theme_17", Res.drawable.theme_17, Res.drawable.theme_17_thumb, brightness = 133), // themes/9a19a4e0…jpg
        FeedTheme("theme_18", Res.drawable.theme_18, Res.drawable.theme_18_thumb, brightness = 183), // themes/ac31af68…jpg
        FeedTheme("theme_19", Res.drawable.theme_19, Res.drawable.theme_19_thumb, brightness = 115), // themes/b6fa2c35…jpg
        FeedTheme("theme_20", Res.drawable.theme_20, Res.drawable.theme_20_thumb, brightness = 24), // themes/bfe74d20…jpg
        FeedTheme("theme_21", Res.drawable.theme_21, Res.drawable.theme_21_thumb, brightness = 70), // themes/cd86f56d…jpg
        FeedTheme("theme_22", Res.drawable.theme_22, Res.drawable.theme_22_thumb, brightness = 16), // themes/d4474071…jpg
        FeedTheme("theme_23", Res.drawable.theme_23, Res.drawable.theme_23_thumb, brightness = 34), // themes/f6455f24…jpg
        FeedTheme("theme_24", Res.drawable.theme_24, Res.drawable.theme_24_thumb, brightness = 75), // themes/f8a1643a…jpg
        FeedTheme("theme_25", Res.drawable.theme_25, Res.drawable.theme_25_thumb, brightness = 28), // themes/f922662c…jpg
        FeedTheme("theme_26", Res.drawable.theme_26, Res.drawable.theme_26_thumb, brightness = 49), // themes/fee874c3…jpg
    )

    /** The mosque under a crescent: what a reader sees before choosing. */
    const val DEFAULT_ID = "theme_06"

    fun of(id: String?): FeedTheme = ALL.firstOrNull { it.id == id } ?: ALL.first { it.id == DEFAULT_ID }
}

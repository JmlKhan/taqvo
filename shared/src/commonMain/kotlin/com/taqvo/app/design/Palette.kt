package com.taqvo.app.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class TaqvoColors(
    val background: Color,
    val surface: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val ring: Color,
)

val LightColors = TaqvoColors(
    background = Color(0xFFFBFAF7),
    surface = Color(0xFFFFFFFF),
    hairline = Color(0xFFE7E5DD),
    textPrimary = Color(0xFF16160F),
    textSecondary = Color(0xFF6F6E62),
    textTertiary = Color(0xFFA5A498),
    accent = Color(0xFFB5820B),
    ring = Color(0xFFE3A21C),
)

val DarkColors = TaqvoColors(
    background = Color(0xFF0B0D0C),
    surface = Color(0xFF131614),
    hairline = Color(0xFF232825),
    textPrimary = Color(0xFFF1F3F1),
    textSecondary = Color(0xFF8C948F),
    textTertiary = Color(0xFF5A625D),
    accent = Color(0xFFF0B429),
    ring = Color(0xFFF0B429),
)

/**
 * The stand-in wallpaper the widget previews in Settings › Widgets sit on — a flat, deliberately
 * uninteresting neutral, not a photo, so the eye reads the widget card rather than the backdrop.
 *
 * It lives here rather than inline at the one call site for a greppable reason: `Palette.kt` and
 * `widget/WidgetPalette.kt` are meant to be the only two files in the tree holding a colour
 * literal, which is what keeps "the app uses amber and nothing else" a claim you can verify with
 * a search instead of a reading. It is not part of [TaqvoColors] because it is not a theme role —
 * it stands in for the user's wallpaper, which no theme controls, and so is the same in light and
 * dark.
 */
val PreviewWallpaper = Color(0xFF9C8F73)

/**
 * The stand-in Lock Screen behind the iPhone lock-screen widget's preview in Settings › Widgets: a
 * dark, neutral slate, because the widget sits under the clock in white on a Lock Screen whichever
 * theme the phone is in. Not a theme role for the same reason [PreviewWallpaper] is not; the type
 * drawn on it is [DarkColors]' own, so it matches the dark theme's ink.
 */
val PreviewLockScreen = Color(0xFF22262A)

/**
 * One wallpaper style's colours (Settings › Wallpapers): a vertical wash from [top] to [bottom],
 * the words in [ink], the translation in [secondary], and the mihrab's point and the reference in
 * [accent]. They are not [TaqvoColors] because a wallpaper is not themed: it is a picture that
 * stays as chosen whichever theme the phone is in. Each [ink] and [secondary] holds at least
 * 6:1 against both ends of its wash, and each [accent] at least 4.5:1, so the words read on any
 * part of the card.
 */
@Immutable
data class WallpaperColors(
    val top: Color,
    val bottom: Color,
    val ink: Color,
    val secondary: Color,
    val accent: Color,
)

/** The dark theme's own ink and amber over a deep green-black: the app's night. */
val WallpaperNight = WallpaperColors(
    top = Color(0xFF16201B),
    bottom = Color(0xFF0B0D0C),
    ink = Color(0xFFF1F3F1),
    secondary = Color(0xFFB4BBB6),
    accent = Color(0xFFF0B429),
)

/** The light theme's paper warming into sand. */
val WallpaperDawn = WallpaperColors(
    top = Color(0xFFFBFAF7),
    bottom = Color(0xFFEFE4C9),
    ink = Color(0xFF16160F),
    secondary = Color(0xFF55544A),
    accent = Color(0xFF7D5807),
)

/** A dusk of burnt amber going down to brown-black. */
val WallpaperDusk = WallpaperColors(
    top = Color(0xFF4A3414),
    bottom = Color(0xFF120E08),
    ink = Color(0xFFF7EEDC),
    secondary = Color(0xFFD5C6A8),
    accent = Color(0xFFF0B429),
)

/**
 * The feed's text colours (feed spec §7), chosen in the text style editor. Like the wallpapers they
 * sit on, they are not themed: the reader picks what reads on their wallpaper. "Auto" is not here:
 * it is the wallpaper's own [WallpaperColors.ink].
 */
object FeedInk {
    val white = Color(0xFFFFFFFF)
    val cream = Color(0xFFF7EEDC)
    val black = Color(0xFF16160F)
    val amber = Color(0xFFF0B429)
    val sage = Color(0xFFB9D3C2)
    val rose = Color(0xFFF2C4C0)

    /** The shadow under text and chrome drawn over a wallpaper, so it reads on the light Dawn wash. */
    val shadow = Color(0x66000000)

    /** The chips and the goal pill's ground over the wallpaper. */
    val chip = Color(0x33000000)

    /** The home screen's chips and floating buttons (the bell, the help chips, the three pills): a deep green glass. */
    val glass = Color(0xE0132219)

    /** The Mavzular and Ilovalar screens' ground, and their tiles over it. */
    val deep = Color(0xFF0E1A14)
    val tile = Color(0xFF1A2B22)

    /** The heart's fill once liked. */
    val heart = Color(0xFFE5484D)
}

/** The Lock Screen slate of the widget previews, deepened. */
val WallpaperSlate = WallpaperColors(
    top = PreviewLockScreen,
    bottom = Color(0xFF0E1012),
    ink = Color(0xFFF1F3F1),
    secondary = Color(0xFFB0B6BC),
    accent = Color(0xFFF0B429),
)

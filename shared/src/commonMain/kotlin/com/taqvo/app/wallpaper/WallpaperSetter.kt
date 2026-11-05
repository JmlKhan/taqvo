package com.taqvo.app.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

/** Which screen a wallpaper goes on, where the phone lets the app choose. */
enum class WallpaperTarget { HOME, LOCK, BOTH }

/** What became of a wallpaper handed to the phone. */
enum class WallpaperOutcome {
    /** It is on the screen (Android). */
    SET,

    /** It is in the photo library, for the reader to set from there (iOS). */
    SAVED,

    /** The reader refused the photo library, or the phone forbids changing the wallpaper. */
    REFUSED,

    /** Anything else; nothing was changed. */
    FAILED,
}

/**
 * The one platform question the wallpaper screen asks. Android sets a wallpaper itself, on the home
 * screen, the Lock Screen or both; iOS lets no app do that, so there the picture goes to the photo
 * library, and the screen says how to set it from Photos.
 */
interface WallpaperSetter {
    /** True where [apply] puts the picture on the screen; false where it only saves it. */
    val setsDirectly: Boolean

    /** The phone's screen in pixels, portrait: the size the picture is drawn at. */
    fun screenSize(): IntSize

    /** Hands [image] to the phone. [target] is ignored where [setsDirectly] is false. Never throws. */
    suspend fun apply(image: ImageBitmap, target: WallpaperTarget): WallpaperOutcome
}

expect fun createWallpaperSetter(): WallpaperSetter

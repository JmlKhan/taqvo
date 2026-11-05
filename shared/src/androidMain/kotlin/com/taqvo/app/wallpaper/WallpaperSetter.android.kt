package com.taqvo.app.wallpaper

import android.app.WallpaperManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.taqvo.app.settings.appContext
import kotlin.coroutines.cancellation.CancellationException

actual fun createWallpaperSetter(): WallpaperSetter = AndroidWallpaperSetter

private object AndroidWallpaperSetter : WallpaperSetter {
    override val setsDirectly = true

    /**
     * The whole display, bars included, since a wallpaper runs under both. Not
     * `WallpaperManager.desiredMinimumWidth`, which on launchers that scroll the wallpaper is
     * several screens wide: the card would then be centred across pages and cut on the first.
     */
    override fun screenSize(): IntSize {
        val windows = appContext.getSystemService(WindowManager::class.java)
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windows.maximumWindowMetrics.bounds.let { it.width() to it.height() }
        } else {
            @Suppress("DEPRECATION")
            DisplayMetrics().also { windows.defaultDisplay.getRealMetrics(it) }.let { it.widthPixels to it.heightPixels }
        }
        // Portrait whichever way the phone is held while choosing.
        return IntSize(minOf(w, h), maxOf(w, h))
    }

    override suspend fun apply(image: ImageBitmap, target: WallpaperTarget): WallpaperOutcome =
        withContext(Dispatchers.IO) {
            val manager = WallpaperManager.getInstance(appContext)
            // A managed or kiosk phone can forbid it; nothing is tried there.
            if (!manager.isWallpaperSupported || !manager.isSetWallpaperAllowed) return@withContext WallpaperOutcome.REFUSED
            val which = when (target) {
                WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }
            try {
                // allowBackup true: a phone restored from backup keeps the reader's wallpaper.
                // The id is 0 when the system took nothing.
                val id = manager.setBitmap(image.asAndroidBitmap(), null, true, which)
                if (id != 0) WallpaperOutcome.SET else WallpaperOutcome.FAILED
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (refused: SecurityException) {
                WallpaperOutcome.REFUSED
            } catch (e: Exception) {
                WallpaperOutcome.FAILED
            }
        }
}

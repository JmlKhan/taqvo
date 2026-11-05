package com.taqvo.app.wallpaper

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.IntSize
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIImage
import platform.UIKit.UIScreen
import kotlin.coroutines.resume

actual fun createWallpaperSetter(): WallpaperSetter = IosWallpaperSetter

/**
 * iOS gives no app the wallpaper, so the picture goes to the photo library and the reader sets it
 * from Photos (the screen says how). Add-only access: Taqvo can put a picture in and never sees
 * the library, which is the whole of what the Info.plist line asks for.
 */
@OptIn(ExperimentalForeignApi::class)
private object IosWallpaperSetter : WallpaperSetter {
    override val setsDirectly = false

    /** `nativeBounds` is in pixels and always portrait, which is what a wallpaper is. */
    override fun screenSize(): IntSize = UIScreen.mainScreen.nativeBounds.useContents {
        IntSize(size.width.toInt(), size.height.toInt())
    }

    override suspend fun apply(image: ImageBitmap, target: WallpaperTarget): WallpaperOutcome {
        if (!addAccess()) return WallpaperOutcome.REFUSED
        val png = withContext(Dispatchers.Default) {
            Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
        } ?: return WallpaperOutcome.FAILED
        val picture = png.usePinned { pinned ->
            UIImage(data = NSData.create(bytes = pinned.addressOf(0), length = png.size.toULong()))
        }
        return suspendCancellableCoroutine { cont ->
            PHPhotoLibrary.sharedPhotoLibrary().performChanges(
                { PHAssetChangeRequest.creationRequestForAssetFromImage(picture) },
                completionHandler = { saved, _ ->
                    if (cont.isActive) cont.resume(if (saved) WallpaperOutcome.SAVED else WallpaperOutcome.FAILED)
                },
            )
        }
    }

    /** Asks once; afterwards the system answers from the reader's earlier choice without asking. */
    private suspend fun addAccess(): Boolean = suspendCancellableCoroutine { cont ->
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
            if (cont.isActive) cont.resume(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited)
        }
    }
}

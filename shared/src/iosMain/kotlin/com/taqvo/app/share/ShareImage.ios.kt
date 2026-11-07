package com.taqvo.app.share

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController
import kotlin.coroutines.resume

/** Add-only photo access, the same the wallpapers ask for (Info.plist's NSPhotoLibraryAddUsageDescription). */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun saveImageToGallery(image: ImageBitmap): SaveOutcome {
    val granted = suspendCancellableCoroutine { cont ->
        PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) { status ->
            if (cont.isActive) cont.resume(status == PHAuthorizationStatusAuthorized || status == PHAuthorizationStatusLimited)
        }
    }
    if (!granted) return SaveOutcome.REFUSED
    val picture = encode(image, EncodedImageFormat.JPEG, 90) ?: return SaveOutcome.FAILED
    return suspendCancellableCoroutine { cont ->
        PHPhotoLibrary.sharedPhotoLibrary().performChanges(
            { PHAssetChangeRequest.creationRequestForAssetFromImage(picture) },
            completionHandler = { saved, _ -> if (cont.isActive) cont.resume(if (saved) SaveOutcome.SAVED else SaveOutcome.FAILED) },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun shareImage(image: ImageBitmap, text: String) {
    val picture = encode(image, EncodedImageFormat.PNG, 100) ?: return
    val root = keyWindow()?.rootViewController ?: return
    var top: UIViewController = root
    while (top.presentedViewController != null) top = top.presentedViewController!!
    val sheet = UIActivityViewController(activityItems = listOf(picture, text), applicationActivities = null)
    // iPad: the sheet is a popover and must be anchored, or UIKit raises.
    sheet.popoverPresentationController?.let { popover ->
        popover.sourceView = top.view
        top.view.bounds.useContents { popover.sourceRect = CGRectMake(size.width / 2.0, size.height / 2.0, 0.0, 0.0) }
        popover.permittedArrowDirections = 0uL
    }
    top.presentViewController(sheet, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private suspend fun encode(image: ImageBitmap, format: EncodedImageFormat, quality: Int): UIImage? {
    val bytes = withContext(Dispatchers.Default) {
        Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(format, quality)?.bytes
    } ?: return null
    return bytes.usePinned { pinned -> UIImage(data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())) }
}

private fun keyWindow(): UIWindow? {
    val scenes = UIApplication.sharedApplication.connectedScenes.mapNotNull { it as? UIWindowScene }
    return scenes.flatMap { scene -> scene.windows.mapNotNull { it as? UIWindow } }.firstOrNull { it.isKeyWindow() }
        ?: scenes.firstOrNull()?.windows?.firstOrNull() as? UIWindow
}

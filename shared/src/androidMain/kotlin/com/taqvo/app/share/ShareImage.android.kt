package com.taqvo.app.share

import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.taqvo.app.settings.appContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Through MediaStore, which on Android 10 and later needs no permission at all. Older phones would
 * need WRITE_EXTERNAL_STORAGE, which Tazkir does not ask for: they are told [SaveOutcome.UNSUPPORTED]
 * and the screen offers the share sheet instead, whose "Save" targets do the same job.
 */
actual suspend fun saveImageToGallery(image: ImageBitmap): SaveOutcome = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext SaveOutcome.UNSUPPORTED
    val resolver = appContext.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "taqvo-${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Tazkir")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    try {
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext SaveOutcome.FAILED
        val written = resolver.openOutputStream(uri)?.use { out ->
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        } ?: false
        if (!written) {
            resolver.delete(uri, null, null)
            return@withContext SaveOutcome.FAILED
        }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        SaveOutcome.SAVED
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        SaveOutcome.FAILED
    }
}

/**
 * The PNG goes to the cache's `shared/` folder (one file, overwritten each time) and out through
 * the app's FileProvider, whose grant lasts as long as the receiving app's task.
 */
actual suspend fun shareImage(image: ImageBitmap, text: String) {
    try {
        val file = withContext(Dispatchers.IO) {
            val dir = File(appContext.cacheDir, SHARE_DIR).apply { mkdirs() }
            File(dir, SHARE_FILE).also { f -> f.outputStream().use { image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            // The ClipData is what carries the read grant through the chooser to the chosen app.
            clipData = ClipData.newRawUri(null, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        appContext.startActivity(chooser)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        // A share that cannot start is a no-op; the sheet stays open for another try.
    }
}

private const val JPEG_QUALITY = 90
private const val SHARE_DIR = "shared"
private const val SHARE_FILE = "taqvo-card.png"

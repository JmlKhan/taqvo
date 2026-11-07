package com.taqvo.app.share

import androidx.compose.ui.graphics.ImageBitmap

/** What became of a card image handed to the photo library (feed spec §9). */
enum class SaveOutcome {
    SAVED,

    /** The reader refused the photo library. */
    REFUSED,

    /** This phone cannot save without a permission the app does not ask for (Android 9 and older). */
    UNSUPPORTED,
    FAILED,
}

/** Puts [image] in the phone's photo library as a JPEG. Never throws. */
expect suspend fun saveImageToGallery(image: ImageBitmap): SaveOutcome

/** Hands [image] (as a PNG) and [text] to the platform's share sheet. Never throws. */
expect suspend fun shareImage(image: ImageBitmap, text: String)

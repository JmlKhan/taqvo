package com.taqvo.app.share

import android.content.Intent
import com.taqvo.app.settings.appContext

actual fun shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    // Started from the application context, which has no task of its own to start on.
    val chooser = Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { appContext.startActivity(chooser) }
}

package com.taqvo.app.design

import android.provider.Settings
import com.taqvo.app.settings.appContext

/** "Remove animations" in Accessibility (or Developer options) sets the animator scale to 0. */
actual fun reduceMotion(): Boolean = runCatching {
    Settings.Global.getFloat(appContext.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

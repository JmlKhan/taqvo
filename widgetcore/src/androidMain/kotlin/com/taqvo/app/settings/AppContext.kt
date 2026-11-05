package com.taqvo.app.settings

import android.content.Context

/**
 * Assigned once from `TaqvoApplication.onCreate`. It lives in `:widgetcore` rather than in
 * `shared` only because the widget model's Android `KeyValueStore` needs a `Context` and
 * `widgetcore` cannot depend on `shared`. `shared` re-exposes it through
 * `api(project(":widgetcore"))`, so every existing `import com.taqvo.app.settings.appContext`
 * still resolves.
 */
lateinit var appContext: Context

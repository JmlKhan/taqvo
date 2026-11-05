package com.taqvo.app.recitation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import okio.FileSystem
import com.taqvo.app.settings.createDataStore

/**
 * The library the app runs on, over the process-wide DataStore and the real file system. Built the
 * way the rest of the graph is — a plain factory called from `AppContainer`, no framework.
 */
fun createRecitationLibrary(
    store: DataStore<Preferences> = createDataStore(),
    paths: RecitationPaths = createRecitationPaths(),
): RecitationLibrary = RecitationLibrary(paths, store, FileSystem.SYSTEM)

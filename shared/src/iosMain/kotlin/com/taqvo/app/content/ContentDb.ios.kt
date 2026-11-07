package com.taqvo.app.content

import app.cash.sqldelight.db.SqlDriver
import com.taqvo.app.quran.openBundledDatabase

actual fun createContentDriver(): SqlDriver = openBundledDatabase(ContentDb.FILE, ContentDb.RESOURCE, ContentDb.VERSION)

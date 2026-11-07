package com.taqvo.app.feed

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.taqvo.app.feed.db.FeedDatabase
import com.taqvo.app.settings.appContext

actual fun createFeedDriver(): SqlDriver = AndroidSqliteDriver(FeedDatabase.Schema, appContext, FeedDb.FILE)

package com.taqvo.app.feed

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.taqvo.app.feed.db.FeedDatabase

/**
 * In the driver's default directory, and left in backups: unlike quran.db and content.db, nothing
 * re-creates it, so a restored phone keeps the reader's favorites and collections.
 */
actual fun createFeedDriver(): SqlDriver = NativeSqliteDriver(FeedDatabase.Schema, FeedDb.FILE)

package com.taqvo.app.quran

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.taqvo.app.quran.db.QuranDatabase
import com.taqvo.app.resources.Res
import com.taqvo.app.settings.appContext

actual fun createQuranDriver(): SqlDriver =
    openBundledDatabase(QuranDb.FILE, QuranDb.RESOURCE, QuranDb.VERSION, QuranDatabase.Schema)

/**
 * Opens a database that ships prebuilt in the app's resources — quran.db, and the feed's
 * content.db — copying it out first if it is missing or carries a different `user_version`.
 */
internal fun openBundledDatabase(
    fileName: String,
    resource: String,
    version: Int,
    schema: app.cash.sqldelight.db.SqlSchema<app.cash.sqldelight.db.QueryResult.Value<Unit>>,
): SqlDriver {
    val file = appContext.getDatabasePath(fileName)
    if (!file.exists() || userVersion(file) != version) {
        file.parentFile?.mkdirs()
        val bytes = runBlocking { Res.readBytes(resource) }
        val temp = File(file.parentFile, "$fileName.tmp")
        temp.writeBytes(bytes)
        // A stale -wal/-shm/-journal beside the old file would otherwise be replayed against the
        // freshly installed one on next open, applying leftover journalled changes from a
        // database that no longer exists.
        listOf("-wal", "-shm", "-journal").forEach { suffix -> File(file.parentFile, "$fileName$suffix").delete() }
        // File.renameTo is atomic when the source and destination are on the same filesystem,
        // which they are here (both under the app's private databases directory).
        check(temp.renameTo(file)) { "Failed to install $file from $temp" }
    }
    return AndroidSqliteDriver(
        schema = schema,
        context = appContext,
        name = fileName,
        // Opened at the bundled file's own user_version, not SQLDelight's schema version (which
        // stays 1: the schema never changes, only the text inside it): the framework compares the
        // two and, with the file ahead, would call onDowngrade and throw. The bundled file already
        // has the schema, so create, upgrade and downgrade are all deliberately nothing.
        callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        },
    )
}

/**
 * Reads `user_version` straight from the SQLite file header (bytes 60..63, big-endian) instead of
 * opening the database, so checking the version never requires two opens of the same file.
 * Returns -1 (never equal to a real version) for a missing or truncated file, which forces a copy.
 */
private fun userVersion(file: File): Int = runCatching {
    RandomAccessFile(file, "r").use { raf ->
        if (raf.length() < 64) return -1
        raf.seek(60)
        val bytes = ByteArray(4)
        raf.readFully(bytes)
        ((bytes[0].toInt() and 0xFF) shl 24) or
            ((bytes[1].toInt() and 0xFF) shl 16) or
            ((bytes[2].toInt() and 0xFF) shl 8) or
            (bytes[3].toInt() and 0xFF)
    }
}.getOrDefault(-1)

internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

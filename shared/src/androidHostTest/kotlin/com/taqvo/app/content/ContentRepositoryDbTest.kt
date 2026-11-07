package com.taqvo.app.content

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The bundled content.db against what the feed assumes of it (feed content spec §1–§3). */
class ContentRepositoryDbTest {
    private val repo = ContentRepository(driverFactory = { JdbcSqliteDriver("jdbc:sqlite:" + dbFile().absolutePath) }, io = Dispatchers.Unconfined)

    @Test fun theFileCarriesTheVersionTheAppCopiesItAt() {
        // A rebuilt content.db that kept the old user_version would never reach a phone that has the old one.
        RandomAccessFile(dbFile(), "r").use { raf ->
            raf.seek(60)
            assertEquals(ContentDb.VERSION, raf.readInt())
        }
    }

    @Test fun everyItemIsReadWithItsTopics() = runTest {
        val catalog = repo.catalog()
        assertEquals(499, catalog.items.size)
        assertEquals(13, catalog.topics.size)
        catalog.items.forEach { item ->
            assertTrue(item.topics.isNotEmpty() && item.topics.size <= 3, "${item.id} has ${item.topics}")
            assertEquals(item.primaryTopic, item.topics.first())
            assertTrue(item.primaryTopic in catalog.topics, "${item.id}'s topic ${item.primaryTopic} is not a topic")
            assertNotNull(item.surah)
            assertNotNull(item.ayah)
            assertTrue(item.arabic.isNotBlank())
        }
    }

    @Test fun everyItemHasItsTextInEveryLanguage() = runTest {
        val ids = repo.catalog().items.map { it.id }.toSet()
        ContentLang.entries.forEach { lang ->
            val texts = repo.texts(lang)
            assertEquals(ids, texts.keys, "$lang is missing items")
            assertTrue(texts.values.none { it.isBlank() }, "$lang has a blank text")
        }
    }

    @Test fun everyTopicIsLabelledInEveryLanguage() = runTest {
        val topics = repo.catalog().topics.toSet()
        ContentLang.entries.forEach { lang -> assertEquals(topics, repo.topicLabels(lang).keys, "$lang labels") }
    }

    @Test fun theSourcesNameEveryLanguageAndTheArabic() = runTest {
        val langs = repo.sources().map { it.lang }.toSet()
        assertEquals(ContentLang.entries.map { it.tag }.toSet() + "ar", langs)
    }

    @Test fun everyTopicHoldsEnoughForAFullBatchOnItsOwn() = runTest {
        // A topic thinner than a batch would leave a reader weighted towards it seeing others early;
        // the content spec's target is 40, and the floor here is what the feed actually needs.
        repo.catalog().byTopic.forEach { (topic, items) -> assertTrue(items.size >= 20, "$topic has ${items.size}") }
    }

    companion object {
        fun dbFile(): File {
            val moduleRelative = File("src/commonMain/composeResources/files/content.db")
            if (moduleRelative.exists()) return moduleRelative
            val rootRelative = File("shared/src/commonMain/composeResources/files/content.db")
            if (rootRelative.exists()) return rootRelative
            error("content.db not found at ${moduleRelative.absolutePath} or ${rootRelative.absolutePath}")
        }
    }
}

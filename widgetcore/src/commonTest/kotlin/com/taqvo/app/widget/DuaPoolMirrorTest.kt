package com.taqvo.app.widget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DuaPoolMirrorTest {

    private val mirror = DuaPoolMirror(
        languageTag = "ur-PK",
        translationRtl = true,
        arabicIndicDigits = true,
        entries = listOf(
            DuaPoolEntry(91, "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ", "اللہ پاک ہے اور اسی کی تعریف ہے\nدوسری سطر"),
            DuaPoolEntry(96, "أَسْتَغْفِرُ اللَّهَ", ""),
        ),
    )

    @Test
    fun roundTripsEveryFieldAnEmptyTranslationAndANewline() {
        assertEquals(mirror, DuaPoolMirror.deserialize(DuaPoolMirror.serialize(mirror)))
    }

    @Test
    fun anotherVersionOrAMalformedBlockReadsAsNoMirror() {
        val raw = DuaPoolMirror.serialize(mirror)
        assertNull(DuaPoolMirror.deserialize(raw.replaceFirst("1", "9")))
        assertNull(DuaPoolMirror.deserialize(raw + '\u001E'))
        assertNull(DuaPoolMirror.deserialize(""))
        // An ayah pool is not a dua pool.
        assertNull(DuaPoolMirror.deserialize(AyahPoolMirror.serialize(AyahPoolMirror("en", "none", false, false, emptyList()))))
    }

    @Test
    fun everyDayHasAnEntryAndTheCardTurnsOverWithTheDate() {
        val days = (20_000L until 20_002L).map { assertNotNull(mirror.entryFor(it, ayahSeed = 42L)) }
        assertEquals(2, days.toSet().size, "two entries, two consecutive days of one round: both shown")
        assertNull(mirror.copy(entries = emptyList()).entryFor(20_000L, 42L))
    }
}

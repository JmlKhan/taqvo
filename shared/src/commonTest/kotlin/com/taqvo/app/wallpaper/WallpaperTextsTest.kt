package com.taqvo.app.wallpaper

import kotlinx.coroutines.test.runTest
import com.taqvo.app.adhkar.AdhkarItem
import com.taqvo.app.adhkar.AdhkarKind
import com.taqvo.app.adhkar.AdhkarSet
import com.taqvo.app.adhkar.AdhkarText
import com.taqvo.app.adhkar.QuranRef
import com.taqvo.app.feature.quran.FakeQuranSource
import com.taqvo.app.quran.Ayah
import com.taqvo.app.quran.QuranText
import com.taqvo.app.quran.ReadingSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WallpaperTextsTest {

    private fun dua(id: String, hisn: Int, arabic: Map<String, String>) = AdhkarItem(
        id = id, hisn = hisn, kind = AdhkarKind.DUA, sets = AdhkarSet.entries.toSet(), count = 1,
        countDaily = false, quran = null, arabic = arabic, transliteration = emptyMap(), sources = emptyList(),
    )

    @Test
    fun onlyShortSupplicationsSaidAtAnyHourBecomeWallpapers() {
        val items = listOf(
            dua("hisn-091", 91, mapOf("both" to "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ")),
            // Says "we have entered the morning": it would be wrong on an evening lock screen.
            dua("hisn-090", 90, mapOf("morning" to "أَصْبَحْنَا")),
            dua("hisn-077", 77, mapOf("both" to "ا".repeat(WallpaperTexts.MAX_DUA_LENGTH + 1))),
            AdhkarItem(
                id = "hisn-075", hisn = 75, kind = AdhkarKind.QURAN, sets = AdhkarSet.entries.toSet(), count = 1,
                countDaily = false, quran = QuranRef(2, 255, null), arabic = mapOf("both" to "قرآن"),
                transliteration = emptyMap(), sources = emptyList(),
            ),
        )
        val text = AdhkarText("en", mapOf("hisn-091" to mapOf("both" to "Glory be to Allah and praise Him.")))

        val duas = WallpaperTexts.duas(items, text)

        assertEquals(listOf(91), duas.map { it.hisn })
        assertEquals("Glory be to Allah and praise Him.", duas.single().translation)
        assertTrue(!duas.single().quran)
    }

    @Test
    fun aSupplicationWithNoTranslationInTheLanguageHasNone() {
        val items = listOf(dua("hisn-096", 96, mapOf("both" to "أَسْتَغْفِرُ اللَّهَ")))
        assertNull(WallpaperTexts.duas(items, text = null).single().translation)
    }

    @Test
    fun ayahsCarryTheirRoundelAndTheReadersTranslation() = runTest {
        val quran = FakeQuranSource(
            ayahsBySurah = mapOf(94 to listOf(Ayah(94, 6, "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", page = 596, juz = 30, hizbQuarter = 240, sajdah = 0))),
            translationTextsById = mapOf("en.sahih" to mapOf(94 to mapOf(6 to "Indeed, with hardship [will be] ease."))),
        )

        val ayahs = WallpaperTexts.ayahs(quran, ReadingSettings(translationId = "en.sahih"))

        // The fake holds one pool ayah; the other ninety-nine are absent and skipped.
        val ayah = ayahs.single()
        assertEquals(94 to 6, ayah.surah to ayah.ayah)
        assertEquals(QuranText.withMarker("إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", 6), ayah.arabic)
        assertEquals("Indeed, with hardship [will be] ease.", ayah.translation)
        assertTrue(ayah.quran)
    }

    @Test
    fun withTheTranslationOffAnAyahIsArabicAlone() = runTest {
        val quran = FakeQuranSource(
            ayahsBySurah = mapOf(94 to listOf(Ayah(94, 6, "إِنَّ مَعَ ٱلْعُسْرِ يُسْرًا", page = 596, juz = 30, hizbQuarter = 240, sajdah = 0))),
            translationTextsById = mapOf("en.sahih" to mapOf(94 to mapOf(6 to "Indeed, with hardship [will be] ease."))),
        )

        val ayah = WallpaperTexts.ayahs(quran, ReadingSettings(translationId = ReadingSettings.NO_TRANSLATION)).single()

        assertNull(ayah.translation)
    }
}

package com.taqvo.app.ayah

import com.taqvo.app.widget.AyahPool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The ayah themes onboarding asks about (screen 05). */
class AyahThemesTest {

    @Test
    fun `every tagged ayah is one of the curated pool`() {
        AyahThemes.TAGGED.forEach { (theme, refs) ->
            refs.forEach { assertTrue(it in AyahPool.REFS, "$theme: $it is not in the pool") }
            assertEquals(refs.size, refs.distinct().size, "$theme lists an ayah twice")
        }
    }

    @Test
    fun `no pick mix or nothing left means the whole pool`() {
        val none: (Pair<Int, Int>) -> String? = { null }
        assertEquals(AyahPool.REFS, AyahThemes.refsFor(emptyList(), none))
        assertEquals(AyahPool.REFS, AyahThemes.refsFor(listOf("mix", "comfort"), none))
        assertEquals(AyahPool.REFS, AyahThemes.refsFor(listOf("short_ayahs"), none))
        assertEquals(AyahPool.REFS, AyahThemes.refsFor(listOf("unknown"), none))
    }

    @Test
    fun `picked themes are their union in pool order`() {
        val picked = AyahThemes.refsFor(listOf("comfort", "patience")) { null }
        val expected = AyahPool.REFS.filter {
            it in AyahThemes.TAGGED.getValue(AyahTheme.COMFORT) || it in AyahThemes.TAGGED.getValue(AyahTheme.PATIENCE)
        }
        assertEquals(expected, picked)
    }

    @Test
    fun `short is measured on the arabic`() {
        val short = AyahPool.REFS.first()
        val picked = AyahThemes.refsFor(listOf("short_ayahs")) { if (it == short) "قصير" else "ط".repeat(200) }
        assertEquals(listOf(short), picked)
    }

    @Test
    fun `the onboarding options are the theme ids`() {
        assertEquals(
            AyahTheme.entries.map { it.id },
            com.taqvo.app.feature.onboarding.ContentType.ALL,
        )
    }
}

package com.taqvo.app.ayah

import com.taqvo.app.i18n.PlatformFormat
import com.taqvo.app.i18n.UiLanguage
import com.taqvo.app.notifications.AyahReminderText
import com.taqvo.app.quran.QuranSource
import com.taqvo.app.quran.ReadingSettings
import com.taqvo.app.widget.AyahPool

/**
 * The words the daily ayah reminders can carry, read from the bundled Quran at schedule time:
 * the pool ayahs of the reader's themes ([AyahThemes.refsFor]), each as [format] shows it, with
 * the reader's own [reading] translation. The title is the reference, in the interface
 * language's surah naming and digits.
 */
class AyahReminderSource(private val quran: QuranSource) {

    suspend fun candidates(
        themeIds: List<String>,
        format: AyahFormat,
        reading: ReadingSettings,
        platformFormat: PlatformFormat,
    ): List<AyahReminderText> {
        val arabicUi = UiLanguage.of(platformFormat.languageTag()) == UiLanguage.ARABIC
        val surahs = quran.surahs().associateBy { it.number }
        val arabicByRef = mutableMapOf<Pair<Int, Int>, String?>()
        suspend fun arabic(ref: Pair<Int, Int>): String? =
            arabicByRef.getOrPut(ref) { quran.ayahText(ref.first, ref.second) }
        // Every pool ayah's Arabic is read up front: "short" is measured on it.
        AyahPool.REFS.forEach { arabic(it) }
        val refs = AyahThemes.refsFor(themeIds) { arabicByRef[it] }
        val translationOn = reading.translationId != ReadingSettings.NO_TRANSLATION
        return refs.mapNotNull { ref ->
            val (surah, ayah) = ref
            val arabicText = arabicByRef[ref] ?: return@mapNotNull null
            val translation = if (translationOn) quran.translationText(reading.translationId, surah, ayah)?.takeIf { it.isNotBlank() } else null
            val body = when {
                format == AyahFormat.ARABIC || translation == null -> arabicText
                format == AyahFormat.TRANSLATION -> translation
                else -> "$arabicText\n$translation"
            }
            val info = surahs[surah]
            val name = if (arabicUi) info?.nameArabic else info?.nameLatin
            val number = "${platformFormat.localizedDigits(surah)}:${platformFormat.localizedDigits(ayah)}"
            AyahReminderText(surah, ayah, title = listOfNotNull(name, number).joinToString(" · "), body = body)
        }
    }
}

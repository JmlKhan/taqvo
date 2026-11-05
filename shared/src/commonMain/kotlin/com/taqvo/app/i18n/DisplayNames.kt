package com.taqvo.app.i18n

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.manropeFamily
import com.taqvo.app.domain.AdhanVoice
import com.taqvo.app.domain.HighLatitudePreference
import com.taqvo.app.domain.Prayer
import com.taqvo.app.domain.PrayerSound
import com.taqvo.app.feature.common.authorityShortName
import com.taqvo.app.prayer.engine.registry.Registry
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.adhan_voice_azeez
import com.taqvo.app.resources.adhan_voice_azemi
import com.taqvo.app.resources.adhan_voice_custom
import com.taqvo.app.resources.adhan_voice_madinah
import com.taqvo.app.resources.adhan_voice_makkah
import com.taqvo.app.resources.adhan_voice_original
import com.taqvo.app.resources.asr_automatic
import com.taqvo.app.resources.high_lat_automatic
import com.taqvo.app.resources.high_lat_middle
import com.taqvo.app.resources.high_lat_seventh
import com.taqvo.app.resources.high_lat_twilight
import com.taqvo.app.resources.madhab_hanafi
import com.taqvo.app.resources.madhab_standard
import com.taqvo.app.resources.prayer_asr
import com.taqvo.app.resources.prayer_dhuhr
import com.taqvo.app.resources.prayer_fajr
import com.taqvo.app.resources.prayer_isha
import com.taqvo.app.resources.prayer_maghrib
import com.taqvo.app.resources.prayer_sunrise
import com.taqvo.app.resources.sound_adhan
import com.taqvo.app.resources.sound_notification
import com.taqvo.app.resources.sound_silent
import com.taqvo.app.resources.sound_takbir
import com.taqvo.app.resources.timetable_automatic
import com.taqvo.app.resources.ui_language

/**
 * The language the interface is actually in: the one whose strings Compose has resolved. Read
 * from the resources themselves ([Res.string.ui_language] is the language's own code in each
 * `values-*` file) rather than from the platform's locale tag, so this can never disagree with
 * the words on screen — a phone whose resource locale and default locale differ (a per-app
 * language, a regional variant the tag spells unexpectedly) used to get Arabic strings with the
 * English naming rule, and so showed every prayer name twice.
 */
@Composable
fun uiLanguage(): UiLanguage = UiLanguage.of(stringResource(Res.string.ui_language))

/** Right-to-left interface: Arabic and Urdu. Usable outside a mirrored subtree, unlike `LocalLayoutDirection`. */
@Composable
fun isRtlLocale(): Boolean = uiLanguage().rtl

/** Latin-script interface: English, French, Turkish, Indonesian. Manrope and tracking apply. */
@Composable
fun isLatinScriptUi(): Boolean = uiLanguage().latinScript

/**
 * Manrope ships Latin glyphs only, so Arabic, Urdu and Bengali copy must come from the OS face —
 * SF on iOS, the OEM font on Android (spec §3). This is the one decision every text style in the
 * app defers to, which is why it is applied once in the theme rather than per `Text`.
 */
@Composable
fun uiFontFamily(): FontFamily = if (isLatinScriptUi()) manropeFamily() else FontFamily.Default

/** The prayer's name in the UI language alone — the pairing rule lives in [PrayerNaming]. */
@Composable
fun localizedPrayerName(prayer: Prayer): String = stringResource(
    when (prayer) {
        Prayer.FAJR -> Res.string.prayer_fajr
        Prayer.SUNRISE -> Res.string.prayer_sunrise
        Prayer.DHUHR -> Res.string.prayer_dhuhr
        Prayer.ASR -> Res.string.prayer_asr
        Prayer.MAGHRIB -> Res.string.prayer_maghrib
        Prayer.ISHA -> Res.string.prayer_isha
    },
)

/**
 * The name of a stored timetable choice ([com.taqvo.app.domain.PrayerSettings.timetable]):
 * "Automatic", or the registry entry's short name; an id the registry no longer has reads as itself.
 */
@Composable
fun timetableDisplayName(timetable: String): String = when (timetable) {
    AUTOMATIC_TIMETABLE -> stringResource(Res.string.timetable_automatic)
    else -> Registry.byId(timetable)?.let { authorityShortName(it.shortNameKey) } ?: timetable
}

private const val AUTOMATIC_TIMETABLE = "automatic"

@Composable
fun highLatitudeDisplayName(preference: HighLatitudePreference): String = stringResource(
    when (preference) {
        HighLatitudePreference.AUTOMATIC -> Res.string.high_lat_automatic
        HighLatitudePreference.MIDDLE_OF_NIGHT -> Res.string.high_lat_middle
        HighLatitudePreference.SEVENTH_OF_NIGHT -> Res.string.high_lat_seventh
        HighLatitudePreference.TWILIGHT_ANGLE -> Res.string.high_lat_twilight
    },
)

/** The name of a stored Asr school ([com.taqvo.app.domain.PrayerSettings.school]). */
@Composable
fun schoolDisplayName(school: String): String = stringResource(
    when (school) {
        "standard" -> Res.string.madhab_standard
        "hanafi" -> Res.string.madhab_hanafi
        else -> Res.string.asr_automatic
    },
)

@Composable
fun soundDisplayName(sound: PrayerSound): String = stringResource(
    when (sound) {
        PrayerSound.SILENT -> Res.string.sound_silent
        PrayerSound.NOTIFICATION -> Res.string.sound_notification
        PrayerSound.TAKBIR -> Res.string.sound_takbir
        PrayerSound.ADHAN -> Res.string.sound_adhan
    },
)

/**
 * The voice's name. A reciter's name is a name in either language — "Aaqib Azeez" is عاقب عزيز,
 * not a translation of it — so both sets are transliterations of the same person rather than
 * different words, and "Original" is the one entry that is genuinely translated.
 */
@Composable
fun adhanVoiceDisplayName(voice: AdhanVoice): String = stringResource(
    when (voice) {
        AdhanVoice.ORIGINAL -> Res.string.adhan_voice_original
        AdhanVoice.AZEEZ -> Res.string.adhan_voice_azeez
        AdhanVoice.AZEMI -> Res.string.adhan_voice_azemi
        AdhanVoice.MAKKAH -> Res.string.adhan_voice_makkah
        AdhanVoice.MADINAH -> Res.string.adhan_voice_madinah
        AdhanVoice.CUSTOM -> Res.string.adhan_voice_custom
    },
)

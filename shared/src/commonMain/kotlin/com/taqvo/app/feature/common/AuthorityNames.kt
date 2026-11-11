package com.taqvo.app.feature.common

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.authority_awqaf_jordan
import com.taqvo.app.resources.authority_awqaf_kuwait
import com.taqvo.app.resources.authority_awqaf_libya
import com.taqvo.app.resources.authority_awqaf_syria
import com.taqvo.app.resources.authority_awqaf_uae
import com.taqvo.app.resources.authority_bahrain_council
import com.taqvo.app.resources.authority_bik
import com.taqvo.app.resources.authority_cape_calendar
import com.taqvo.app.resources.authority_chicago
import com.taqvo.app.resources.authority_dar_al_fatwa
import com.taqvo.app.resources.authority_diyanet
import com.taqvo.app.resources.authority_diyanet_europe
import com.taqvo.app.resources.authority_dum_rd
import com.taqvo.app.resources.authority_dum_rf
import com.taqvo.app.resources.authority_dum_rt
import com.taqvo.app.resources.authority_emb
import com.taqvo.app.resources.authority_esa
import com.taqvo.app.resources.authority_fazilet
import com.taqvo.app.resources.authority_fianz
import com.taqvo.app.resources.authority_fids
import com.taqvo.app.resources.authority_gaza_awqaf
import com.taqvo.app.resources.authority_grande_mosquee_paris
import com.taqvo.app.resources.authority_habous
import com.taqvo.app.resources.authority_iacad
import com.taqvo.app.resources.authority_icci
import com.taqvo.app.resources.authority_ifb
import com.taqvo.app.resources.authority_ifi
import com.taqvo.app.resources.authority_ift
import com.taqvo.app.resources.authority_iggo
import com.taqvo.app.resources.authority_iit
import com.taqvo.app.resources.authority_inm
import com.taqvo.app.resources.authority_irn
import com.taqvo.app.resources.authority_isna
import com.taqvo.app.resources.authority_iz_bih
import com.taqvo.app.resources.authority_izcg
import com.taqvo.app.resources.authority_jakim
import com.taqvo.app.resources.authority_jamiat
import com.taqvo.app.resources.authority_karachi
import com.taqvo.app.resources.authority_kemenag
import com.taqvo.app.resources.authority_kmsh
import com.taqvo.app.resources.authority_kyrgyz_muftiate
import com.taqvo.app.resources.authority_lakemba
import com.taqvo.app.resources.authority_london_unified
import com.taqvo.app.resources.authority_mac_toronto
import com.taqvo.app.resources.authority_marw
import com.taqvo.app.resources.authority_mauritania
import com.taqvo.app.resources.authority_mjc
import com.taqvo.app.resources.authority_mora_brunei
import com.taqvo.app.resources.authority_muhammadiyah
import com.taqvo.app.resources.authority_muis
import com.taqvo.app.resources.authority_oman_awqaf
import com.taqvo.app.resources.authority_pa_iftaa
import com.taqvo.app.resources.authority_qatar_calendar
import com.taqvo.app.resources.authority_qmdb
import com.taqvo.app.resources.authority_sudan_fiqh
import com.taqvo.app.resources.authority_sunni_endowment
import com.taqvo.app.resources.authority_umm_al_qura
import com.taqvo.app.resources.authority_uzbek_board
import com.taqvo.app.resources.authority_wifaqul_ulama
import com.taqvo.app.resources.method_dubai
import com.taqvo.app.resources.method_egyptian
import com.taqvo.app.resources.method_isna
import com.taqvo.app.resources.method_karachi
import com.taqvo.app.resources.method_kuwait
import com.taqvo.app.resources.method_moonsighting
import com.taqvo.app.resources.method_muslim_world_league
import com.taqvo.app.resources.method_qatar
import com.taqvo.app.resources.method_singapore
import com.taqvo.app.resources.method_turkey
import com.taqvo.app.resources.method_umm_al_qura
import com.taqvo.app.resources.timetable_calculated
import com.taqvo.app.resources.timetable_cautious
import com.taqvo.app.resources.timetable_chicago_eighteen
import com.taqvo.app.resources.timetable_eighteen_degrees
import com.taqvo.app.resources.timetable_fifteen_degrees
import com.taqvo.app.resources.timetable_isha_ninety
import com.taqvo.app.resources.timetable_late_dawn
import com.taqvo.app.resources.timetable_moroccan_calendar
import com.taqvo.app.resources.timetable_rabita
import com.taqvo.app.resources.timetable_twelve_degrees

/**
 * The short name of a timetable the registry names, in the interface's language: an entry's
 * `shortNameKey` ("Diyanet", "London Unified", "Cautious times", "Calculated by Tazkir") or a
 * cautious member's `nameKey` ("Islamic Foundation"). The registry holds keys, not words, because
 * it is plain Kotlin that the website's generator compiles too.
 *
 * A key with no string falls back to the key itself, visibly wrong rather than a crash;
 * `AuthorityNamesTest` holds every key the registry uses to a string.
 */
@Composable
fun authorityShortName(key: String): String = authorityNameRes(key)?.let { stringResource(it) } ?: key

/**
 * The string behind [key], or null for a key the registry does not use. The Other methods reuse
 * the old picker's `method_*` names; every other key is in the authority-names block of
 * `strings.xml`.
 */
fun authorityNameRes(key: String): StringResource? = when (key) {
    "authority_awqaf_jordan" -> Res.string.authority_awqaf_jordan
    "authority_awqaf_kuwait" -> Res.string.authority_awqaf_kuwait
    "authority_awqaf_libya" -> Res.string.authority_awqaf_libya
    "authority_awqaf_syria" -> Res.string.authority_awqaf_syria
    "authority_awqaf_uae" -> Res.string.authority_awqaf_uae
    "authority_bahrain_council" -> Res.string.authority_bahrain_council
    "authority_bik" -> Res.string.authority_bik
    "authority_cape_calendar" -> Res.string.authority_cape_calendar
    "authority_chicago" -> Res.string.authority_chicago
    "authority_dar_al_fatwa" -> Res.string.authority_dar_al_fatwa
    "authority_diyanet" -> Res.string.authority_diyanet
    "authority_diyanet_europe" -> Res.string.authority_diyanet_europe
    "authority_dum_rd" -> Res.string.authority_dum_rd
    "authority_dum_rf" -> Res.string.authority_dum_rf
    "authority_dum_rt" -> Res.string.authority_dum_rt
    "authority_emb" -> Res.string.authority_emb
    "authority_esa" -> Res.string.authority_esa
    "authority_fazilet" -> Res.string.authority_fazilet
    "authority_fianz" -> Res.string.authority_fianz
    "authority_fids" -> Res.string.authority_fids
    "authority_gaza_awqaf" -> Res.string.authority_gaza_awqaf
    "authority_grande_mosquee_paris" -> Res.string.authority_grande_mosquee_paris
    "authority_habous" -> Res.string.authority_habous
    "authority_iacad" -> Res.string.authority_iacad
    "authority_icci" -> Res.string.authority_icci
    "authority_ifb" -> Res.string.authority_ifb
    "authority_ifi" -> Res.string.authority_ifi
    "authority_ift" -> Res.string.authority_ift
    "authority_iggo" -> Res.string.authority_iggo
    "authority_iit" -> Res.string.authority_iit
    "authority_inm" -> Res.string.authority_inm
    "authority_irn" -> Res.string.authority_irn
    "authority_isna" -> Res.string.authority_isna
    "authority_iz_bih" -> Res.string.authority_iz_bih
    "authority_izcg" -> Res.string.authority_izcg
    "authority_jakim" -> Res.string.authority_jakim
    "authority_jamiat" -> Res.string.authority_jamiat
    "authority_karachi" -> Res.string.authority_karachi
    "authority_kemenag" -> Res.string.authority_kemenag
    "authority_kmsh" -> Res.string.authority_kmsh
    "authority_kyrgyz_muftiate" -> Res.string.authority_kyrgyz_muftiate
    "authority_lakemba" -> Res.string.authority_lakemba
    "authority_london_unified" -> Res.string.authority_london_unified
    "authority_mac_toronto" -> Res.string.authority_mac_toronto
    "authority_marw" -> Res.string.authority_marw
    "authority_mauritania" -> Res.string.authority_mauritania
    "authority_mjc" -> Res.string.authority_mjc
    "authority_mora_brunei" -> Res.string.authority_mora_brunei
    "authority_muhammadiyah" -> Res.string.authority_muhammadiyah
    "authority_muis" -> Res.string.authority_muis
    "authority_oman_awqaf" -> Res.string.authority_oman_awqaf
    "authority_pa_iftaa" -> Res.string.authority_pa_iftaa
    "authority_qatar_calendar" -> Res.string.authority_qatar_calendar
    "authority_qmdb" -> Res.string.authority_qmdb
    "authority_sudan_fiqh" -> Res.string.authority_sudan_fiqh
    "authority_sunni_endowment" -> Res.string.authority_sunni_endowment
    "authority_umm_al_qura" -> Res.string.authority_umm_al_qura
    "authority_uzbek_board" -> Res.string.authority_uzbek_board
    "authority_wifaqul_ulama" -> Res.string.authority_wifaqul_ulama
    "timetable_cautious" -> Res.string.timetable_cautious
    "timetable_calculated" -> Res.string.timetable_calculated
    "timetable_twelve_degrees" -> Res.string.timetable_twelve_degrees
    "timetable_late_dawn" -> Res.string.timetable_late_dawn
    "timetable_moroccan_calendar" -> Res.string.timetable_moroccan_calendar
    "timetable_isha_ninety" -> Res.string.timetable_isha_ninety
    "timetable_fifteen_degrees" -> Res.string.timetable_fifteen_degrees
    "timetable_eighteen_degrees" -> Res.string.timetable_eighteen_degrees
    "timetable_chicago_eighteen" -> Res.string.timetable_chicago_eighteen
    "timetable_rabita" -> Res.string.timetable_rabita
    "method_muslim_world_league" -> Res.string.method_muslim_world_league
    "method_isna" -> Res.string.method_isna
    "method_egyptian" -> Res.string.method_egyptian
    "method_umm_al_qura" -> Res.string.method_umm_al_qura
    "method_karachi" -> Res.string.method_karachi
    "method_moonsighting" -> Res.string.method_moonsighting
    "method_turkey" -> Res.string.method_turkey
    "method_kuwait" -> Res.string.method_kuwait
    "method_qatar" -> Res.string.method_qatar
    "method_dubai" -> Res.string.method_dubai
    "method_singapore" -> Res.string.method_singapore
    else -> null
}

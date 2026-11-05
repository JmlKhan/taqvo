package com.taqvo.app.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.LocalTaqvoColors
import com.taqvo.app.design.TaqvoText
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.drawExternalLink
import com.taqvo.app.quran.TextKind
import com.taqvo.app.quran.TranslationInfo
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.attribution_intro
import com.taqvo.app.resources.credit_adhkar
import com.taqvo.app.resources.credit_adhkar_detail
import com.taqvo.app.resources.credit_audio
import com.taqvo.app.resources.credit_audio_detail
import com.taqvo.app.resources.credit_chime
import com.taqvo.app.resources.credit_chime_detail
import com.taqvo.app.resources.credit_calculation
import com.taqvo.app.resources.credit_calculation_detail
import com.taqvo.app.resources.credit_cities
import com.taqvo.app.resources.credit_cities_detail
import com.taqvo.app.resources.credit_quran_font
import com.taqvo.app.resources.credit_quran_font_detail
import com.taqvo.app.resources.credit_quran_layout
import com.taqvo.app.resources.credit_quran_layout_detail
import com.taqvo.app.resources.credit_quran_text
import com.taqvo.app.i18n.uiLanguage
import com.taqvo.app.recitation.Reciter
import com.taqvo.app.resources.credit_quran_text_detail
import com.taqvo.app.resources.credit_recitations
import com.taqvo.app.resources.credit_recitations_detail
import com.taqvo.app.resources.credit_translations
import com.taqvo.app.resources.credit_translations_detail
import com.taqvo.app.resources.credit_typeface
import com.taqvo.app.resources.credit_typeface_detail
import com.taqvo.app.resources.quran_sheet_transliteration
import com.taqvo.app.resources.settings_attribution

private data class Credit(
    val what: String,
    val detail: String,
    /** Where the work comes from, shown bare; tapping the credit opens it (see [sourceUrl]). */
    val source: String? = null,
    /** A second paragraph under the detail, in the same tint: the ten reciters, named. */
    val extra: String? = null,
)

/** A source is shown bare ("tanzil.net"); the link it opens needs the scheme. */
internal fun sourceUrl(source: String): String = "https://$source"

/**
 * The contents of `docs/ATTRIBUTION.md`. GeoNames' CC BY 4.0 makes this screen an obligation.
 * The source lines stay literal: a domain name is the same in every language, and translating
 * one would break the very reference it exists to give.
 */
@Composable
private fun credits(translations: List<TranslationInfo>, reciters: List<Reciter>): List<Credit> = listOf(
    Credit(
        what = stringResource(Res.string.credit_calculation),
        detail = stringResource(Res.string.credit_calculation_detail),
        source = "github.com/batoulapps/adhan-kotlin",
    ),
    Credit(
        what = stringResource(Res.string.credit_cities),
        detail = stringResource(Res.string.credit_cities_detail),
        source = "geonames.org",
    ),
    Credit(
        what = stringResource(Res.string.credit_typeface),
        detail = stringResource(Res.string.credit_typeface_detail),
    ),
    Credit(
        what = stringResource(Res.string.credit_audio),
        detail = stringResource(Res.string.credit_audio_detail),
    ),
    Credit(
        what = stringResource(Res.string.credit_chime),
        detail = stringResource(Res.string.credit_chime_detail),
        source = "mixkit.co",
    ),
    Credit(
        what = stringResource(Res.string.credit_quran_text),
        detail = stringResource(Res.string.credit_quran_text_detail),
        source = "tanzil.net",
    ),
    Credit(
        what = stringResource(Res.string.credit_quran_font),
        detail = stringResource(Res.string.credit_quran_font_detail),
        source = "qurancomplex.gov.sa",
    ),
    Credit(
        what = stringResource(Res.string.credit_quran_layout),
        detail = stringResource(Res.string.credit_quran_layout_detail),
        source = "github.com/zonetecde/mushaf-layout",
    ),
    Credit(
        what = stringResource(Res.string.credit_translations),
        detail = stringResource(Res.string.credit_translations_detail),
        source = "tanzil.net/trans",
    ),
    Credit(
        what = stringResource(Res.string.credit_recitations),
        detail = stringResource(Res.string.credit_recitations_detail),
        source = "alquran.cloud",
        // Read from the catalogue in force, never written down here, for the same reason the
        // translations below are read from the database: a reciter withdrawn from the manifest
        // (spec §2's one obligation we can be held to) disappears from the app *and* from its
        // credits, with no app update and no line left behind naming someone we no longer carry.
        extra = uiLanguage().arabicScript.let { arabic ->
            reciters.joinToString(if (arabic) "، " else ", ") { if (arabic) it.nameAr else it.nameEn }
        }.takeIf { it.isNotBlank() },
    ),
    // The morning and evening adhkar (adhkar spec §4.2): Hisn al-Muslim through the OpenDua
    // dataset, whose CC BY 4.0 asks for exactly this — the collection, the dataset and its licence.
    Credit(
        what = stringResource(Res.string.credit_adhkar),
        detail = stringResource(Res.string.credit_adhkar_detail),
        source = "opendua.org",
    ),
) + translations.map { translation ->
    // Read from the database, never hardcoded, so this list can never drift from what is
    // actually bundled (spec §6).
    // A translation's name is its own title in its own language and stays as stored; the
    // transliteration's stored name is the English word for what it is, so that one row takes the
    // localized label the reading sheet already uses instead.
    val what = if (translation.kind == TextKind.TRANSLITERATION) {
        stringResource(Res.string.quran_sheet_transliteration)
    } else {
        translation.name
    }
    Credit(what = what, detail = translation.translator)
}

@Composable
fun AttributionScreen(
    translations: List<TranslationInfo>,
    reciters: List<Reciter>,
    onBack: () -> Unit,
) {
    val colors = LocalTaqvoColors.current
    val uriHandler = LocalUriHandler.current
    val forward = LocalLayoutDirection.current == LayoutDirection.Ltr
    val credits = credits(translations, reciters)
    // A device with no browser fails silently rather than crashing, as on the About screen.
    fun open(url: String) {
        runCatching { uriHandler.openUri(url) }
    }
    SettingsScaffold(stringResource(Res.string.settings_attribution), onBack) {
        SettingsNote(stringResource(Res.string.attribution_intro))
        Spacer(Modifier.height(16.dp))
        SettingsCard {
            credits.forEachIndexed { i, credit ->
                if (i > 0) CardDivider()
                // A credit with a source leaves the app for it, as About's links do: the whole credit
                // is the target, and the external-link glyph by its name says so before the tap.
                // Tanzil's terms ask for this link, so readers can follow changes to the text.
                val source = credit.source
                val click = if (source != null) Modifier.clickable { open(sourceUrl(source)) } else Modifier
                Column(Modifier.fillMaxWidth().then(click).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(credit.what, style = TaqvoText.rowLabel, color = colors.textPrimary, modifier = Modifier.weight(1f))
                        if (source != null) {
                            Spacer(Modifier.width(12.dp))
                            Canvas(Modifier.size(16.dp)) { drawExternalLink(colors.textTertiary, forward) }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(credit.detail, style = TaqvoText.caption, color = colors.textSecondary)
                    if (credit.extra != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(credit.extra, style = TaqvoText.caption, color = colors.textSecondary)
                    }
                    if (source != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(source, style = TaqvoText.caption, color = colors.textTertiary)
                    }
                }
            }
        }
    }
}

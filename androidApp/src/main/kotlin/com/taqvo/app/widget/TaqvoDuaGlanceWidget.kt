package com.taqvo.app.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.taqvo.app.R
import com.taqvo.app.adhkar.AdhkarCatalogue
import com.taqvo.app.domain.WidgetBackground
import com.taqvo.app.i18n.UiLanguage
import com.taqvo.app.i18n.createPlatformFormat
import java.time.LocalDate
import java.util.Locale

/**
 * The daily-dua home-screen widget, the ayah card's companion: one of the adhkar's supplications
 * said at any hour, its translation in the interface language, and its number in Hisn al-Muslim.
 *
 * Unlike the ayah card this one is plain Glance text, no bitmap: a supplication is set in the OS
 * face (the adhkar page's own rule), which Glance draws itself, and only the Quran needs the
 * bundled Hafs face Glance cannot load. It turns over at midnight on the ayah card's own alarm
 * ([AyahWidgetScheduler]) and redraws on the ayah card's paths ([TaqvoWidgets.updateAyah]): the
 * two daily cards change on exactly the same occasions.
 */
class TaqvoDuaGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ensureDuaMirror()
        // Read inside the lambda and keyed on the revision, as the ayah card is (D1, WidgetRedraw).
        provideContent { DuaWidgetContent(readDuaRender(context, WidgetRedraw.ayahRevision)) }
    }
}

private class DuaWidgetRender(
    val entry: DuaPoolEntry?,
    val languageTag: String,
    val arabicIndicDigits: Boolean,
    val colors: WidgetPaletteColors,
)

/**
 * Writes the dua mirror when there is none — a widget placed before the app was first opened —
 * from the device's language. Best-effort: a failure leaves the card empty until the app runs.
 */
private suspend fun ensureDuaMirror() {
    val store = createWidgetKeyValueStore()
    if (DuaPoolMirror.read(store) != null) return
    val deviceTag = Locale.getDefault().toLanguageTag()
    runCatching {
        DuaPoolMirrorWriter.write(
            store,
            AdhkarCatalogue.load(),
            AdhkarCatalogue.loadText(UiLanguage.of(deviceTag).code),
            deviceTag,
            createPlatformFormat(),
        )
    }
}

private fun readDuaRender(
    context: Context,
    @Suppress("UNUSED_PARAMETER") redrawRevision: Int,
): DuaWidgetRender {
    val store = createWidgetKeyValueStore()
    val mirror = DuaPoolMirror.read(store)
    val today = LocalDate.now()
    val entry = mirror?.entryFor(
        AyahRotation.epochDay(today.year, today.monthValue, today.dayOfMonth),
        AyahPoolMirror.seed(store) ?: 0L,
    )
    val background = store.getString("widget_background")
        ?.let { raw -> WidgetBackground.entries.firstOrNull { it.name == raw } }
        ?: WidgetBackground.FOLLOW_THEME
    val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return DuaWidgetRender(
        entry = entry,
        languageTag = mirror?.languageTag ?: Locale.getDefault().toLanguageTag(),
        arabicIndicDigits = mirror?.arabicIndicDigits == true,
        colors = WidgetPalette.colorsFor(background, nightMode == Configuration.UI_MODE_NIGHT_YES),
    )
}

@Composable
private fun DuaWidgetContent(render: DuaWidgetRender) {
    val context = LocalContext.current
    val size = LocalSize.current
    val entry = render.entry
    WidgetCard(render.colors) {
        if (entry == null) return@WidgetCard
        val ink = Color(render.colors.textArgb)
        // A short cell keeps the Arabic and its reference; the translation needs the height.
        val roomy = size.height >= 150.dp
        val reference = context.getString(
            R.string.widget_dua_reference,
            WidgetDigits.localize(entry.hisn.toString(), render.arabicIndicDigits, render.languageTag),
        )
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.arabic,
                style = TextStyle(
                    color = ColorProvider(ink),
                    fontSize = arabicSize(entry.arabic.length, roomy),
                    textAlign = TextAlign.Center,
                ),
                maxLines = if (roomy) 6 else 3,
            )
            if (roomy && entry.translation.isNotEmpty()) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = entry.translation,
                    style = TextStyle(
                        color = ColorProvider(ink.copy(alpha = 0.66f)),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 4,
                )
            }
            Spacer(GlanceModifier.height(8.dp))
            Text(
                text = reference,
                style = TextStyle(
                    color = ColorProvider(Color(render.colors.accentArgb)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

/** The pool runs from thirty characters to three hundred; the longest step down so they stay whole. */
private fun arabicSize(length: Int, roomy: Boolean): TextUnit = when {
    length <= 60 -> if (roomy) 24.sp else 20.sp
    length <= 130 -> if (roomy) 19.sp else 16.sp
    else -> if (roomy) 15.sp else 13.sp
}

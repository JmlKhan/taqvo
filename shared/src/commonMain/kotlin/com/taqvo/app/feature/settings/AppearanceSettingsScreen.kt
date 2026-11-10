package com.taqvo.app.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.ThemeMode
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.CheckMark
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.i18n.AppLanguage
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.appearance_language_label
import com.taqvo.app.resources.appearance_note
import com.taqvo.app.resources.appearance_theme_label
import com.taqvo.app.resources.settings_appearance
import com.taqvo.app.resources.theme_dark
import com.taqvo.app.resources.theme_light
import com.taqvo.app.resources.theme_system

@Composable
internal fun themeDisplayName(mode: ThemeMode): String = stringResource(
    when (mode) {
        ThemeMode.SYSTEM -> Res.string.theme_system
        ThemeMode.LIGHT -> Res.string.theme_light
        ThemeMode.DARK -> Res.string.theme_dark
    },
)

/**
 * The app's theme and its language (chosen here, not taken from the phone): the widgets, their background and the ways to add them have
 * their own screen, Settings › Widgets, where a reader who skipped onboarding's widget step can
 * find them again.
 */
@Composable
fun AppearanceSettingsScreen(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    currentLanguage: AppLanguage = AppLanguage.DEFAULT,
    onPickLanguage: (AppLanguage) -> Unit = {},
) {
    SettingsScaffold(stringResource(Res.string.settings_appearance), onBack) {
        SectionLabel(stringResource(Res.string.appearance_theme_label))
        SettingsCard(Modifier.selectableGroup()) {
            ThemeMode.entries.forEachIndexed { i, mode ->
                if (i > 0) CardDivider()
                TaqvoRow(
                    label = themeDisplayName(mode),
                    onClick = { onPick(mode) },
                    selectable = true,
                    selected = mode == current,
                    trailing = { if (mode == current) CheckMark() },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        SettingsNote(stringResource(Res.string.appearance_note))

        // The language is the app's own choice, kept in Settings and applied at once. Each is
        // named in itself, so a reader who cannot read the current one can still find theirs.
        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(Res.string.appearance_language_label))
        SettingsCard(Modifier.selectableGroup()) {
            AppLanguage.entries.forEachIndexed { i, language ->
                if (i > 0) CardDivider()
                TaqvoRow(
                    label = language.endonym,
                    onClick = { onPickLanguage(language) },
                    selectable = true,
                    selected = language == currentLanguage,
                    trailing = { if (language == currentLanguage) CheckMark() },
                )
            }
        }
    }
}

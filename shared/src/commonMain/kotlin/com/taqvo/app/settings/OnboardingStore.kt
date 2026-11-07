package com.taqvo.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.ayah.AyahReminderSettings
import com.taqvo.app.feature.onboarding.OnboardingAnswers
import com.taqvo.app.wallpaper.WallpaperStyle
import kotlin.random.Random

/**
 * Onboarding's answers and the settings it creates (spec §3), in the app's DataStore beside
 * [TasbeehStore]. Every answer is written the moment it is given, and the step the reader is on
 * is kept too, so a reader whose app is killed half way resumes where they were (spec §0).
 *
 * The lists are stored as one comma-joined string each: the ids are fixed ASCII words with no
 * commas, and a single key per list keeps a cleared answer from leaving stale keys behind.
 */
class OnboardingStore(private val store: DataStore<Preferences>) {

    val answers: Flow<OnboardingAnswers> = store.data.map { prefs ->
        OnboardingAnswers(
            name = prefs[NAME],
            goals = prefs[GOALS].toIds(),
            contentTypes = prefs[CONTENT_TYPES].toIds(),
            useCases = prefs[USE_CASES].toIds(),
        )
    }

    /** The step to resume at, by name; null before onboarding has begun. */
    val step: Flow<String?> = store.data.map { it[STEP] }

    val ayahReminders: Flow<AyahReminderSettings> = store.data.map { prefs ->
        AyahReminderSettings(
            enabled = prefs[REMINDERS_ON] ?: false,
            count = prefs[REMINDER_COUNT] ?: AyahReminderSettings.DEFAULT_COUNT,
            startMinute = prefs[REMINDER_START] ?: AyahReminderSettings.DEFAULT_START,
            endMinute = prefs[REMINDER_END] ?: AyahReminderSettings.DEFAULT_END,
            denied = prefs[REMINDERS_DENIED] ?: false,
        )
    }

    val ayahFormat: Flow<AyahFormat> = store.data.map { prefs ->
        prefs[AYAH_FORMAT]?.let { name -> AyahFormat.entries.firstOrNull { it.name == name } }
            ?: AyahFormat.ARABIC_TRANSLATION
    }

    /** The wallpaper look chosen on screen 11; Night until one is chosen. */
    val wallpaperStyle: Flow<WallpaperStyle> = store.data.map { prefs ->
        prefs[WALLPAPER_STYLE]?.let { name -> WallpaperStyle.entries.firstOrNull { it.name == name } }
            ?: WallpaperStyle.NIGHT
    }

    /** The day the streak began (screen 07); null until it has. */
    val streakStart: Flow<LocalDate?> = store.data.map { prefs ->
        prefs[STREAK_START]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    suspend fun setStep(step: String) = store.edit { it[STEP] = step }

    suspend fun setName(name: String?) = store.edit { prefs ->
        if (name == null) prefs.remove(NAME) else prefs[NAME] = name
    }

    suspend fun setGoals(ids: List<String>) = store.edit { it[GOALS] = ids.joinToString(SEPARATOR) }

    suspend fun setContentTypes(ids: List<String>) = store.edit { it[CONTENT_TYPES] = ids.joinToString(SEPARATOR) }

    suspend fun setUseCases(ids: List<String>) = store.edit { it[USE_CASES] = ids.joinToString(SEPARATOR) }

    suspend fun setAyahReminders(settings: AyahReminderSettings) = store.edit { prefs ->
        prefs[REMINDERS_ON] = settings.enabled
        prefs[REMINDER_COUNT] = settings.count
        prefs[REMINDER_START] = settings.startMinute
        prefs[REMINDER_END] = settings.endMinute
        prefs[REMINDERS_DENIED] = settings.denied
    }

    /**
     * The system's answer to the notification prompt, written over whatever count and window are
     * stored at that moment: a callback that kept an older copy of the settings cannot undo them.
     */
    suspend fun setAyahRemindersAllowed(granted: Boolean) = store.edit { prefs ->
        prefs[REMINDERS_ON] = granted
        prefs[REMINDERS_DENIED] = !granted
    }

    suspend fun setAyahFormat(format: AyahFormat) = store.edit { it[AYAH_FORMAT] = format.name }

    suspend fun setWallpaperStyle(style: WallpaperStyle) = store.edit { it[WALLPAPER_STYLE] = style.name }

    /** Starts the streak on [today] unless it has already begun. */
    suspend fun startStreak(today: LocalDate) = store.edit { prefs ->
        if (prefs[STREAK_START] == null) prefs[STREAK_START] = today.toString()
    }

    /**
     * The per-install seed that fixes which ayah each reminder carries on a given day, made the
     * first time it is asked for.
     */
    suspend fun reminderSeed(): Long {
        store.data.first()[REMINDER_SEED]?.let { return it }
        var seed = 0L
        store.edit { prefs ->
            seed = prefs[REMINDER_SEED] ?: Random.nextLong().also { prefs[REMINDER_SEED] = it }
        }
        return seed
    }

    private fun String?.toIds(): List<String> =
        this?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val SEPARATOR = ","
        val STEP = stringPreferencesKey("onboarding_step")
        val NAME = stringPreferencesKey("onboarding_name")
        val GOALS = stringPreferencesKey("onboarding_goals")
        val CONTENT_TYPES = stringPreferencesKey("onboarding_content_types")
        val USE_CASES = stringPreferencesKey("onboarding_use_cases")
        val REMINDERS_ON = booleanPreferencesKey("ayah_reminders_on")
        val REMINDERS_DENIED = booleanPreferencesKey("ayah_reminders_denied")
        val REMINDER_COUNT = intPreferencesKey("ayah_reminder_count")
        val REMINDER_START = intPreferencesKey("ayah_reminder_start")
        val REMINDER_END = intPreferencesKey("ayah_reminder_end")
        val REMINDER_SEED = longPreferencesKey("ayah_reminder_seed")
        val AYAH_FORMAT = stringPreferencesKey("ayah_format")
        val WALLPAPER_STYLE = stringPreferencesKey("onboarding_wallpaper_style")
        val STREAK_START = stringPreferencesKey("streak_start")
    }
}

package com.taqvo.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.taqvo.app.analytics.OnboardingEvents
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.di.AppContainer
import com.taqvo.app.feature.onboarding.AyahPreviewText
import com.taqvo.app.feature.onboarding.OnboardingActions
import com.taqvo.app.feature.onboarding.OnboardingModel
import com.taqvo.app.feature.onboarding.OnboardingScreen
import com.taqvo.app.feature.onboarding.OnboardingStep
import com.taqvo.app.feature.onboarding.WallpaperSample
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.i18n.UiLanguage
import com.taqvo.app.location.LocationPermission
import com.taqvo.app.nav.Navigator
import com.taqvo.app.nav.Screen
import com.taqvo.app.notifications.NotificationOnboarding
import com.taqvo.app.notifications.RescheduleTrigger
import com.taqvo.app.notifications.requestExactAlarmAccess
import com.taqvo.app.quran.QuranText
import com.taqvo.app.quran.RTL_TRANSLATION_LANGUAGES
import com.taqvo.app.quran.ReadingSettings
import com.taqvo.app.settings.SettingsRepository
import com.taqvo.app.wallpaper.WallpaperSource
import com.taqvo.app.wallpaper.WallpaperStyle
import com.taqvo.app.wallpaper.WallpaperText
import com.taqvo.app.widget.WidgetAddPath
import com.taqvo.app.widget.widgetAddPath
import kotlin.time.Clock

/** The ayah the wallpaper screens show (spec, screen 12: "Read! In the name of your Lord"). */
private val WALLPAPER_AYAH = 96 to 1

/** The ayah the reminder previews show (spec, screen 09: "in the remembrance of Allah hearts find rest"). */
private val REMINDER_AYAH = 13 to 28

/** One sample ayah in the reader's translation, as the onboarding screens need it. */
private data class SampleAyah(val surah: Int, val ayah: Int, val surahName: String, val arabic: String, val translation: String?)

@Composable
internal fun OnboardingRoute(
    onboardingStepState: MutableState<OnboardingStep>,
    container: AppContainer,
    useGpsFix: () -> Unit,
    navigator: Navigator,
    scope: CoroutineScope,
    notificationOnboarding: (Boolean) -> NotificationOnboarding,
    settings: SettingsRepository,
    exactAlarmsAllowedState: State<Boolean>,
) {
    var step by onboardingStepState
    val exactAlarmsAllowed by exactAlarmsAllowedState
    val store = container.onboardingStore
    val analytics = container.analytics
    val format = LocalPlatformFormat.current
    val languageTag = format.languageTag()

    // Null until DataStore answers: a resumed onboarding must not decide which screens apply (the
    // widget guide, the exact-alarm step) from answers that have not been read yet.
    val storedAnswers by store.answers.collectAsState(null)
    val storedReminders by store.ayahReminders.collectAsState(null)
    val ayahFormat by store.ayahFormat.collectAsState(AyahFormat.ARABIC_TRANSLATION)
    val style by store.wallpaperStyle.collectAsState(WallpaperStyle.NIGHT)

    // The resume point (spec §0): written on every step, so a killed onboarding comes back here.
    LaunchedEffect(step) { store.setStep(step.name) }

    var samples by remember { mutableStateOf<Pair<SampleAyah, SampleAyah>?>(null) }
    var translationRtl by remember { mutableStateOf(false) }
    LaunchedEffect(languageTag) {
        val reading = settings.readingSettings(languageTag).first()
        translationRtl = reading.translationId.substringBefore('.') in RTL_TRANSLATION_LANGUAGES
        samples = runCatching { loadSample(container, reading, WALLPAPER_AYAH, languageTag) to loadSample(container, reading, REMINDER_AYAH, languageTag) }.getOrNull()
    }
    fun reference(sample: SampleAyah) =
        "${sample.surahName} ${format.localizedDigits(sample.surah)}:${format.localizedDigits(sample.ayah)}"

    val wallpaperSample = samples?.first?.let { sample ->
        WallpaperSample(
            WallpaperText(
                source = WallpaperSource.AYAH,
                arabic = QuranText.withMarker(sample.arabic, sample.ayah),
                translation = sample.translation,
                quran = true,
                surah = sample.surah,
                ayah = sample.ayah,
            ),
            reference(sample),
            translationRtl,
        )
    }
    val previewFor: (AyahFormat) -> AyahPreviewText? = { shown ->
        samples?.second?.let { sample ->
            val text = when {
                shown == AyahFormat.ARABIC || sample.translation == null -> sample.arabic
                shown == AyahFormat.TRANSLATION -> sample.translation
                else -> "${sample.arabic}\n${sample.translation}"
            }
            AyahPreviewText(reference(sample), text)
        }
    }

    fun track(event: String, properties: Map<String, Any?>) = analytics.track(event, properties)

    val answers = storedAnswers ?: return
    val reminders = storedReminders ?: return

    OnboardingScreen(
        step = step,
        onStep = { step = it },
        model = OnboardingModel(
            answers = answers,
            reminders = reminders,
            ayahFormat = ayahFormat,
            style = style,
            wallpaperSample = wallpaperSample,
            previewFor = previewFor,
            exactAlarmsNeeded = !exactAlarmsAllowed,
            ios = widgetAddPath != WidgetAddPath.ANDROID_PIN,
            today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.dayOfWeek,
        ),
        actions = OnboardingActions(
            setName = { scope.launch { store.setName(it) } },
            setGoals = { ids -> scope.launch { store.setGoals(ids) }; track(OnboardingEvents.ANSWER, mapOf("question" to "goals", "ids" to ids)) },
            setContentTypes = { ids -> scope.launch { store.setContentTypes(ids) }; track(OnboardingEvents.ANSWER, mapOf("question" to "content", "ids" to ids)) },
            setUseCases = { ids -> scope.launch { store.setUseCases(ids) }; track(OnboardingEvents.ANSWER, mapOf("question" to "use_cases", "ids" to ids)) },
            setReminders = { scope.launch { store.setAyahReminders(it) } },
            setRemindersAllowed = { granted -> scope.launch { store.setAyahRemindersAllowed(granted) } },
            setAyahFormat = { chosen ->
                scope.launch { store.setAyahFormat(chosen) }
                track(OnboardingEvents.AYAH_FORMAT, mapOf("format" to chosen.name))
            },
            setStyle = { chosen ->
                scope.launch { store.setWallpaperStyle(chosen) }
                track(OnboardingEvents.THEME, mapOf("theme" to chosen.name))
            },
            startStreak = {
                scope.launch { store.startStreak(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date) }
            },
            onNotificationPermission = { granted ->
                track(OnboardingEvents.NOTIF_PERMISSION, mapOf("granted" to granted))
                scope.launch { notificationOnboarding(granted).enable() }
            },
            onLocationPermission = { permission ->
                track(OnboardingEvents.LOCATION, mapOf("method" to if (permission == LocationPermission.GRANTED) "gps" else "manual"))
                if (permission == LocationPermission.GRANTED) useGpsFix()
            },
            onChooseCity = { navigator.push(Screen.CitySearch) },
            onRequestExactAlarms = ::requestExactAlarmAccess,
            prepare = { container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED) },
            track = ::track,
            onComplete = {
                track(OnboardingEvents.COMPLETE, emptyMap())
                scope.launch {
                    settings.setOnboardingComplete(true)
                    // The reminders follow the answers given since the loading screen armed them
                    // (the ayah format, the place): armed again now that everything is known.
                    container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
                    // Onboarding ends on the feed, the app's main screen (feed spec §2).
                    navigator.replaceAll(Screen.Feed)
                }
            },
        ),
        locationRepository = container.locationRepository,
        widgetPinRequester = container.widgetPinRequester,
        addPath = widgetAddPath,
    )
}

private suspend fun loadSample(
    container: AppContainer,
    reading: ReadingSettings,
    ref: Pair<Int, Int>,
    languageTag: String,
): SampleAyah {
    val quran = container.quranRepository
    val (surah, ayah) = ref
    val info = quran.surah(surah)
    val translation = if (reading.translationId == ReadingSettings.NO_TRANSLATION) {
        null
    } else {
        quran.translationText(reading.translationId, surah, ayah)?.takeIf { it.isNotBlank() }
    }
    return SampleAyah(
        surah = surah,
        ayah = ayah,
        surahName = if (UiLanguage.of(languageTag) == UiLanguage.ARABIC) info.nameArabic else info.nameLatin,
        arabic = quran.ayahText(surah, ayah).orEmpty(),
        translation = translation,
    )
}

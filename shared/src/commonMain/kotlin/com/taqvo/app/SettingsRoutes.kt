package com.taqvo.app

import androidx.compose.runtime.collectAsState
import com.taqvo.app.resources.settings_notifications_off
import com.taqvo.app.resources.settings_daily_ayahs_value
import com.taqvo.app.notifications.rememberNotificationPermissionRequester
import com.taqvo.app.feature.settings.DailyAyahsSettingsScreen
import com.taqvo.app.ayah.AyahReminderSettings
import com.taqvo.app.ayah.AyahFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.audio.SoundPreviewPlayer
import com.taqvo.app.design.ThemeMode
import com.taqvo.app.di.AppContainer
import com.taqvo.app.domain.GeoLocation
import com.taqvo.app.domain.HighLatitudePreference
import com.taqvo.app.domain.LocationSource
import com.taqvo.app.domain.NotificationSettings
import com.taqvo.app.domain.PrayerSettings
import com.taqvo.app.domain.PrayerSound
import com.taqvo.app.domain.WidgetBackground
import com.taqvo.app.feature.onboarding.OnboardingStep
import com.taqvo.app.feature.recitation.RecitationState
import com.taqvo.app.feature.recitation.reciterName
import com.taqvo.app.feature.settings.AboutScreen
import com.taqvo.app.feature.settings.AboutTimesScreen
import com.taqvo.app.feature.settings.aboutUnitCityId
import com.taqvo.app.feature.settings.AppearanceSettingsScreen
import com.taqvo.app.feature.settings.AttributionScreen
import com.taqvo.app.feature.settings.CitySearchScreen
import com.taqvo.app.feature.settings.HighLatitudePickerScreen
import com.taqvo.app.feature.settings.LocationSettingsScreen
import com.taqvo.app.feature.settings.AdjustmentsState
import com.taqvo.app.feature.settings.ManualAdjustmentsScreen
import com.taqvo.app.feature.settings.NotificationSettingsScreen
import com.taqvo.app.feature.settings.PrayerTimesSettingsScreen
import com.taqvo.app.feature.settings.SettingsRootScreen
import com.taqvo.app.feature.settings.WidgetsSettingsScreen
import com.taqvo.app.feature.settings.TimetableCheck
import com.taqvo.app.feature.settings.TimetableChooser
import com.taqvo.app.feature.settings.TimetableStatus
import com.taqvo.app.feature.settings.highLatitudeNeedsCheck
import com.taqvo.app.feature.settings.timetableName
import com.taqvo.app.feature.settings.aboutTimesUiState
import com.taqvo.app.feature.settings.themeDisplayName
import com.taqvo.app.feature.settings.WidgetsRowValue
import com.taqvo.app.feature.settings.widgetsRowValue
import com.taqvo.app.i18n.ClockFormat
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.i18n.timetableDisplayName
import com.taqvo.app.location.LocationPermission
import com.taqvo.app.nav.Navigator
import com.taqvo.app.nav.Screen
import com.taqvo.app.prayer.engine.registry.Registry
import com.taqvo.app.notifications.RescheduleTrigger
import com.taqvo.app.notifications.openAppNotificationSettings
import com.taqvo.app.notifications.requestExactAlarmAccess
import com.taqvo.app.prayer.PrayerTimesEngine
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.prayer_current_location
import com.taqvo.app.resources.settings_widgets_all
import com.taqvo.app.resources.settings_widgets_count
import com.taqvo.app.resources.settings_widgets_none
import com.taqvo.app.settings.ResolvedCityName
import com.taqvo.app.settings.SettingsRepository
import com.taqvo.app.widget.WidgetMirrorRefresher
import com.taqvo.app.widget.WidgetPlacement
import com.taqvo.app.widget.refreshWidgets
import com.taqvo.app.widget.reread
import com.taqvo.app.adhkar.AdhkarCatalogue
import com.taqvo.app.feature.settings.WallpapersScreen
import com.taqvo.app.i18n.uiLanguage
import com.taqvo.app.quran.RTL_TRANSLATION_LANGUAGES
import com.taqvo.app.quran.displayName
import com.taqvo.app.wallpaper.WallpaperSource
import com.taqvo.app.wallpaper.WallpaperText
import com.taqvo.app.wallpaper.WallpaperTexts
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException

@Composable
internal fun SettingsRootRoute(
    locationState: State<GeoLocation?>,
    cityDisplayNameState: State<String?>,
    prayerSettingsState: State<PrayerSettings>,
    themeModeState: State<ThemeMode>,
    notificationSettingsState: State<NotificationSettings>,
    container: AppContainer,
    navigator: Navigator,
    recitationStateState: State<RecitationState>,
) {
    val location by locationState
    val cityDisplayName by cityDisplayNameState
    val prayerSettings by prayerSettingsState
    val themeMode by themeModeState
    val notificationSettings by notificationSettingsState
    val recitationState by recitationStateState
    // Null until a read answers, so the Widgets row shows no value rather than a guess that
    // changes a moment later, or a count nobody knows. Read again on every ON_START, not just on
    // entering composition, because widgets are added and removed outside the app and the reader
    // comes back from the home screen to this tab; a read that cannot tell keeps the last answer
    // (reread). The read hops off the main thread, since Android's answer is a binder call to the
    // launcher and iOS's can take up to two seconds.
    var placement by remember { mutableStateOf<WidgetPlacement?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(container.widgetPlacementSource, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val shown = placement
            placement = withContext(Dispatchers.Default) { container.widgetPlacementSource.reread(shown) }
        }
    }
    val format = LocalPlatformFormat.current
    val widgetsValue = when (val value = widgetsRowValue(placement)) {
        WidgetsRowValue.Hidden -> null
        WidgetsRowValue.AllAdded -> stringResource(Res.string.settings_widgets_all)
        WidgetsRowValue.NoneAdded -> stringResource(Res.string.settings_widgets_none)
        is WidgetsRowValue.Some -> stringResource(
            Res.string.settings_widgets_count,
            format.localizedDigits(value.placed),
            format.localizedDigits(value.total),
        )
    }
    val dailyAyahs by container.onboardingStore.ayahReminders.collectAsState(AyahReminderSettings())
    val dailyAyahsValue = if (dailyAyahs.enabled) {
        stringResource(Res.string.settings_daily_ayahs_value, format.localizedDigits(dailyAyahs.minutesOfDay().size))
    } else {
        stringResource(Res.string.settings_notifications_off)
    }
    SettingsRootScreen(
        dailyAyahsValue = dailyAyahsValue,
        onOpenDailyAyahs = { navigator.push(Screen.DailyAyahs) },
        // Null only when there is no location at all — the row then reads
        // "Not set" rather than naming a city nobody chose.
        cityName = if (location == null) {
            null
        } else {
            cityDisplayName ?: stringResource(Res.string.prayer_current_location)
        },
        methodName = timetableDisplayName(prayerSettings.timetable),
        themeName = themeDisplayName(themeMode),
        notificationSettings = notificationSettings,
        onOpenLocation = { navigator.push(Screen.LocationSettings) },
        onOpenPrayerTimes = { navigator.push(Screen.PrayerTimesSettings) },
        onOpenNotifications = { navigator.push(Screen.NotificationSettings) },
        onOpenAppearance = { navigator.push(Screen.Appearance) },
        widgetsValue = widgetsValue,
        onOpenWidgets = { navigator.push(Screen.Widgets) },
        onOpenWallpapers = { navigator.push(Screen.Wallpapers) },
        onOpenAbout = { navigator.push(Screen.About) },
        reciterName = recitationState.reciter?.let { reciterName(it) }.orEmpty(),
        onOpenRecitation = { navigator.push(Screen.RecitationSettings) },
        onOpenFavorites = { navigator.push(Screen.Favorites) },
        onOpenCollections = { navigator.push(Screen.Collections) },
    )
}

@Composable
internal fun NotificationSettingsRoute(
    notificationSettingsState: State<NotificationSettings>,
    exactAlarmsAllowedState: State<Boolean>,
    notificationsGrantedState: State<Boolean>,
    scope: CoroutineScope,
    refreshPermissions: suspend () -> Unit,
    navigator: Navigator,
    settings: SettingsRepository,
    container: AppContainer,
    soundPreviewPlayer: SoundPreviewPlayer,
) {
    val notificationSettings by notificationSettingsState
    val exactAlarmsAllowed by exactAlarmsAllowedState
    val notificationsGranted by notificationsGrantedState
    NotificationSettingsScreen(
        settings = notificationSettings,
        exactAlarmsUnavailable = !exactAlarmsAllowed,
        notificationsGranted = notificationsGranted,
        onOpenNotificationSettings = ::openAppNotificationSettings,
        onRequestExactAlarms = ::requestExactAlarmAccess,
        onPermissionChecked = { scope.launch { refreshPermissions() } },
        onBack = { navigator.pop() },
        onToggleEnabled = { enabled ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(enabled = enabled))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickLead = { minutes ->
            scope.launch {
                settings.setNotificationSettings(
                    notificationSettings.copy(remindBeforeMinutes = minutes),
                )
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickSound = { prayer, sound ->
            scope.launch {
                val updated = notificationSettings.copy(
                    sounds = notificationSettings.sounds + (prayer to sound),
                )
                settings.setNotificationSettings(updated)
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onToggleTahajjud = { on ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(tahajjud = on))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickTahajjudSound = { sound ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(tahajjudSound = sound))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onToggleAdhkar = { on ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(adhkar = on))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickAdhkarSound = { sound ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(adhkarSound = sound))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onToggleDuha = { on ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(duha = on))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickDuhaTiming = { timing ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(duhaTiming = timing))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickDuhaSound = { sound ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(duhaSound = sound))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onToggleIqama = { on ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(iqama = on))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickIqamaMinutes = { prayer, minutes ->
            scope.launch {
                val updated = notificationSettings.copy(
                    iqamaMinutes = notificationSettings.iqamaMinutes + (prayer to minutes),
                )
                settings.setNotificationSettings(updated)
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickIqamaSound = { sound ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(iqamaSound = sound))
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onPickVoice = { voice ->
            scope.launch {
                settings.setNotificationSettings(notificationSettings.copy(voice = voice))
                // The same reschedule a sound change needs, and for the same
                // reason: a channel's sound is immutable, so the new voice
                // only reaches the user through channels built from the new
                // plan.
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        // The sheet's Takbir and Adhan buttons audition the voice that is
        // actually chosen, never the original by default.
        onPreviewSound = { soundPreviewPlayer.play(it, notificationSettings.voice) },
        onPreviewVoice = { voice -> soundPreviewPlayer.play(PrayerSound.ADHAN, voice) },
        onStopPreview = { soundPreviewPlayer.stop() },
    )
}

/**
 * After anything that changes the prayer times outside the Prayer screen (ruling R53): a
 * prayer-settings write, a new location (a city picked, a GPS fix), the one-time backfill of a
 * location's country or region, and each time the iPhone app is in front (APP_FOREGROUND). The armed
 * notifications and the widgets' mirror move to the new times at once, as a language change moves
 * them to the new words (`LanguageChangeReschedule`), instead of at the next foreground, alarm or
 * top-up.
 *
 * One at a time, like the reschedule inside it: two quick taps refresh in the order they were
 * made, so the mirror the widgets end on is the latest settings'. The engine's days, cold after a
 * change, are computed off the main thread (the coordinator plans on the default dispatcher, and
 * so does the mirror here).
 */
internal suspend fun refreshForNewTimes(
    container: AppContainer,
    settings: SettingsRepository,
    trigger: RescheduleTrigger = RescheduleTrigger.SETTINGS_CHANGED,
) {
    newTimesRefresh.withLock {
        container.notificationCoordinator.reschedule(trigger)
        withContext(Dispatchers.Default) {
            // Null, not the composition's format: that is rebuilt on a new clock format only a
            // frame after the write that just happened, so it can still be the old one. Null
            // builds the mirror's from the stored setting, which is always current.
            WidgetMirrorRefresher.refresh(settings, container.prayerTimesEngine, format = null)
        }
        refreshWidgets()
    }
}

private val newTimesRefresh = Mutex()

/*
 * Every prayer-time control writes its own field through the repository on touch (spec §2.2's
 * per-field writes), then refreshes what was armed with the old times ([refreshForNewTimes]).
 */

@Composable
internal fun PrayerTimesSettingsRoute(
    prayerSettingsState: State<PrayerSettings>,
    clockFormatState: State<ClockFormat>,
    locationState: State<GeoLocation?>,
    cityDisplayNameState: State<String?>,
    today: LocalDate,
    scope: CoroutineScope,
    settings: SettingsRepository,
    container: AppContainer,
    navigator: Navigator,
) {
    val prayerSettings by prayerSettingsState
    val location by locationState
    val cityDisplayName by cityDisplayNameState
    // App's state, collected since launch, as the prayer settings are: a collector of its own
    // here would start at 24-hour and show a 12-hour reader "24-hour" for a frame.
    val clockFormat by clockFormatState
    fun write(change: suspend () -> Unit) {
        scope.launch {
            change()
            refreshForNewTimes(container, settings)
        }
    }
    // Today's day as the Prayer screen has it (the engine's cache already holds it): what applies
    // here, whether the stored timetable does, and what is paused.
    val day = location?.let { container.prayerTimesEngine.dayFor(it, today, prayerSettings) }
    PrayerTimesSettingsScreen(
        settings = prayerSettings,
        status = day?.let { TimetableStatus.of(prayerSettings, it) },
        adjustmentsPaused = day?.pausedAdjustments?.isNotEmpty() == true,
        placeName = cityDisplayName.takeIf { location != null },
        today = today,
        clockFormat = clockFormat,
        // Re-plans the notifications and rewrites the widget mirror, like every write here: the
        // clock format is part of how both are worded.
        onClockFormat = { write { settings.setClockFormat(it) } },
        onSchool = { write { settings.setSchool(it) } },
        onHijriOffset = { write { settings.setHijriOffsetDays(it) } },
        onShowSunrise = { write { settings.setShowSunrise(it) } },
        onShowBothAsr = { write { settings.setShowBothAsr(it) } },
        onShowWhereDiffer = { write { settings.setShowWhereDiffer(it) } },
        onSaudiFajrLater = { write { settings.setSaudiFajrLater(it) } },
        onBack = { navigator.pop() },
        // A timetable belongs to a place: without one, the way forward is to set it.
        onOpenTimetable = { navigator.push(if (location != null) Screen.Timetable else Screen.LocationSettings) },
        onOpenHighLatitudePicker = { navigator.push(Screen.HighLatitudePicker) },
        onOpenManualAdjustments = { navigator.push(Screen.ManualAdjustments) },
        onOpenAboutTimes = { navigator.push(Screen.AboutTimes) },
    )
}

/**
 * "About these times" (spec §2.3, task 11): filled from the never-early engine's own resolution
 * for the stored location, exactly as the Prayer screen and Manual adjustments read it
 * ([com.taqvo.app.prayer.PrayerTimesEngine.dayFor]). Pops back if the location disappears from
 * under it (the location screen's own "clear" — unreachable today, but [ManualAdjustmentsScreen]
 * guards the same way for when it becomes one).
 */
@Composable
internal fun AboutTimesRoute(
    prayerSettingsState: State<PrayerSettings>,
    locationState: State<GeoLocation?>,
    cityDisplayNameState: State<String?>,
    today: LocalDate,
    container: AppContainer,
    navigator: Navigator,
) {
    val prayerSettings by prayerSettingsState
    val location by locationState
    val cityDisplayName by cityDisplayNameState
    val loc = location
    if (loc == null) {
        LaunchedEffect(Unit) { navigator.pop() }
        return
    }
    // Read once, as the screen opens: the stack moves on when a link pushes the next screen.
    val offersPrayerTimes = remember { aboutOffersPrayerTimesSettings(navigator.backStack.value) }
    val zone = remember(loc.timeZoneId) { TimeZone.of(loc.timeZoneId) }
    val place = remember(loc) { PrayerTimesEngine.placeOf(loc) }
    val engineDay = remember(loc, prayerSettings, today) {
        container.prayerTimesEngine.dayFor(loc, today, prayerSettings)
    }
    val state = remember(engineDay, place, today) {
        aboutTimesUiState(engineDay, place, today, prayerSettings.hijriOffsetDays)
    }
    // A city's own table (Umm al-Qura's and QMDB's places): About names the city whose table the times
    // follow, in the reader's language, also where the location's own nearest city is another (a suburb's
    // fix inside the city's reach). The first frame reads the name already loaded, so it never shows the
    // location's name and then the city's.
    val unitCityId = aboutUnitCityId(state.resolution, loc.cityId)
    val unitCityName by produceState(unitCityId?.let { container.cityRepository.loadedDisplayName(it) }, unitCityId, cityDisplayName) {
        value = unitCityId?.let { container.cityRepository.displayName(it) }
    }
    AboutTimesScreen(
        state = state,
        engineDay = engineDay,
        place = place,
        cityLabel = unitCityName ?: cityDisplayName ?: stringResource(Res.string.prayer_current_location),
        zone = zone,
        onBack = { navigator.pop() },
        onMatchMyMosque = { navigator.push(Screen.MatchMyMosque) },
        onOpenPrayerTimesSettings = if (offersPrayerTimes) ({ navigator.push(Screen.PrayerTimesSettings) }) else null,
    )
}

/**
 * Whether About these times links to Prayer times settings: everywhere but when About was opened
 * from that screen, where the link would only stack it a second time and Back is the way there.
 */
internal fun aboutOffersPrayerTimesSettings(backStack: List<Screen>): Boolean =
    backStack.getOrNull(backStack.lastIndex - 1) != Screen.PrayerTimesSettings

/**
 * The high-latitude rule of an Other method (spec §2.2). A confirmed one is checked again with the
 * new rule before it is written (review I1): never earlier than Automatic, the rule is written and
 * the confirmation kept; earlier, the warning names the new figures, "Follow it" writes the rule
 * and confirms, and the other button changes nothing.
 */
@Composable
internal fun HighLatitudePickerRoute(
    prayerSettingsState: State<PrayerSettings>,
    locationState: State<GeoLocation?>,
    today: LocalDate,
    scope: CoroutineScope,
    settings: SettingsRepository,
    container: AppContainer,
    navigator: Navigator,
) {
    val prayerSettings by prayerSettingsState
    val location by locationState
    val screenScope = rememberCoroutineScope()
    val current = prayerSettings.legacyHighLatitude ?: HighLatitudePreference.AUTOMATIC
    val timetable = prayerSettings.timetable
    val chooser = remember(location, prayerSettings, today) {
        TimetableChooser(
            checkScope = screenScope,
            check = { rule, onProgress ->
                val here = location ?: return@TimetableChooser TimetableCheck.NotHere
                val withRule = prayerSettings.copy(legacyHighLatitude = HighLatitudePreference.valueOf(rule))
                checkTimetable(container.prayerTimesEngine, here, withRule, today, timetable, onProgress)
            },
            nameKeyOf = { Registry.byId(timetable)?.shortNameKey ?: timetable },
            apply = { rule, confirmed ->
                scope.launch {
                    settings.setLegacyHighLatitude(HighLatitudePreference.valueOf(rule))
                    if (confirmed) {
                        settings.setTimetableConfirmed(timetable, location?.let { container.prayerTimesEngine.automaticEntryId(it) })
                    }
                    refreshForNewTimes(container, settings)
                }
                navigator.pop()
            },
            needsCheck = { location != null && highLatitudeNeedsCheck(prayerSettings) },
        )
    }
    HighLatitudePickerScreen(
        current = current,
        onPick = { picked -> if (picked == current) navigator.pop() else chooser.choose(picked.name) },
        onBack = { navigator.pop() },
        checking = chooser.checking?.progress,
        warning = chooser.warning,
        automatic = location?.let { container.prayerTimesEngine.dayFor(it, today, prayerSettings).resolution.timetableName() },
        onKeep = chooser::keep,
        onFollow = chooser::follow,
    )
}

@Composable
internal fun ManualAdjustmentsRoute(
    prayerSettingsState: State<PrayerSettings>,
    locationState: State<GeoLocation?>,
    container: AppContainer,
    today: LocalDate,
    scope: CoroutineScope,
    settings: SettingsRepository,
    navigator: Navigator,
) {
    val prayerSettings by prayerSettingsState
    val location by locationState
    val state = location?.let { AdjustmentsState.of(container.prayerTimesEngine.dayFor(it, today, prayerSettings), prayerSettings, TimeZone.of(it.timeZoneId)) }
        ?: AdjustmentsState()
    ManualAdjustmentsScreen(
        adjustments = prayerSettings.minuteAdjustments,
        confirmed = prayerSettings.confirmedAdjustments,
        state = state,
        onSet = { prayer, minutes, confirmUnder ->
            scope.launch {
                settings.setMinuteAdjustment(prayer, minutes, confirmUnder)
                refreshForNewTimes(container, settings)
            }
        },
        onOpenTimetable = { navigator.push(Screen.Timetable) },
        onBack = { navigator.pop() },
    )
}

@Composable
internal fun LocationSettingsRoute(
    locationState: State<GeoLocation?>,
    cityDisplayNameState: State<String?>,
    locationSourceState: State<LocationSource>,
    container: AppContainer,
    useGpsFix: () -> Unit,
    navigator: Navigator,
) {
    val location by locationState
    val cityDisplayName by cityDisplayNameState
    val locationSource by locationSourceState
    LocationSettingsScreen(
        location = location,
        cityName = cityDisplayName,
        locationSource = locationSource,
        locationRepository = container.locationRepository,
        onLocationPermission = { permission ->
            if (permission == LocationPermission.GRANTED) useGpsFix()
        },
        onChooseCity = { navigator.push(Screen.CitySearch) },
        onBack = { navigator.pop() },
    )
}

@Composable
internal fun CitySearchRoute(
    container: AppContainer,
    scope: CoroutineScope,
    settings: SettingsRepository,
    languageTag: String,
    backStackState: State<List<Screen>>,
    onboardingStepState: MutableState<OnboardingStep>,
    navigator: Navigator,
) {
    val backStack by backStackState
    var onboardingStep by onboardingStepState
    CitySearchScreen(
        cityRepository = container.cityRepository,
        onPick = { city ->
            scope.launch {
                val picked = city.toGeoLocation()
                // The row the user tapped was already rendered in their
                // language, so the name is known here — stored with the
                // location so the header needs no lookup on any later
                // launch, the first one after picking included.
                settings.setLocation(
                    picked,
                    ResolvedCityName(city.displayName, languageTag),
                )
                // The only writer of MANUAL, which is what makes turning the
                // toggle off reversible: back out of the search and the
                // stored source — and so the toggle — is untouched.
                settings.setLocationSource(LocationSource.MANUAL)
                // A location save no longer writes a calculation method (spec §8): the
                // never-early engine's Automatic timetable already follows the picked city.
                // The alarms and widgets armed for the old city follow it now (ruling R53).
                refreshForNewTimes(container, settings)
            }
            // Reached from onboarding, a successful pick answers the location
            // question, so the flow continues rather than re-asking it: on to the widget guide,
            // which onboarding itself passes over (or finishes on) when the reader did not ask
            // for widgets.
            if (backStack.contains(Screen.Onboarding)) {
                onboardingStep = OnboardingStep.WIDGET_GUIDE
            }
            navigator.pop()
        },
        onBack = { navigator.pop() },
    )
}

@Composable
internal fun AppearanceRoute(
    themeModeState: State<ThemeMode>,
    scope: CoroutineScope,
    settings: SettingsRepository,
    navigator: Navigator,
) {
    val themeMode by themeModeState
    AppearanceSettingsScreen(
        current = themeMode,
        // No pop: the whole app repaints behind this screen, and seeing that happen
        // is the confirmation the choice took effect.
        onPick = { scope.launch { settings.setThemeMode(it) } },
        onBack = { navigator.pop() },
    )
}

/**
 * Settings › Widgets: the background every widget shares, each widget's preview, and the way to
 * add the ones that are not on a home screen.
 */
@Composable
internal fun WidgetsRoute(
    container: AppContainer,
    scope: CoroutineScope,
    settings: SettingsRepository,
    widgetBackgroundState: State<WidgetBackground>,
    navigator: Navigator,
) {
    val widgetBackground by widgetBackgroundState
    WidgetsSettingsScreen(
        widgetBackground = widgetBackground,
        onPickWidgetBackground = { value ->
            scope.launch {
                settings.setWidgetBackground(value)
                // The widget processes never see SettingsRepository/DataStore —
                // only the mirror — so the choice has to be written there too,
                // under the same key TaqvoGlanceWidget.kt (Android) and the iOS
                // TimelineProvider (Task 24) read.
                com.taqvo.app.widget.WidgetMirrorWriter.writeBackground(
                    com.taqvo.app.widget.createWidgetKeyValueStore(),
                    value,
                )
                com.taqvo.app.widget.refreshWidgets()
            }
        },
        widgetPinRequester = container.widgetPinRequester,
        widgetPlacementSource = container.widgetPlacementSource,
        onBack = { navigator.pop() },
    )
}

/** Settings › About Taqvo, the app's profile page; Attribution & licences opens from its Open source card. */
@Composable
internal fun AboutRoute(navigator: Navigator) {
    AboutScreen(
        onBack = { navigator.pop() },
        onOpenAttribution = { navigator.push(Screen.Attribution) },
    )
}

@Composable
internal fun AttributionRoute(
    container: AppContainer,
    recitationStateState: State<RecitationState>,
    navigator: Navigator,
) {
    val recitationState by recitationStateState
    // The translation credits must never drift from what is actually
    // bundled (spec §6), so they are read from the database rather than
    // hardcoded; an empty list while loading is fine, it fills in a frame
    // later.
    val translations by produceState(initialValue = emptyList<com.taqvo.app.quran.TranslationInfo>()) {
        value = container.quranRepository.translations()
    }
    val feedSources by produceState(initialValue = emptyList<com.taqvo.app.content.ContentSourceInfo>()) {
        value = runCatching { container.contentRepository.sources() }.getOrDefault(emptyList())
    }
    AttributionScreen(
        translations = translations,
        // The catalogue in force, so a reciter withdrawn by a manifest
        // refresh leaves the credits with them (spec §2).
        reciters = recitationState.reciters,
        onBack = { navigator.pop() },
        feedSources = feedSources,
    )
}

/**
 * Settings › Wallpapers. The ayahs come from the Quran database in the reader's translation, the
 * supplications from the adhkar file in the interface's language; the ayahs are shown as soon as
 * they are read, before the supplications have been.
 */
@Composable
internal fun WallpapersRoute(
    container: AppContainer,
    settings: SettingsRepository,
    languageTag: String,
    navigator: Navigator,
) {
    val ui = uiLanguage()
    var texts by remember { mutableStateOf(emptyMap<WallpaperSource, List<WallpaperText>>()) }
    var surahNames by remember { mutableStateOf(emptyMap<Int, String>()) }
    var ayahTranslationRtl by remember { mutableStateOf(false) }
    LaunchedEffect(languageTag, ui) {
        val quran = container.quranRepository
        val reading = settings.readingSettings(languageTag).first()
        ayahTranslationRtl = reading.translationId.substringBefore('.') in RTL_TRANSLATION_LANGUAGES
        // An Arabic-script interface names surahs in Arabic, as the reader does.
        surahNames = quran.surahs().associate { it.number to it.displayName(ui.arabicScript) }
        texts = mapOf(WallpaperSource.AYAH to WallpaperTexts.ayahs(quran, reading))
        val duas = try {
            WallpaperTexts.duas(AdhkarCatalogue.load(), AdhkarCatalogue.loadText(ui.code))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (unreadable: Exception) {
            // The supplications' segment stays empty; the ayahs are unaffected.
            emptyList()
        }
        texts = texts + (WallpaperSource.DUA to duas)
    }
    WallpapersScreen(
        texts = texts,
        surahNames = surahNames,
        ayahTranslationRtl = ayahTranslationRtl,
        duaTranslationRtl = ui.rtl,
        setter = container.wallpaperSetter,
        onBack = { navigator.pop() },
    )
}

/**
 * Settings › Daily ayahs. Every change is saved, and the plan armed again so the reminders follow
 * at once. Switching on asks the system first, as onboarding does: a refusal leaves them off.
 */
@Composable
internal fun DailyAyahsRoute(
    container: AppContainer,
    scope: CoroutineScope,
    navigator: Navigator,
) {
    val store = container.onboardingStore
    val reminders by store.ayahReminders.collectAsState(AyahReminderSettings())
    val ayahFormat by store.ayahFormat.collectAsState(AyahFormat.ARABIC_TRANSLATION)
    fun save(settings: AyahReminderSettings) {
        scope.launch {
            store.setAyahReminders(settings)
            container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
        }
    }
    val requestPermission = rememberNotificationPermissionRequester { granted ->
        scope.launch {
            store.setAyahRemindersAllowed(granted)
            container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
        }
    }
    DailyAyahsSettingsScreen(
        settings = reminders,
        format = ayahFormat,
        onSettings = ::save,
        onEnable = requestPermission,
        onFormat = { chosen ->
            scope.launch {
                store.setAyahFormat(chosen)
                container.notificationCoordinator.reschedule(RescheduleTrigger.SETTINGS_CHANGED)
            }
        },
        onBack = { navigator.pop() },
    )
}

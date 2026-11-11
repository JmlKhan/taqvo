package com.taqvo.app.feature.onboarding

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.ayah.AyahFormat
import com.taqvo.app.ayah.AyahReminderSettings
import com.taqvo.app.design.components.TaqvoPrimaryButton
import com.taqvo.app.design.components.TaqvoTextLink
import com.taqvo.app.domain.WidgetBackground
import com.taqvo.app.feature.settings.WidgetPreview
import com.taqvo.app.i18n.AppLanguage
import com.taqvo.app.i18n.LocalPlatformFormat
import com.taqvo.app.location.LocationPermission
import com.taqvo.app.location.LocationRepository
import com.taqvo.app.location.rememberLocationPermissionRequester
import com.taqvo.app.notifications.rememberNotificationPermissionRequester
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.onb_content_comfort
import com.taqvo.app.resources.onb_content_gratitude
import com.taqvo.app.resources.onb_content_mix
import com.taqvo.app.resources.onb_content_patience
import com.taqvo.app.resources.onb_content_reflection
import com.taqvo.app.resources.onb_content_short
import com.taqvo.app.resources.onb_content_title
import com.taqvo.app.resources.onb_goal_habit
import com.taqvo.app.resources.onb_goal_hardship
import com.taqvo.app.resources.onb_goal_iman
import com.taqvo.app.resources.onb_goal_peace
import com.taqvo.app.resources.onb_goal_remember
import com.taqvo.app.resources.onb_goals_title
import com.taqvo.app.resources.onb_use_daily_ayahs
import com.taqvo.app.resources.onb_use_daily_habit
import com.taqvo.app.resources.onb_use_prayer_times
import com.taqvo.app.resources.onb_use_quran_reading
import com.taqvo.app.resources.onb_use_tasbeh
import com.taqvo.app.resources.onb_use_title
import com.taqvo.app.resources.onb_use_wallpapers
import com.taqvo.app.resources.onb_use_widgets
import com.taqvo.app.resources.onboarding_widget_cta_add
import com.taqvo.app.resources.onboarding_widget_cta_done
import com.taqvo.app.resources.onboarding_widget_secondary
import com.taqvo.app.resources.onboarding_widget_steps_android
import com.taqvo.app.resources.onboarding_widget_title
import com.taqvo.app.widget.PinnableWidget
import com.taqvo.app.widget.WidgetAddPath
import com.taqvo.app.widget.WidgetPinRequester
import com.taqvo.app.wallpaper.WallpaperStyle
import com.taqvo.app.widget.askToPin

/** What onboarding shows: the answers so far and the settings they have made. */
data class OnboardingModel(
    val answers: OnboardingAnswers,
    val language: AppLanguage,
    val reminders: AyahReminderSettings,
    val ayahFormat: AyahFormat,
    /** The feed background chosen on the theme screen (an id from `FeedThemes`). */
    val themeId: String,
    /** The wallpaper look the later showcase screens mock up. */
    val style: WallpaperStyle,
    /** The ayah the wallpaper screens draw; null until the Quran database has answered. */
    val wallpaperSample: WallpaperSample?,
    /** The sample ayah as a reminder would show it in each format. */
    val previewFor: (AyahFormat) -> AyahPreviewText?,
    val exactAlarmsNeeded: Boolean,
    val ios: Boolean,
    val today: DayOfWeek,
)

/** Everything onboarding writes or asks for. Each answer is saved the moment it is given. */
class OnboardingActions(
    val setLanguage: (AppLanguage) -> Unit,
    val setName: (String?) -> Unit,
    val setGoals: (List<String>) -> Unit,
    val setContentTypes: (List<String>) -> Unit,
    val setUseCases: (List<String>) -> Unit,
    val setReminders: (AyahReminderSettings) -> Unit,
    /** The notification prompt's answer: turns the reminders on or off, leaving their times be. */
    val setRemindersAllowed: (Boolean) -> Unit,
    val setAyahFormat: (AyahFormat) -> Unit,
    val setTheme: (String) -> Unit,
    val startStreak: () -> Unit,
    /** The system's answer to the notification prompt: prayer notifications follow it too. */
    val onNotificationPermission: (Boolean) -> Unit,
    val onLocationPermission: (LocationPermission) -> Unit,
    val onChooseCity: () -> Unit,
    val onRequestExactAlarms: () -> Unit,
    /** Plans and arms the notifications; screen 10 runs while it does. */
    val prepare: suspend () -> Unit,
    val track: (String, Map<String, Any?>) -> Unit,
    val onComplete: () -> Unit,
)

/**
 * Onboarding (spec §1): splash, five questions, the reminders, the reader's wallpaper, the
 * showcase, the welcome, the ayah format, the place for the prayer times and the widget guide.
 *
 * A screen that comes before a system prompt has one button and it always opens the prompt (App
 * Review guideline 5.1.1(iv)): the reminders screen and the location screen. The alternative to a
 * refusal comes after the prompt, never beside it.
 */
@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    onStep: (OnboardingStep) -> Unit,
    model: OnboardingModel,
    actions: OnboardingActions,
    locationRepository: LocationRepository,
    widgetPinRequester: WidgetPinRequester,
    addPath: WidgetAddPath,
) {
    val context = OnboardingContext(
        exactAlarmsNeeded = model.exactAlarmsNeeded,
        notificationsGranted = model.reminders.enabled,
        wantsWidgets = model.answers.wantsWidgets,
    )
    val progress = OnboardingFlow.progress(step, context)
    fun advance(from: OnboardingStep = step, ctx: OnboardingContext = context) {
        val next = OnboardingFlow.next(from, ctx)
        if (next == null) actions.onComplete() else onStep(next)
    }
    fun skip(clear: () -> Unit) {
        clear()
        actions.track(com.taqvo.app.analytics.OnboardingEvents.SKIP, mapOf("step_id" to step.name))
        advance()
    }

    LaunchedEffect(step) {
        actions.track(
            com.taqvo.app.analytics.OnboardingEvents.STEP_VIEW,
            mapOf("step_id" to step.name, "index" to OnboardingFlow.steps(context).indexOf(step)),
        )
    }

    val requestLocation = rememberLocationPermissionRequester(locationRepository) { permission ->
        actions.onLocationPermission(permission)
        when (locationAnswer(permission)) {
            LocationAnswer.CONTINUE -> advance(OnboardingStep.LOCATION)
            // The city search moves the step on itself once a city is picked.
            LocationAnswer.CHOOSE_CITY -> actions.onChooseCity()
            LocationAnswer.STAY -> Unit
        }
    }
    var asking by remember { mutableStateOf(false) }
    val requestNotifications = rememberNotificationPermissionRequester { granted ->
        asking = false
        actions.setRemindersAllowed(granted)
        actions.onNotificationPermission(granted)
        // Granted, Android 14+ asks for exact alarms next; the context is the one the answer makes.
        advance(OnboardingStep.REMINDERS, context.copy(notificationsGranted = granted))
    }
    // A step this reader does not see (the widget guide after the city search, for one who asked
    // for no widgets; a resumed step the answers have since ruled out) moves straight on.
    LaunchedEffect(step, context) {
        if (step != OnboardingStep.EXACT_ALARMS && step !in OnboardingFlow.steps(context)) advance()
    }
    // The exact-alarm grant happens in system settings; back in front holding it, the step is done.
    LaunchedEffect(step, model.exactAlarmsNeeded) {
        if (step == OnboardingStep.EXACT_ALARMS && !model.exactAlarmsNeeded) advance()
    }

    when (step) {
        OnboardingStep.SPLASH -> SplashScreen(onStart = { advance() })
        OnboardingStep.LANGUAGE -> LanguageScreen(progress, model.language, onPick = actions.setLanguage, onContinue = { advance() })
        OnboardingStep.INTRO -> IntroScreen(progress, onContinue = { advance() })
        OnboardingStep.NAME -> {
            var name by rememberSaveable { mutableStateOf(model.answers.name.orEmpty()) }
            NameScreen(
                progress,
                name,
                onName = { name = it },
                onContinue = { actions.setName(cleanName(name)); advance() },
                onSkip = { skip { actions.setName(null) } },
            )
        }
        OnboardingStep.GOALS -> QuestionScreen(
            progress,
            stringResource(Res.string.onb_goals_title),
            listOf(
                Goal.REMEMBER to stringResource(Res.string.onb_goal_remember),
                Goal.PEACE to stringResource(Res.string.onb_goal_peace),
                Goal.HARDSHIP to stringResource(Res.string.onb_goal_hardship),
                Goal.IMAN to stringResource(Res.string.onb_goal_iman),
                Goal.HABIT to stringResource(Res.string.onb_goal_habit),
            ),
            model.answers.goals.toSet(),
            onToggle = { actions.setGoals(model.answers.goals.toggled(it)) },
            onContinue = { advance() },
            onSkip = { skip { actions.setGoals(emptyList()) } },
        )
        OnboardingStep.CONTENT -> QuestionScreen(
            progress,
            stringResource(Res.string.onb_content_title),
            ContentType.ALL.zip(
                listOf(
                    stringResource(Res.string.onb_content_short),
                    stringResource(Res.string.onb_content_comfort),
                    stringResource(Res.string.onb_content_gratitude),
                    stringResource(Res.string.onb_content_patience),
                    stringResource(Res.string.onb_content_reflection),
                    stringResource(Res.string.onb_content_mix),
                ),
            ),
            model.answers.contentTypes.toSet(),
            onToggle = { actions.setContentTypes(model.answers.contentTypes.toggled(it)) },
            onContinue = { advance() },
            onSkip = { skip { actions.setContentTypes(emptyList()) } },
        )
        OnboardingStep.JOURNEY -> JourneyScreen(progress, onContinue = { advance() })
        OnboardingStep.STREAK -> {
            LaunchedEffect(Unit) { actions.startStreak() }
            StreakScreen(progress, model.today, LocalPlatformFormat.current.localizedDigits(1), onContinue = { advance() })
        }
        OnboardingStep.USE_CASES -> QuestionScreen(
            progress,
            stringResource(Res.string.onb_use_title),
            UseCase.ALL.zip(
                listOf(
                    stringResource(Res.string.onb_use_widgets),
                    stringResource(Res.string.onb_use_wallpapers),
                    stringResource(Res.string.onb_use_daily_ayahs),
                    stringResource(Res.string.onb_use_prayer_times),
                    stringResource(Res.string.onb_use_tasbeh),
                    stringResource(Res.string.onb_use_quran_reading),
                    stringResource(Res.string.onb_use_daily_habit),
                ),
            ),
            model.answers.useCases.toSet(),
            onToggle = { actions.setUseCases(model.answers.useCases.toggled(it)) },
            onContinue = { advance() },
            onSkip = { skip { actions.setUseCases(emptyList()) } },
        )
        OnboardingStep.REMINDERS -> RemindersScreen(
            progress,
            model.reminders,
            onSettings = actions.setReminders,
            preview = model.previewFor(model.ayahFormat),
            asking = asking,
            onSave = {
                actions.track(
                    com.taqvo.app.analytics.OnboardingEvents.NOTIF_CONFIG,
                    mapOf("count" to model.reminders.count, "start" to model.reminders.startMinute, "end" to model.reminders.endMinute),
                )
                asking = true
                requestNotifications()
            },
        )
        OnboardingStep.EXACT_ALARMS -> ExactAlarmsScreen(progress, onAllow = actions.onRequestExactAlarms, onLater = { advance() })
        OnboardingStep.LOADING -> LoadingScreen(progress, work = actions.prepare, onDone = { advance(OnboardingStep.LOADING) })
        OnboardingStep.THEME -> ThemeScreen(progress, model.themeId, onSelect = actions.setTheme, onContinue = { advance() })
        OnboardingStep.PREVIEW -> PreviewScreen(progress, model.wallpaperSample, model.themeId, onContinue = { advance() })
        OnboardingStep.SHOWCASE_LOCK ->
            ShowcaseLockScreen(progress, model.style, model.ios, model.previewFor(model.ayahFormat), onContinue = { advance() })
        OnboardingStep.SHOWCASE_HOME ->
            ShowcaseHomeScreen(progress, model.style, prayerFirst = model.answers.wantsPrayerTimes, onContinue = { advance() })
        OnboardingStep.WELCOME -> WelcomeScreen(model.style, model.answers.name, onDone = { advance(OnboardingStep.WELCOME) })
        OnboardingStep.AYAH_FORMAT -> AyahFormatScreen(
            progress,
            model.ayahFormat,
            onFormat = actions.setAyahFormat,
            preview = model.previewFor,
            onContinue = { advance() },
        )
        OnboardingStep.LOCATION -> LocationScreen(progress, onContinue = requestLocation)
        OnboardingStep.WIDGET_GUIDE -> if (addPath == WidgetAddPath.ANDROID_PIN) {
            OnboardingWidgetPage(widgetPinRequester, addPath, onComplete = { advance(OnboardingStep.WIDGET_GUIDE) }, progress = progress)
        } else {
            IosWidgetGuideScreen(progress, addPath, onDone = { advance(OnboardingStep.WIDGET_GUIDE) })
        }
    }
}

private fun List<String>.toggled(id: String): List<String> = if (id in this) this - id else this + id

/**
 * The widget guide on Android, and the launcher's answer to "Add widget". A launcher that takes the
 * request shows its own sheet over the app, and onboarding finishes underneath it, because the
 * user's answer never comes back reliably. One that refuses (no pin support, a work profile that
 * forbids it, or one that simply declines, as some vivo, OPPO and Xiaomi home screens do) would
 * leave the tap doing nothing, so the steps by hand replace the button. [addPath] is a parameter so
 * a test on either platform can drive the Android screen.
 */
@Composable
internal fun OnboardingWidgetPage(
    widgetPinRequester: WidgetPinRequester,
    addPath: WidgetAddPath,
    onComplete: () -> Unit,
    progress: Float? = null,
) {
    val canPin = widgetPinRequester.isSupported(PinnableWidget.PRAYER)
    // Set only when the launcher turns "Add widget" down.
    var pinRefused by rememberSaveable { mutableStateOf(false) }
    val offer = widgetStepOffer(canPin, pinRefused, addPath)
    // The steps sit in the part that scrolls; when they replace the button they are scrolled into
    // view rather than appearing out of sight below it.
    val stepsInView = remember { BringIntoViewRequester() }
    LaunchedEffect(pinRefused) {
        if (pinRefused) {
            withFrameNanos {}
            stepsInView.bringIntoView()
        }
    }
    OnbPage(
        progress = progress,
        onSkip = null,
        content = {
            WidgetPreview(
                background = WidgetBackground.FOLLOW_THEME,
                systemIsDark = isSystemInDarkTheme(),
                content = null,
            )
            Spacer(Modifier.height(28.dp))
            OnbTitle(stringResource(Res.string.onboarding_widget_title))
            Spacer(Modifier.height(12.dp))
            OnbBody(stringResource(widgetStepBodyKey(addPath)))
            if (offer == WidgetStepOffer.ANDROID_STEPS) {
                Spacer(Modifier.height(20.dp))
                // A polite live region only after a refused tap: the button TalkBack was on has just
                // gone, and this is what took its place.
                OnbBody(
                    stringResource(Res.string.onboarding_widget_steps_android),
                    Modifier
                        .bringIntoViewRequester(stepsInView)
                        .then(if (pinRefused) Modifier.semantics { liveRegion = LiveRegionMode.Polite } else Modifier),
                )
            }
        },
        footer = {
            when (offer) {
                WidgetStepOffer.PIN -> {
                    TaqvoPrimaryButton(
                        stringResource(Res.string.onboarding_widget_cta_add),
                        onClick = {
                            widgetPinRequester.askToPin(
                                PinnableWidget.PRAYER,
                                onTaken = onComplete,
                                onRefused = { pinRefused = true },
                            )
                        },
                    )
                    TaqvoTextLink(stringResource(Res.string.onboarding_widget_secondary), onClick = onComplete)
                }
                WidgetStepOffer.ANDROID_STEPS, WidgetStepOffer.DONE ->
                    TaqvoPrimaryButton(stringResource(Res.string.onboarding_widget_cta_done), onClick = onComplete)
            }
        },
    )
}


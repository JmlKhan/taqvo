package com.taqvo.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.taqvo.app.di.AppContainer
import com.taqvo.app.feature.onboarding.OnboardingScreen
import com.taqvo.app.feature.onboarding.OnboardingStep
import com.taqvo.app.location.LocationPermission
import com.taqvo.app.nav.Navigator
import com.taqvo.app.nav.Screen
import com.taqvo.app.notifications.NotificationOnboarding
import com.taqvo.app.notifications.requestExactAlarmAccess
import com.taqvo.app.settings.SettingsRepository

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
    var onboardingStep by onboardingStepState
    val exactAlarmsAllowed by exactAlarmsAllowedState
    OnboardingScreen(
        step = onboardingStep,
        onStep = { onboardingStep = it },
        locationRepository = container.locationRepository,
        widgetPinRequester = container.widgetPinRequester,
        onLocationPermission = { permission ->
            if (permission == LocationPermission.GRANTED) useGpsFix()
        },
        onChooseCity = { navigator.push(Screen.CitySearch) },
        onNotificationPermission = { granted ->
            scope.launch { notificationOnboarding(granted).enable() }
        },
        onComplete = {
            scope.launch {
                settings.setOnboardingComplete(true)
                navigator.replaceAll(Screen.Prayer)
            }
        },
        exactAlarmsNeeded = !exactAlarmsAllowed,
        onRequestExactAlarms = ::requestExactAlarmAccess,
    )
}

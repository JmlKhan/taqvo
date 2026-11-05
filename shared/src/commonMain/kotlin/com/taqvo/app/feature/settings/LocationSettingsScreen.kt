package com.taqvo.app.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import com.taqvo.app.design.components.CardDivider
import com.taqvo.app.design.components.RowToggle
import com.taqvo.app.design.components.TaqvoRow
import com.taqvo.app.domain.GeoLocation
import com.taqvo.app.domain.LocationSource
import com.taqvo.app.location.LocationPermission
import com.taqvo.app.location.LocationRepository
import com.taqvo.app.location.rememberLocationPermissionRequester
import com.taqvo.app.resources.Res
import com.taqvo.app.resources.location_choose_city_instead
import com.taqvo.app.resources.location_city
import com.taqvo.app.resources.location_country
import com.taqvo.app.resources.location_denied_note
import com.taqvo.app.resources.location_in_use
import com.taqvo.app.resources.location_privacy_note
import com.taqvo.app.resources.location_timezone
import com.taqvo.app.resources.location_use_my_location
import com.taqvo.app.resources.settings_location
import com.taqvo.app.resources.prayer_current_location
import com.taqvo.app.settings.ZoneIds

/**
 * The timezone is on screen deliberately: when prayer times look wrong by a whole hour, a city
 * whose IANA zone does not match where the user actually is, is almost always the reason, and it
 * is invisible everywhere else in the app.
 */
@Composable
fun LocationSettingsScreen(
    location: GeoLocation?,
    /**
     * The city's name in the interface language, resolved once in `App.kt` and passed down so
     * this screen and the Settings row it is reached from can never disagree. Null when there is
     * no location, or none whose city the bundle knows a name for.
     */
    cityName: String?,
    locationSource: LocationSource,
    locationRepository: LocationRepository,
    onLocationPermission: (LocationPermission) -> Unit,
    onChooseCity: () -> Unit,
    onBack: () -> Unit,
) {
    var permission by remember { mutableStateOf(LocationPermission.NOT_REQUESTED) }
    LaunchedEffect(location) { permission = locationRepository.permission() }

    val requestLocation = rememberLocationPermissionRequester(locationRepository) { granted ->
        permission = granted
        onLocationPermission(granted)
    }

    SettingsScaffold(stringResource(Res.string.settings_location), onBack) {
        SettingsCard {
            TaqvoRow(
                stringResource(Res.string.location_use_my_location),
                // Read from the stored preference, not guessed from the location: a fix taken
                // near a bundled city carries that city's name too, which is why the old
                // guess ("no city name means GPS") read OFF for almost everyone who had it on.
                toggle = RowToggle(locationSource == LocationSource.GPS) { wantsGps ->
                    // There is always a location in force, so switching off is not "no
                    // location" — it is "a city I pick myself", which is the same
                    // destination as the row below. The source flips to MANUAL only when a
                    // city is actually picked, so backing out of the search leaves this on.
                    if (wantsGps) requestLocation() else onChooseCity()
                },
            )
        }

        Spacer(Modifier.height(28.dp))
        SectionLabel(stringResource(Res.string.location_in_use))
        SettingsCard {
            // An em dash for "nothing yet" rather than a translated word: it reads the same in
            // both languages, and a country code or an IANA id is never translated either.
            TaqvoRow(
                stringResource(Res.string.location_city),
                value = cityName ?: stringResource(Res.string.prayer_current_location),
            )
            CardDivider()
            TaqvoRow(stringResource(Res.string.location_country), value = location?.countryCode ?: "-")
            CardDivider()
            // The zone the times are on, under the name it was picked by: Kyiv is named
            // Europe/Kyiv where an old phone draws it in Europe/Kiev, but a zone the phone has no
            // name for at all is named as the phone's own, the clock its times are actually on.
            TaqvoRow(
                stringResource(Res.string.location_timezone),
                value = location?.let { ZoneIds.named(inUse = it.timeZoneId, picked = it.pickedTimeZoneId) } ?: "-",
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsCard {
            TaqvoRow(stringResource(Res.string.location_choose_city_instead), onClick = onChooseCity)
        }

        Spacer(Modifier.height(16.dp))
        SettingsNote(stringResource(Res.string.location_privacy_note))

        if (permission == LocationPermission.DENIED) {
            Spacer(Modifier.height(12.dp))
            SettingsNote(stringResource(Res.string.location_denied_note))
        }
    }
}

package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentMapCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentMapTracker {
    /**
     * Incident ID, latitude, longitude
     */
    val lastLocation: StateFlow<IncidentMapCoordinates>
}

@Singleton
class AppIncidentMapTracker @Inject constructor(
    appPreferences: LocalAppPreferencesDataSource,
    @ApplicationScope externalScope: CoroutineScope,
) : IncidentMapTracker {
    override val lastLocation = appPreferences.userData
        .mapLatest {
            var mapIncident = EmptyIncident.id
            var latitude = 0.0
            var longitude = 0.0
            with(it.casesMapBounds) {
                if (south < north && west < east) {
                    latitude = (south + north) * 0.5
                    longitude = (west + east) * 0.5
                    if (latitude in -90.0..90.0 && longitude in -180.0..180.0) {
                        mapIncident = incidentId
                    } else {
                        latitude = 0.0
                        longitude = 0.0
                    }
                }
                IncidentMapCoordinates(
                    mapIncident,
                    latitude = latitude,
                    longitude = longitude,
                )
            }
        }
        .stateIn(
            externalScope,
            initialValue = IncidentMapCoordinates(
                EmptyIncident.id,
                0.0,
                0.0,
            ),
            started = SharingStarted.WhileSubscribed(),
        )
}

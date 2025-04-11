package com.crisiscleanup.core.data

import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentMapCoordinates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentMapTracker {
    /**
     * Incident ID, latitude, longitude
     */
    val lastLocation: Flow<IncidentMapCoordinates>
}

@Singleton
class AppIncidentMapTracker @Inject constructor(
    appPreferences: LocalAppPreferencesDataSource,
) : IncidentMapTracker {
    override val lastLocation = appPreferences.userData
        .mapLatest {
            var mapIncident = EmptyIncident.id
            var latitude = 0.0
            var longitude = 0.0
            with(it.casesMapBounds) {
                if (incidentId > 0 && south < north && west < east) {
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
}

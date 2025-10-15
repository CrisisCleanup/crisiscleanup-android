package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.BoundedRegionParameters
import com.crisiscleanup.core.model.data.IncidentWorksitesCachePreferences
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Instant

class IncidentCachePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<IncidentCachePreferences>,
) {
    val preferences = dataStore.data.map {
        IncidentWorksitesCachePreferences(
            isPaused = it.isPaused,
            isRegionBounded = it.isRegionBounded,
            BoundedRegionParameters(
                isRegionMyLocation = it.isRegionMyLocation,
                regionLatitude = it.regionLatitude,
                regionLongitude = it.regionLongitude,
                regionRadiusMiles = it.regionRadiusMiles,
            ),
            lastReconciled = Instant.fromEpochSeconds(it.caseReconciliationSeconds),
        )
    }

    /**
     * Updates preferences relating to pausing sync and region syncing
     */
    suspend fun setPauseRegionPreferences(preferences: IncidentWorksitesCachePreferences) {
        dataStore.updateData {
            val regionParameters = preferences.boundedRegionParameters
            it.copy {
                isPaused = preferences.isPaused
                isRegionBounded = preferences.isRegionBounded
                isRegionMyLocation = regionParameters.isRegionMyLocation
                regionLatitude = regionParameters.regionLatitude
                regionLongitude = regionParameters.regionLongitude
                regionRadiusMiles = regionParameters.regionRadiusMiles
            }
        }
    }

    suspend fun setLastReconciled(lastReconciled: Instant) {
        dataStore.updateData {
            it.copy {
                caseReconciliationSeconds = lastReconciled.epochSeconds
            }
        }
    }
}

package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.IncidentWorksitesCachePreferences
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IncidentCachePreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<IncidentCachePreferences>,
) {
    val preferences = dataStore.data.map {
        IncidentWorksitesCachePreferences(
            isPaused = it.isPaused,
            isRegionBounded = it.isRegionBounded,
            isRegionMyLocation = it.isRegionMyLocation,
            regionLatitude = it.regionLatitude,
            regionLongitude = it.regionLongitude,
            regionRadiusMiles = it.regionRadiusMiles,
        )
    }

    suspend fun setPreferences(preferences: IncidentWorksitesCachePreferences) {
        dataStore.updateData {
            it.copy {
                isPaused = preferences.isPaused
                isRegionBounded = preferences.isRegionBounded
                isRegionMyLocation = preferences.isRegionMyLocation
                regionLatitude = preferences.regionLatitude
                regionLongitude = preferences.regionLongitude
                regionRadiusMiles = preferences.regionRadiusMiles
            }
        }
    }
}

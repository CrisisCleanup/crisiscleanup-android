package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.BoundedRegionParameters
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
            BoundedRegionParameters(
                isRegionMyLocation = it.isRegionMyLocation,
                regionLatitude = it.regionLatitude,
                regionLongitude = it.regionLongitude,
                regionRadiusMiles = it.regionRadiusMiles,
            ),
        )
    }

    suspend fun setPreferences(preferences: IncidentWorksitesCachePreferences) {
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
}

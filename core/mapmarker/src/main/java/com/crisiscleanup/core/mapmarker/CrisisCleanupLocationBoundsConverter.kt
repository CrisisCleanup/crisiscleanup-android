package com.crisiscleanup.core.mapmarker

import com.crisiscleanup.core.mapmarker.util.toBounds
import com.crisiscleanup.core.mapmarker.util.toLatLng
import com.crisiscleanup.core.model.data.Location
import com.crisiscleanup.core.model.data.LocationAreaBounds
import com.crisiscleanup.core.model.data.LocationBoundsConverter
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

class CrisisCleanupLocationBoundsConverter @Inject constructor() : LocationBoundsConverter {
    override fun convert(location: Location): LocationAreaBounds {
        val incidentBounds = listOf(location).toLatLng().toBounds()
        return object : LocationAreaBounds {
            override fun isInBounds(latitude: Double, longitude: Double) =
                incidentBounds.containsLocation(LatLng(latitude, longitude))
        }
    }
}

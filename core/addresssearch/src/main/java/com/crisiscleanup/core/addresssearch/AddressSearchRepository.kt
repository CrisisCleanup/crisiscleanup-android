package com.crisiscleanup.core.addresssearch

import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.maps.model.LatLng

interface AddressSearchRepository {
    fun clearCache()

    suspend fun searchAddresses(
        query: String,
        countryCodes: List<String> = emptyList(),
        center: LatLng? = null,
        southwest: LatLng? = null,
        northeast: LatLng? = null,
    ): Collection<LocationAddress>
}
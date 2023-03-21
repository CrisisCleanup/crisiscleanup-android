package com.crisiscleanup.core.addresssearch

import com.crisiscleanup.core.addresssearch.model.KeyLocationAddress
import com.google.android.gms.maps.model.LatLng

interface AddressSearchRepository {
    fun clearCache()

    suspend fun searchAddresses(
        query: String,
        countryCodes: List<String> = emptyList(),
        center: LatLng? = null,
        southwest: LatLng? = null,
        northeast: LatLng? = null,
        maxResults: Int = 8,
    ): List<KeyLocationAddress>
}
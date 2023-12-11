package com.crisiscleanup.core.addresssearch

import com.crisiscleanup.core.addresssearch.model.KeySearchAddress
import com.crisiscleanup.core.model.data.LocationAddress
import com.google.android.gms.maps.model.LatLng

interface AddressSearchRepository {
    fun clearCache()

    suspend fun getAddress(coordinates: LatLng): LocationAddress?

    fun startSearchSession()

    suspend fun searchAddresses(
        query: String,
        countryCodes: List<String> = emptyList(),
        center: LatLng? = null,
        southwest: LatLng? = null,
        northeast: LatLng? = null,
        maxResults: Int = 10,
    ): List<KeySearchAddress>

    suspend fun getPlaceAddress(placeId: String): LocationAddress?
}

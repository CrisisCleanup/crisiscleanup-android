package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.addresssearch.model.KeyLocationAddress
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.mapmarker.model.DefaultCoordinates
import com.crisiscleanup.core.model.data.LocationAddress
import com.crisiscleanup.feature.caseeditor.model.LocationInputData
import com.crisiscleanup.feature.caseeditor.model.asCaseLocation
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicReference

internal class LocationSearchManager(
    private val incidentId: Long,
    private val worksiteProvider: EditableWorksiteProvider,
    locationInputData: LocationInputData,
    searchWorksitesRepository: SearchWorksitesRepository,
    locationProvider: LocationProvider,
    private val addressSearchRepository: AddressSearchRepository,
    iconProvider: MapCaseIconProvider,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val querySearchThresholdLength: Int = 3,
) {
    private val activeQuery = AtomicReference("")

    private val isSearchingWorksites = MutableStateFlow(false)
    private val isSearchingAddresses = MutableStateFlow(false)
    private val isSearchingCoordinateAddress = MutableStateFlow(false)
    val isSearching = combine(
        isSearchingWorksites,
        isSearchingAddresses,
        isSearchingCoordinateAddress,
    ) { b0, b1, b2 -> b0 || b1 || b2 }

    private val searchQuery = locationInputData.locationQuery
        .debounce(100)
        .map(String::trim)
        .distinctUntilChanged()
        .onEach {
            synchronized(activeQuery) {
                activeQuery.set(it)
            }
        }
        .filter { it.length >= querySearchThresholdLength }

    private val worksitesSearch = searchQuery
        .map { q ->
            isSearchingWorksites.value = true
            try {
                val worksitesSearch =
                    searchWorksitesRepository.locationSearchWorksites(incidentId, q)
                val worksites = worksitesSearch.map { it.asCaseLocation(iconProvider) }
                Pair(q, worksites)
            } finally {
                synchronized(activeQuery) {
                    if (activeQuery.get() == q) {
                        isSearchingWorksites.value = false
                    }
                }
            }
        }
        .flowOn(coroutineDispatcher)

    private val oneMinute = 1 / 60

    private val addressSearch = searchQuery
        .map { q ->
            isSearchingAddresses.value = true
            try {
                val incidentBounds = worksiteProvider.incidentBounds

                var center: LatLng? = null
                locationProvider.coordinates?.let {
                    val deviceLocation = LatLng(it.first, it.second)
                    if (incidentBounds.containsLocation(deviceLocation)) {
                        center = deviceLocation
                    }
                }
                if (center == null && incidentBounds.centroid != DefaultCoordinates) {
                    center = incidentBounds.centroid
                }

                var searchSw: LatLng? = null
                var searchNe: LatLng? = null
                val boundsSw = incidentBounds.bounds.southwest
                val boundsNe = incidentBounds.bounds.northeast
                if (boundsNe.latitude - boundsSw.latitude > oneMinute &&
                    (boundsSw.longitude + 360 - boundsNe.longitude > oneMinute)
                ) {
                    searchSw = boundsSw
                    searchNe = boundsNe
                }

                val addresses = addressSearchRepository.searchAddresses(
                    q,
                    countryCodes = listOf("US"),
                    center = center,
                    southwest = searchSw,
                    northeast = searchNe,
                )
                Pair(q, addresses)
            } finally {
                synchronized(activeQuery) {
                    if (activeQuery.get() == q) {
                        isSearchingAddresses.value = false
                    }
                }
            }
        }
        .flowOn(coroutineDispatcher)

    val searchResults = combine(
        searchQuery,
        worksitesSearch,
        addressSearch,
    ) { q, worksiteResults, addressResults ->
        val addresses =
            if (addressResults.first == q) {
                addressResults.second
            } else {
                emptyList()
            }
        val worksites =
            if (worksiteResults.first == q) {
                worksiteResults.second
            } else {
                emptyList()
            }
        LocationSearchResults(q, addresses, worksites)
    }

    suspend fun queryAddress(coordinates: LatLng): LocationAddress? {
        isSearchingCoordinateAddress.value = true
        try {
            return addressSearchRepository.getAddress(coordinates)
        } finally {
            isSearchingCoordinateAddress.value = false
        }
    }
}

data class LocationSearchResults(
    val query: String,
    val addresses: List<KeyLocationAddress>,
    val worksites: List<CaseSummaryResult>,
) {
    val isEmpty = addresses.isEmpty() && worksites.isEmpty()
}

package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.addresssearch.AddressSearchRepository
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.model.data.LocationAddress
import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearch
import com.crisiscleanup.feature.caseeditor.model.ExistingCaseLocation
import com.crisiscleanup.feature.caseeditor.model.LocationInputData
import com.crisiscleanup.feature.caseeditor.model.asCaseLocation
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference

internal class LocationSearchManager(
    private val incidentId: Long,
    locationInputData: LocationInputData,
    searchWorksitesRepository: SearchWorksitesRepository,
    locationProvider: LocationProvider,
    addressSearchRepository: AddressSearchRepository,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    minAddressQueryLength: Int = 4,
) {
    private val activeQuery = AtomicReference("")

    private val isSearchingWorksites = MutableStateFlow(false)
    private val isSearchingAddresses = MutableStateFlow(false)
    val isSearching = combine(
        isSearchingWorksites,
        isSearchingAddresses,
    ) { b1, b2 ->
        b1 || b2
    }

    private val searchQuery = locationInputData.locationQuery
        .debounce(100)
        .map(String::trim)
        .distinctUntilChanged()
        .map {
            activeQuery.set(it)
            it
        }
        .filter { it.length >= minAddressQueryLength }

    private val worksitesSearch = searchQuery
        .map { q ->
            isSearchingWorksites.value = true
            try {
                val worksitesSearch =
                    searchWorksitesRepository.locationSearchWorksites(incidentId, q)
                val worksites =
                    worksitesSearch.map(NetworkWorksiteLocationSearch::asCaseLocation)
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

    private val addressSearch = searchQuery
        .map { q ->
            isSearchingAddresses.value = true
            try {
                var center: LatLng? = null
                locationProvider.coordinates?.let {
                    center = LatLng(it.first, it.second)
                }

                val addresses = addressSearchRepository.searchAddresses(
                    q,
                    center = center,
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
            if (addressResults.first == q) addressResults.second
            else emptyList()
        val worksites =
            if (worksiteResults.first == q) worksiteResults.second
            else emptyList()
        LocationSearchResults(q, addresses, worksites)
    }
}

data class LocationSearchResults(
    val query: String,
    val addresses: Collection<LocationAddress>,
    val worksites: Collection<ExistingCaseLocation>,
) {
    val isEmpty = addresses.isEmpty() && worksites.isEmpty()
}


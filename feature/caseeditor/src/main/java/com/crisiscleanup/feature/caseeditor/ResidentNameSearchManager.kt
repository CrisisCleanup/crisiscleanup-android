package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import com.crisiscleanup.feature.caseeditor.model.asCaseLocation
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

class ResidentNameSearchManager(
    private val incidentId: Long,
    propertyInputData: PropertyInputData,
    searchWorksitesRepository: SearchWorksitesRepository,
    iconProvider: MapCaseIconProvider,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val querySearchThresholdLength: Int = 3,
    private val ignoreNetworkId: Long = -1,
    disableNameSearch: Boolean = false,
) {
    private val steadyStateName = AtomicReference(propertyInputData.residentName.value.trim())

    private val activeQuery = AtomicReference("")

    private val isSearchingWorksites = MutableStateFlow(false)

    private var stopSearching = MutableStateFlow(disableNameSearch)

    private val searchQuery = propertyInputData.residentName
        .debounce(100)
        .filter { !stopSearching.value }
        .map(String::trim)
        .filter { it != steadyStateName.get() }
        .distinctUntilChanged()
        .onEach {
            synchronized(activeQuery) {
                activeQuery.set(it)
            }
        }

    private val worksitesSearch = searchQuery
        .filter { it.length >= querySearchThresholdLength }
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

    val searchResults = combine(
        stopSearching,
        searchQuery,
        worksitesSearch,
    ) { stop, q, worksiteResults ->
        val isValid = !stop && q.isNotBlank() && q.contains(worksiteResults.first)
        val worksites = if (isValid) {
            if (ignoreNetworkId > 0) {
                worksiteResults.second.filter { it.networkWorksiteId != ignoreNetworkId }
            } else {
                worksiteResults.second
            }
        } else emptyList()
        ResidentNameSearchResults(q, worksites)
    }

    fun stopSearchingWorksites() {
        stopSearching.value = true
    }

    fun updateSteadyStateName(name: String) {
        steadyStateName.set(name.trim())
    }
}

data class ResidentNameSearchResults(
    val query: String,
    val worksites: List<CaseSummaryResult>,
) {
    val isEmpty = worksites.isEmpty()
    val isNotEmpty = !isEmpty
}

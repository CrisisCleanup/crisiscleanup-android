package com.crisiscleanup.feature.caseeditor

import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.feature.caseeditor.model.PropertyInputData
import com.crisiscleanup.feature.caseeditor.model.asCaseLocation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicReference

class ResidentNameSearchManager(
    private val incidentId: Long,
    propertyInputData: PropertyInputData,
    searchWorksitesRepository: SearchWorksitesRepository,
    iconProvider: MapCaseIconProvider,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val querySearchThresholdLength: Int = 3,
) {
    private val initialName = propertyInputData.residentName.value
    private var isInitialQuery = true

    private val activeQuery = AtomicReference("")

    private val isSearchingWorksites = MutableStateFlow(false)

    private var stopSearching = MutableStateFlow(false)

    private val searchQuery = propertyInputData.residentName
        .debounce(100)
        .map(String::trim)
        .map {
            if (it == initialName && isInitialQuery || stopSearching.value) {
                if (isInitialQuery) {
                    isInitialQuery = false
                }
                return@map ""
            }
            it
        }
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
        val worksites = if (isValid) worksiteResults.second
        else emptyList()
        ResidentNameSearchResults(q, worksites)
    }

    fun stopSearchingWorksites() {
        stopSearching.value = true
    }
}

data class ResidentNameSearchResults(
    val query: String,
    val worksites: List<CaseSummaryResult>,
) {
    val isEmpty = worksites.isEmpty()
    val isNotEmpty = !isEmpty
}


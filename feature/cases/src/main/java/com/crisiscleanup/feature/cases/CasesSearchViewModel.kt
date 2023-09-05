package com.crisiscleanup.feature.cases

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CasesSearchViewModel @Inject constructor(
    private val incidentSelector: IncidentSelector,
    private val worksitesRepository: WorksitesRepository,
    private val searchWorksitesRepository: SearchWorksitesRepository,
    databaseManagementRepository: AppDataManagementRepository,
    private val mapCaseIconProvider: MapCaseIconProvider,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val isInitialLoading = MutableStateFlow(true)
    private val isSearching = MutableStateFlow(false)
    private val isSearchingLocal = MutableStateFlow(false)
    private val isCombiningResults = MutableStateFlow(false)
    val isSelectingResult = MutableStateFlow(false)
    val isLoading = combine(
        isInitialLoading,
        isSearching,
        isSearchingLocal,
        isCombiningResults,
        isSelectingResult,
    ) { b0, b1, b2, b3, b4 -> b0 || b1 || b2 || b3 || b4 }
        .stateIn(
            scope = viewModelScope,
            initialValue = true,
            started = SharingStarted.WhileSubscribed(),
        )

    val selectedWorksite = MutableStateFlow(Pair(EmptyIncident.id, EmptyWorksite.id))

    val recentWorksites = incidentSelector.incidentId
        .flatMapLatest { incidentId ->
            try {
                if (incidentId > 0) {
                    worksitesRepository.streamRecentWorksites(incidentId)
                        .mapLatest { list ->
                            list.map { summary ->
                                CaseSummaryResult(
                                    summary,
                                    getIcon(summary.workType),
                                    listItemKey = summary.id,
                                )
                            }
                        }
                } else {
                    flowOf(emptyList())
                }
            } finally {
                isInitialLoading.value = false
            }
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(),
        )

    val searchQuery = MutableStateFlow("")
    private val incidentSearchQuery = combine(
        incidentSelector.incidentId,
        searchQuery
            .debounce(200)
            .map(String::trim)
            .distinctUntilChanged(),
        ::Pair,
    )
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 0,
        )

    private val networkSearchResults = incidentSearchQuery
        .map { (incidentId, q) ->
            if (incidentId != EmptyIncident.id) {
                if (q.length < 3) {
                    return@map CasesSearchResults(q)
                }

                isSearching.value = true
                try {
                    val results = searchWorksitesRepository.searchWorksites(incidentId, q)
                    val options = results.map { summary ->
                        CaseSummaryResult(
                            summary,
                            getIcon(summary.workType),
                        )
                    }
                    return@map CasesSearchResults(q, false, options)
                } catch (e: Exception) {
                    logger.logException(e)
                } finally {
                    // TODO Does this pattern work with cancellations?
                    isSearching.value = false
                }
            }
            CasesSearchResults(q, false)
        }
        .flowOn(ioDispatcher)

    private val localSearchResults = incidentSearchQuery
        .map { (incidentId, q) ->
            if (incidentId != EmptyIncident.id) {
                if (q.length < 2) {
                    return@map CasesSearchResults(q)
                }

                isSearchingLocal.value = true
                try {
                    val results = searchWorksitesRepository.getMatchingLocalWorksites(incidentId, q)
                    val options = results.map { summary ->
                        CaseSummaryResult(
                            summary,
                            getIcon(summary.workType),
                            listItemKey = if (summary.networkId > 0) summary.networkId else -summary.id,
                        )
                    }
                    return@map CasesSearchResults(q, false, options)
                } catch (e: Exception) {
                    logger.logException(e)
                } finally {
                    // TODO Does this pattern work with cancellations?
                    isSearchingLocal.value = false
                }
            }
            CasesSearchResults(q, false)
        }
        .flowOn(ioDispatcher)

    val searchResults = combine(
        incidentSearchQuery,
        localSearchResults,
        networkSearchResults,
        ::Triple,
    )
        .filter { (incidentQ, localResults, networkResults) ->
            val q = incidentQ.second
            q == localResults.q || q == networkResults.q
        }
        .map { (incidentQ, localResults, networkResults) ->
            val q = incidentQ.second
            isCombiningResults.value = true
            try {
                val hasLocalResults = q == localResults.q
                val hasNetworkResults = q == networkResults.q
                val options = if (hasLocalResults && hasNetworkResults) {
                    val localResultIdIndex = localResults.options.mapIndexed { index, option ->
                        Pair(index, option)
                    }.associateBy { it.second.listItemKey }

                    val results = localResults.options.toMutableList()
                    val combined = mutableListOf<CaseSummaryResult>()
                    networkResults.options.forEach { networkResult ->
                        val matchingLocal = localResultIdIndex[networkResult.listItemKey]
                        if (matchingLocal == null) {
                            combined.add(networkResult)
                        } else {
                            results[matchingLocal.first] = matchingLocal.second
                        }
                    }
                    combined.addAll(results)

                    combined
                } else if (hasLocalResults) {
                    localResults.options
                } else if (hasNetworkResults) {
                    networkResults.options
                } else {
                    emptyList()
                }

                CasesSearchResults(q, false, options)
            } finally {
                isCombiningResults.value = false
            }
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = CasesSearchResults(),
            started = SharingStarted.WhileSubscribed(),
        )

    init {
        viewModelScope.launch(ioDispatcher) {
            databaseManagementRepository.rebuildFts()
        }
    }

    private fun getIcon(workType: WorkType?) = workType?.let {
        mapCaseIconProvider.getIconBitmap(it.statusClaim, it.workType, false)
    }

    fun onBack(): Boolean {
        if (searchQuery.value.isNotEmpty()) {
            searchQuery.value = ""
            return false
        }

        return true
    }

    fun onSelectWorksite(result: CaseSummaryResult) {
        viewModelScope.launch(ioDispatcher) {
            if (isSelectingResult.value) {
                return@launch
            }
            isSelectingResult.value = true
            try {
                val incidentId = incidentSelector.incidentId.value
                val worksiteId = with(result) {
                    if (summary.id > 0) {
                        summary.id
                    } else {
                        worksitesRepository.getLocalId(networkWorksiteId)
                    }
                }
                selectedWorksite.value = Pair(incidentId, worksiteId)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isSelectingResult.value = false
            }
        }
    }

    fun clearSelection() {
        selectedWorksite.value = Pair(EmptyIncident.id, EmptyWorksite.id)
    }
}

data class CasesSearchResults(
    val q: String = "",
    val isShortQ: Boolean = true,
    val options: List<CaseSummaryResult> = emptyList(),
)

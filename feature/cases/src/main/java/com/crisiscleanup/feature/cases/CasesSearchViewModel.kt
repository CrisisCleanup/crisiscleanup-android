package com.crisiscleanup.feature.cases

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.combine
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.throttleLatest
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.SearchWorksitesRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.mapmarker.MapCaseIconProvider
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorksiteSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
import kotlinx.coroutines.flow.combine as kCombine

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
    private val isSearchingNetwork = MutableStateFlow(false)
    private val isSearchingLocal = MutableStateFlow(false)
    private val isCombiningResults = MutableStateFlow(false)
    val isSelectingResult = MutableStateFlow(false)
    val isSearching = kCombine(
        isSearchingLocal,
        isSearchingNetwork,
        ::Pair,
    )
        .map { (b0, b1) -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val isLoading = combine(
        isInitialLoading,
        isSearching,
        isCombiningResults,
        isSelectingResult,
    ) { b0, b1, b2, b3 -> b0 || b1 || b2 || b3 }
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
    private val incidentSearchQuery = kCombine(
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
        .throttleLatest(150)
        .mapLatest { (incidentId, q) ->
            if (incidentId != EmptyIncident.id) {
                if (q.length < 3) {
                    return@mapLatest CasesSearchResults(q)
                }

                isSearchingNetwork.value = true
                try {
                    val results = searchWorksitesRepository.searchWorksites(incidentId, q)
                    val options = results.map { summary ->
                        CaseSummaryResult(
                            summary,
                            getIcon(summary.workType),
                        )
                    }
                    return@mapLatest CasesSearchResults(q, false, options)
                } catch (e: Exception) {
                    logger.logException(e)
                } finally {
                    // TODO Does this pattern work with cancellations?
                    isSearchingNetwork.value = false
                }
            }
            CasesSearchResults(q, false)
        }
        .flowOn(ioDispatcher)

    private val localSearchResults = incidentSearchQuery
        .throttleLatest(150)
        .mapLatest { (incidentId, q) ->
            if (incidentId != EmptyIncident.id) {
                if (q.length < 2) {
                    return@mapLatest CasesSearchResults(q)
                }

                isSearchingLocal.value = true
                try {
                    val results = searchWorksitesRepository.getMatchingLocalWorksites(incidentId, q)
                    var options = results.map { summary -> summary.asCaseSummary(this::getIcon) }

                    var leadingCaseSummary: CaseSummaryResult? = null
                    if (options.isNotEmpty()) {
                        searchWorksitesRepository.getWorksiteByCaseNumber(incidentId, q.trim())
                            ?.let { caseNumberMatch ->
                                if (options.first().summary.id != caseNumberMatch.id) {
                                    leadingCaseSummary =
                                        caseNumberMatch.asCaseSummary(this::getIcon)
                                }
                            }
                    }
                    leadingCaseSummary?.let { option ->
                        options = options
                            .filter { it.summary.id != option.summary.id }
                            .toMutableList()
                            .also { it.add(0, option) }
                    }

                    return@mapLatest CasesSearchResults(q, false, options)
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
        isSearching,
        localSearchResults,
        networkSearchResults,
    ) {
            incidentQ,
            searching,
            localResults,
            networkResults,
        ->
        Triple(
            incidentQ,
            searching,
            Pair(localResults, networkResults),
        )
    }
        .filter { (incidentQ, _, searchResults) ->
            val q = incidentQ.second
            val (localResults, networkResults) = searchResults
            q == localResults.q || q == networkResults.q
        }
        .mapLatest { (incidentQ, searching, searchResults) ->
            val q = incidentQ.second
            val (localResults, networkResults) = searchResults
            isCombiningResults.value = true
            var isShortQ = false
            try {
                val hasLocalResults = q == localResults.q
                val hasNetworkResults = q == networkResults.q
                val options = if (hasLocalResults && hasNetworkResults) {
                    val localResultIdIndex = localResults.options.mapIndexed { index, option ->
                        Pair(index, option)
                    }.associateBy { it.second.listItemKey }

                    val localOptions = localResults.options
                    val qLower = q.trim().lowercase()
                    val firstCaseNumberLower =
                        localOptions.firstOrNull()?.summary?.caseNumber?.lowercase()
                    val hasCaseNumberMatch = qLower == firstCaseNumberLower

                    val combined = mutableListOf<CaseSummaryResult>()
                    val localCombined = mutableSetOf<Long>()
                    networkResults.options.forEach { networkResult ->
                        if (!hasCaseNumberMatch || networkResult.summary.caseNumber.lowercase() != qLower) {
                            val matchingLocal = localResultIdIndex[networkResult.listItemKey]
                            if (matchingLocal == null) {
                                combined.add(networkResult)
                            } else {
                                combined.add(matchingLocal.second)
                                localCombined.add(matchingLocal.second.summary.id)
                            }
                        }
                    }

                    val caseNumberMatch = if (hasCaseNumberMatch) localOptions.first() else null
                    val ignoreLocalId = caseNumberMatch?.summary?.id
                    val localNotCombined = localOptions.filter {
                        it.summary.id != ignoreLocalId &&
                                !localCombined.contains(it.summary.id)
                    }
                    combined.addAll(localNotCombined)

                    caseNumberMatch?.let { combined.add(0, it) }

                    isShortQ = localResults.isShortQ && networkResults.isShortQ
                    combined
                } else if (hasLocalResults) {
                    isShortQ = localResults.isShortQ
                    localResults.options
                } else if (hasNetworkResults) {
                    isShortQ = networkResults.isShortQ
                    networkResults.options
                } else {
                    emptyList()
                }

                CasesSearchResults(q, isShortQ && !searching, options)
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

private fun WorksiteSummary.asCaseSummary(getIcon: (WorkType?) -> Bitmap?) =
    CaseSummaryResult(
        this,
        getIcon(workType),
        listItemKey = if (networkId > 0) networkId else -id,
    )
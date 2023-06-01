package com.crisiscleanup.feature.cases.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.commoncase.model.CaseSummaryResult
import com.crisiscleanup.core.data.IncidentSelector
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CasesSearchViewModel @Inject constructor(
    private val incidentSelector: IncidentSelector,
    private val worksitesRepository: WorksitesRepository,
    private val searchWorksitesRepository: SearchWorksitesRepository,
    private val mapCaseIconProvider: MapCaseIconProvider,
    private val translator: KeyTranslator,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val isInitialLoading = MutableStateFlow(true)
    private val isSearching = MutableStateFlow(false)
    val isSelectingResult = MutableStateFlow(false)
    val isLoading = combine(
        isInitialLoading,
        isSearching,
        isSelectingResult,
    ) { b0, b1, b2 -> b0 || b1 || b2 }
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
    val searchResults = combine(
        incidentSelector.incidentId,
        searchQuery
            .debounce(200)
            .map(String::trim)
            .distinctUntilChanged(),
        ::Pair
    )
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
                    isSearching.value = false
                }
            }
            CasesSearchResults(q, false)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = CasesSearchResults(),
            started = SharingStarted.WhileSubscribed(),
        )

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
                    if (summary.id > 0) summary.id
                    else worksitesRepository.getLocalId(incidentId, networkWorksiteId)
                }
                selectedWorksite.value = Pair(incidentId, worksiteId)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isSelectingResult.value = false
            }
        }
    }

    fun translate(key: String) = translator.translate(key) ?: key
}

data class CasesSearchResults(
    val q: String = "",
    val isShortQ: Boolean = true,
    val options: List<CaseSummaryResult> = emptyList(),
)
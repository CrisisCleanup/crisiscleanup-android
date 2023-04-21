package com.crisiscleanup.feature.cases.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CasesSearchViewModel @Inject constructor(
    private val incidentSelector: IncidentSelector,
    private val worksitesRepository: WorksitesRepository,
    private val searchWorksitesRepository: SearchWorksitesRepository,
    private val mapCaseIconProvider: MapCaseIconProvider,
    @Logger(CrisisCleanupLoggers.Cases) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val isSearching = MutableStateFlow(false)
    val isSelectingResult = MutableStateFlow(false)
    val isLoading = combine(
        isSearching,
        isSelectingResult,
    ) { b0, b1 -> b0 || b1 }
        .stateIn(
            scope = viewModelScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    val selectedWorksite = MutableStateFlow(Pair(EmptyIncident.id, EmptyWorksite.id))

    val recentWorksites = incidentSelector.incidentId
        .flatMapLatest { incidentId ->
            if (incidentId > 0) {
                worksitesRepository.streamRecentWorksites(incidentId)
                    .mapLatest { list ->
                        list.map { summary ->
                            CaseSummaryResult(
                                summary,
                                getIcon(summary.workType),
                            )
                        }
                    }
            } else {
                flowOf(emptyList())
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
            .distinctUntilChanged()
            .filter { it.length > 2 },
        ::Pair
    )
        .map { (incidentId, q) ->
            if (incidentId != EmptyIncident.id) {
                isSearching.value = true
                try {
                    val results = searchWorksitesRepository.searchWorksites(incidentId, q)
                    return@map results.map { summary ->
                        CaseSummaryResult(
                            summary,
                            getIcon(summary.workType),
                        )
                    }
                } catch (e: Exception) {
                    logger.logException(e)
                } finally {
                    isSearching.value = false
                }
            }
            emptyList()
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
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

    fun onSelectWorksite(networkWorksiteId: Long) {
        viewModelScope.launch(ioDispatcher) {
            isSelectingResult.value = true
            try {
                val incidentId = incidentSelector.incidentId.value
                val worksiteId = worksitesRepository.getLocalId(incidentId, networkWorksiteId)
                selectedWorksite.value = Pair(incidentId, worksiteId)
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isSelectingResult.value = false
            }
        }
    }
}
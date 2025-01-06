package com.crisiscleanup.feature.crisiscleanuplists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.ListDataRefresher
import com.crisiscleanup.core.data.repository.ListsRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    incidentSelector: IncidentSelector,
    private val listDataRefresher: ListDataRefresher,
    private val listsRepository: ListsRepository,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val currentIncident = incidentSelector.incident

    val incidentLists = incidentSelector.incident
        .filter { it != EmptyIncident }
        .flatMapLatest {
            listsRepository.streamIncidentLists(it.id)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = emptyList(),
            started = SharingStarted.WhileSubscribed(3_000),
        )

    val isRefreshingData = MutableStateFlow(false)

    val allLists = listsRepository.pageLists()
        .flowOn(ioDispatcher)
        .cachedIn(viewModelScope)

    private val isListsRefreshed = MutableStateFlow(false)

    val openAllListsTab = combine(
        isListsRefreshed,
        incidentSelector.incidentId,
        ::Pair,
    )
        .filter { (isRefreshed, incidentId) ->
            isRefreshed && incidentId != EmptyIncident.id
        }
        .map { (_, incidentId) ->
            if (listsRepository.getIncidentListCount(incidentId) == 0) {
                return@map true
            }
            false
        }
        .distinctUntilChanged()
        .flowOn(ioDispatcher)

    init {
        viewModelScope.launch(ioDispatcher) {
            refreshLists()
        }
    }

    suspend fun refreshLists(force: Boolean = false) {
        if (!isRefreshingData.compareAndSet(expect = false, update = true)) {
            return
        }

        try {
            listDataRefresher.refreshListData(force)

            isListsRefreshed.value = true
        } finally {
            isRefreshingData.value = false
        }
    }
}

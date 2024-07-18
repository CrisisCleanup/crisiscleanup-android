package com.crisiscleanup.feature.crisiscleanuplists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifier
import com.crisiscleanup.core.data.model.ExistingWorksiteIdentifierNone
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.ListsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.domain.LoadSelectIncidents
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyList
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.ListModel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.crisiscleanuplists.navigation.ViewListArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    listsRepository: ListsRepository,
    private val incidentsRepository: IncidentsRepository,
    accountDataRepository: AccountDataRepository,
    private val incidentSelector: IncidentSelector,
    appPreferencesRepository: LocalAppPreferencesRepository,
    private val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Lists) private val logger: AppLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val viewListArgs = ViewListArgs(savedStateHandle)

    private val listId = viewListArgs.listId

    private val loadSelectIncidents = LoadSelectIncidents(
        incidentsRepository = incidentsRepository,
        accountDataRepository = accountDataRepository,
        incidentSelector = incidentSelector,
        appPreferencesRepository = appPreferencesRepository,
        coroutineScope = viewModelScope,
    )

    val viewState = listsRepository.streamList(listId)
        .mapLatest { list ->
            if (list == EmptyList) {
                val listNotFound =
                    translator("list.not_found_deleted")
                return@mapLatest ViewListViewState.Error(listNotFound)
            }

            val lookup = listsRepository.getListObjectData(list)
            var objectIds = list.objectIds
            if (list.model == ListModel.List) {
                objectIds = objectIds.filter { it != list.networkId }
            }
            val objectData = objectIds.map { id ->
                lookup[id]
            }
            ViewListViewState.Success(list, objectData)
        }
        .flowOn(ioDispatcher)
        .stateIn(
            scope = viewModelScope,
            initialValue = ViewListViewState.Loading,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    val screenTitle = viewState.map {
        (it as? ViewListViewState.Success)?.list?.let { list ->
            return@map list.name
        }

        translator("list.list")
    }
        .stateIn(
            scope = viewModelScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(3_000),
        )

    var isConfirmingOpenWorksite by mutableStateOf(false)
        private set
    var openWorksiteId by mutableStateOf(ExistingWorksiteIdentifierNone)
    var openWorksiteError by mutableStateOf("")

    var isChangingIncident by mutableStateOf(false)
        private set
    private var openWorksiteChangeIncident = EmptyIncident
    private var pendingOpenWorksite = EmptyWorksite
    var changeIncidentConfirmMessage by mutableStateOf("")
        private set

    init {
        viewModelScope.launch(ioDispatcher) {
            listsRepository.refreshList(listId)
        }
    }

    fun onConfirmChangeIncident() {
        if (isChangingIncident) {
            return
        }

        val changeIncident = openWorksiteChangeIncident
        val changeWorksite = pendingOpenWorksite
        try {
            if (changeIncident == EmptyIncident ||
                changeWorksite == EmptyWorksite
            ) {
                return
            }
        } finally {
            clearChangeIncident()
        }

        isChangingIncident = true
        viewModelScope.launch(ioDispatcher) {
            try {
                loadSelectIncidents.persistIncident(changeIncident)
                openWorksiteId = ExistingWorksiteIdentifier(changeIncident.id, changeWorksite.id)
            } finally {
                isChangingIncident = false
            }
        }
    }

    fun clearChangeIncident() {
        openWorksiteChangeIncident = EmptyIncident
        pendingOpenWorksite = EmptyWorksite
        changeIncidentConfirmMessage = ""
    }

    fun onOpenWorksite(worksite: Worksite) {
        if (worksite == EmptyWorksite ||
            isConfirmingOpenWorksite ||
            isChangingIncident
        ) {
            return
        }
        isConfirmingOpenWorksite = true

        viewModelScope.launch(ioDispatcher) {
            try {
                (viewState.value as? ViewListViewState.Success)?.list.let { list ->
                    val targetIncidentId = worksite.incidentId
                    if (list?.incident?.id == targetIncidentId) {
                        val targetWorksiteId = ExistingWorksiteIdentifier(
                            incidentId = targetIncidentId,
                            worksiteId = worksite.id,
                        )
                        if (targetIncidentId == incidentSelector.incident.value.id) {
                            openWorksiteId = targetWorksiteId
                        } else {
                            val cachedIncident = incidentsRepository.getIncident(targetIncidentId)
                            if (cachedIncident == null) {
                                openWorksiteError =
                                    translator("list.incident_not_downloaded_error")
                            } else {
                                openWorksiteChangeIncident = cachedIncident
                                pendingOpenWorksite = worksite
                                changeIncidentConfirmMessage =
                                    translator("list.change_incident_confirm")
                                        .replace("{incident_name}", cachedIncident.shortName)
                                        .replace("{case_number}", worksite.caseNumber)
                            }
                        }
                    } else {
                        openWorksiteError =
                            translator("list.case_number_not_in_this_incident")
                                .replace("{case_number}", worksite.caseNumber)
                                .replace("{incident_name}", list?.incident?.shortName ?: "")
                                .trim()
                    }
                }
            } catch (e: Exception) {
                logger.logException(e)
            } finally {
                isConfirmingOpenWorksite = false
            }
        }
    }
}

sealed interface ViewListViewState {
    data object Loading : ViewListViewState

    data class Success(
        val list: CrisisCleanupList,
        val objectData: List<Any?>,
    ) : ViewListViewState

    data class Error(
        val message: String,
    ) : ViewListViewState
}

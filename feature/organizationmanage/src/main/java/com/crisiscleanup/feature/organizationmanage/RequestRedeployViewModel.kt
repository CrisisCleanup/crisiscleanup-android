package com.crisiscleanup.feature.organizationmanage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AccountDataRefresher
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.RequestRedeployRepository
import com.crisiscleanup.core.model.data.EmptyIncidentIdNameType
import com.crisiscleanup.core.model.data.IncidentIdNameType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestRedeployViewModel @Inject constructor(
    private val incidentsRepository: IncidentsRepository,
    accountDataRepository: AccountDataRepository,
    accountDataRefresher: AccountDataRefresher,
    private val requestRedeployRepository: RequestRedeployRepository,
    private val translator: KeyResourceTranslator,
    @Logger(CrisisCleanupLoggers.Account) private val logger: AppLogger,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val requestedIncidentsStream = MutableStateFlow<Set<Long>>(emptySet())
    private val incidentsStream = MutableStateFlow<List<IncidentIdNameType>?>(null)
    val viewState = combine(
        incidentsStream,
        accountDataRepository.accountData,
        requestedIncidentsStream,
        ::Triple,
    )
        .filter { (incidents, _, _) -> incidents != null }
        .mapLatest { (incidents, accountData, requestedIds) ->
            val approvedIds = accountData.approvedIncidents
            RequestRedeployViewState.Ready(incidents!!, approvedIds, requestedIds)
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = RequestRedeployViewState.Loading,
            started = SharingStarted.WhileSubscribed(),
        )

    val isRequestingRedeploy = MutableStateFlow(false)
    var isRedeployRequested by mutableStateOf(false)
        private set
    var redeployErrorMessage by mutableStateOf("")
        private set

    val isTransient = combine(
        viewState,
        isRequestingRedeploy,
        ::Pair,
    )
        .mapLatest { (state, b0) -> state == RequestRedeployViewState.Loading || b0 }

    init {
        viewModelScope.launch(ioDispatcher) {
            accountDataRefresher.updateApprovedIncidents(true)

            requestedIncidentsStream.value = requestRedeployRepository.getRequestedIncidents()

            incidentsStream.value = incidentsRepository.getIncidentsList()
        }
    }

    fun requestRedeploy(incident: IncidentIdNameType) {
        if (incident == EmptyIncidentIdNameType) {
            return
        }

        if (isRequestingRedeploy.value) {
            return
        }
        isRequestingRedeploy.value = true

        redeployErrorMessage = ""

        viewModelScope.launch(ioDispatcher) {
            try {
                if (requestRedeployRepository.requestRedeploy(incident.id)) {
                    isRedeployRequested = true
                } else {
                    // TODO More informative error state where possible
                    redeployErrorMessage =
                        translator("info.redeploy_request_failed")
                            .replace("{incident_name}", incident.shortName)
                }
            } catch (e: Exception) {
                logger.logException(e)
                redeployErrorMessage =
                    translator("info.redeploy_request_failure")
            } finally {
                isRequestingRedeploy.value = false
            }
        }
    }
}

sealed interface RequestRedeployViewState {
    data object Loading : RequestRedeployViewState
    data class Ready(
        val incidents: List<IncidentIdNameType>,
        val approvedIncidentIds: Set<Long>,
        val requestedIncidentIds: Set<Long>,
    ) : RequestRedeployViewState
}

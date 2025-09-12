package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.subscribedReplay
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.AppPreferencesRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import com.crisiscleanup.core.model.data.IncidentsData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentSelector {
    val incidentId: StateFlow<Long>

    val incident: StateFlow<Incident>

    val data: StateFlow<IncidentsData>

    fun selectIncident(incident: Incident)
    suspend fun submitIncidentChange(incident: Incident): Boolean
}

@Singleton
class IncidentSelectManager @Inject constructor(
    incidentsRepository: IncidentsRepository,
    accountDataRepository: AccountDataRepository,
    private val appPreferencesRepository: AppPreferencesRepository,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) : IncidentSelector {
    private val incidentsSource = combine(
        incidentsRepository.incidents,
        accountDataRepository.accountData,
        ::Pair,
    )
        .mapLatest { (incidents, accountData) ->
            accountData.filterApproved(incidents)
        }

    private val preferencesIncidentId =
        appPreferencesRepository.preferences.map {
            it.selectedIncidentId
        }

    private val selectedIncident = combine(
        preferencesIncidentId,
        incidentsSource,
        ::Pair,
    )
        .map { (selectedId, incidents) ->
            incidents.firstOrNull { it.id == selectedId }
                ?: EmptyIncident
        }

    override val data = combine(
        incidentsSource,
        selectedIncident,
        ::Pair,
    )
        .map { (incidents, selected) ->
            if (incidents.isEmpty()) {
                IncidentsData.Empty
            } else {
                IncidentsData.Incidents(incidents, selected)
            }
        }.stateIn(
            scope = coroutineScope,
            initialValue = IncidentsData.Loading,
            started = subscribedReplay(),
        )

    override var incident = data.mapNotNull {
        (it as? IncidentsData.Incidents)?.selected
    }
        .stateIn(
            scope = coroutineScope,
            initialValue = EmptyIncident,
            started = subscribedReplay(),
        )

    override val incidentId = incident.map { it.id }
        .stateIn(
            scope = coroutineScope,
            initialValue = EmptyIncident.id,
            started = subscribedReplay(),
        )

    override fun selectIncident(incident: Incident) {
        coroutineScope.launch {
            submitIncidentChange(incident)
        }
    }

    override suspend fun submitIncidentChange(incident: Incident): Boolean {
        val incidentId = incident.id
        val incidents = incidentsSource.first()
        incidents
            .find { it.id == incidentId }
            ?.let {
                appPreferencesRepository.setSelectedIncident(incidentId)
                return true
            }

        return false
    }
}

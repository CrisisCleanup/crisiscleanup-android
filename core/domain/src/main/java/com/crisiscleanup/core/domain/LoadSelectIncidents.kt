package com.crisiscleanup.core.domain

import com.crisiscleanup.core.data.IncidentSelector
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LoadSelectIncidents(
    incidentsRepository: IncidentsRepository,
    accountDataRepository: AccountDataRepository,
    private val incidentSelector: IncidentSelector,
    private val appPreferencesRepository: LocalAppPreferencesRepository,
    private val coroutineScope: CoroutineScope,
) {
    val data = combine(
        incidentsRepository.incidents,
        accountDataRepository.accountData,
        ::Pair,
    )
        .mapLatest { (incidents, accountData) ->
            incidents.filter { accountData.approvedIncidents.contains(it.id) }
        }
        .map { incidents ->
            var selectedId = incidentSelector.incidentId.first()
            if (selectedId == EmptyIncident.id) {
                selectedId = appPreferencesRepository.userPreferences.first().selectedIncidentId
            }

            // Update incident data or select first if current incident (ID) not found
            var incident = incidents.find { it.id == selectedId } ?: EmptyIncident
            if (incident == EmptyIncident && incidents.isNotEmpty()) {
                incident = incidents[0]
                appPreferencesRepository.setSelectedIncident(incident.id)
            }

            incidentSelector.setIncident(incident)

            if (incident.id == EmptyIncident.id) {
                IncidentsData.Empty
            } else {
                IncidentsData.Incidents(incidents)
            }
        }.stateIn(
            scope = coroutineScope,
            initialValue = IncidentsData.Loading,
            started = SharingStarted.WhileSubscribed(3_000),
        )

    fun selectIncident(incident: Incident) {
        coroutineScope.launch {
            (data.first() as? IncidentsData.Incidents)?.let { incidentsData ->
                incidentsData.incidents.find { it.id == incident.id }?.let { matchingIncident ->
                    appPreferencesRepository.setSelectedIncident(matchingIncident.id)

                    incidentSelector.setIncident(matchingIncident)
                }
            }
        }
    }
}

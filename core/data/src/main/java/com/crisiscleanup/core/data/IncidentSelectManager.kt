package com.crisiscleanup.core.data

import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentSelector {
    val incidentId: StateFlow<Long>

    val incident: StateFlow<Incident>

    fun setIncident(incident: Incident)
}

@Singleton
class IncidentSelectManager @Inject constructor() : IncidentSelector {
    override var incident = MutableStateFlow(EmptyIncident)
        private set

    override var incidentId = MutableStateFlow(EmptyIncident.id)
        private set

    override fun setIncident(incident: Incident) {
        // TODO Atomic set
        this.incident.value = incident
        incidentId.value = incident.id
    }
}
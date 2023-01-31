package com.crisiscleanup.core.data

import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentSelector {
    val incidentId: StateFlow<Long>

    val incident: StateFlow<Incident>

    fun setIncident(incident: Incident)
}

@Singleton
class IncidentSelectManager @Inject constructor() : IncidentSelector {
    private var _incident = MutableStateFlow(EmptyIncident)
    override val incident = _incident.asStateFlow()

    private var _incidentId = MutableStateFlow(_incident.value.id)
    override val incidentId: StateFlow<Long> = _incidentId

    override fun setIncident(incident: Incident) {
        _incident.value = incident
        _incidentId.value = incident.id
    }
}
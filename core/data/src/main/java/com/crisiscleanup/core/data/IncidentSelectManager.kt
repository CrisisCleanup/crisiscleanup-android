package com.crisiscleanup.core.data

import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.Incident
import javax.inject.Inject
import javax.inject.Singleton

interface IncidentSelector {
    val incidentId: Long

    var incident: Incident
}

@Singleton
class IncidentSelectManager @Inject constructor() : IncidentSelector {
    override var incidentId: Long = EmptyIncident.id
        get() = incident.id
        private set

    override var incident: Incident = EmptyIncident
}
package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.network.model.NetworkIncident

fun NetworkIncident.asEntity() = IncidentEntity(
    id = id,
    startAt = startAt,
    name = name,
    shortName = shortName,
    activePhoneNumber = activePhoneNumber,
    isArchived = isArchived ?: false,
)

fun NetworkIncident.locationsAsEntity(): List<IncidentLocationEntity> =
    locations.map { location -> IncidentLocationEntity(location.id, location.location) }

fun NetworkIncident.incidentLocationCrossReferences():
        List<IncidentIncidentLocationCrossRef> = locations.map { location ->
    IncidentIncidentLocationCrossRef(id, location.id)
}
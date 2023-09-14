package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.EmptyWorksite

data class ExistingWorksiteIdentifier(
    val incidentId: Long,
    // This is the local (database) ID not network ID
    val worksiteId: Long,
) {
    val isDefined = incidentId != EmptyIncident.id &&
        worksiteId != EmptyWorksite.id
}

val ExistingWorksiteIdentifierNone = ExistingWorksiteIdentifier(
    EmptyIncident.id,
    EmptyWorksite.id,
)

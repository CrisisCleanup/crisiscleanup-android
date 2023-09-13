package com.crisiscleanup.core.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface WorksiteInteractor {
    fun onSelectCase(
        incidentId: Long,
        worksiteId: Long,
    )

    fun wasCaseSelected(
        incidentId: Long,
        worksiteId: Long,
        reference: Instant = Clock.System.now(),
    ): Boolean
}

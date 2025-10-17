package com.crisiscleanup.core.data

import kotlin.time.Clock
import kotlin.time.Instant

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

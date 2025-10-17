package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.model.data.EmptyIncident
import kotlin.time.Clock
import kotlin.time.Instant

enum class IncidentPullDataType {
    WorksitesCore,
    WorksitesAdditional,
    Organizations,
}

private val worksiteDataPullTypes = setOf(
    IncidentPullDataType.WorksitesCore,
    IncidentPullDataType.WorksitesAdditional,
)

data class IncidentDataPullStats(
    val incidentId: Long = EmptyIncident.id,
    val incidentName: String = EmptyIncident.shortName,
    val pullType: IncidentPullDataType = IncidentPullDataType.WorksitesCore,
    val isIndeterminate: Boolean = false,
    val stepTotal: Int = 0,
    val currentStep: Int = 0,
    val notificationMessage: String = "",

    val isStarted: Boolean = false,
    val startTime: Instant = Clock.System.now(),
    val isEnded: Boolean = false,

    val dataCount: Int = 0,
    val queryCount: Int = 0,
    val savedCount: Int = 0,

    private val startProgressAmount: Float = 0.001f,
) {
    val isOngoing: Boolean
        get() = isStarted && !isEnded

    val isPullingWorksites: Boolean
        get() = worksiteDataPullTypes.contains(pullType)

    val progress: Float
        get() {
            if (!isStarted) {
                return 0f
            }

            if (isEnded) {
                return 1f
            }

            if (isIndeterminate) {
                return 0.5f
            }

            return if (dataCount > 0) {
                ((queryCount + savedCount) / (2f * dataCount)).coerceIn(0f, 0.999f)
            } else {
                startProgressAmount
            }
        }
}

package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class NetworkWorksiteChangesResult(
    val errors: List<NetworkCrisisCleanupApiError>? = null,
    val error: String? = null,
    val changes: List<NetworkWorksiteChange>? = null,
)

@Serializable
data class NetworkWorksiteChange(
    @SerialName("incident_id")
    val incidentId: Long,
    @SerialName("worksite_id")
    val worksiteId: Long,
    @SerialName("invalidated_at")
    val invalidatedAt: Instant?,
)

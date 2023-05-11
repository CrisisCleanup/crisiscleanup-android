package com.crisiscleanup.core.network.worksitechange

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 01 Initial model
 */
const val WorksiteChangeModelVersion = 2

@Serializable
data class WorkTypeTransfer(
    val reason: String,
    val workTypes: List<String>,
) {
    val hasValue = reason.isNotBlank() && workTypes.isNotEmpty()
}

@Serializable
data class WorksiteChange(
    val start: WorksiteSnapshot?,
    val change: WorksiteSnapshot,
    val requestWorkTypes: WorkTypeTransfer? = null,
    val releaseWorkTypes: WorkTypeTransfer? = null,
) {
    val isWorkTypeTransfer =
        requestWorkTypes?.hasValue == true || releaseWorkTypes?.hasValue == true
}

data class SyncWorksiteChange(
    val id: Long,
    val createdAt: Instant,
    val syncUuid: String,
    val isPartiallySynced: Boolean,
    val worksiteChange: WorksiteChange,
)

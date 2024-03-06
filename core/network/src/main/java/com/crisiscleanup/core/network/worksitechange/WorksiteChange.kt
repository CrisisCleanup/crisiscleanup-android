package com.crisiscleanup.core.network.worksitechange

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 01 Initial model
 * 02 Work type requests & releases
 * 03 Photo change
 * 04 Explicit change flags
 */
const val WORKSITE_CHANGE_MODEL_VERSION = 4

@Serializable
data class WorkTypeTransfer(
    val reason: String,
    val workTypes: List<String>,
) {
    val hasValue = reason.isNotBlank() && workTypes.isNotEmpty()
}

@Serializable
data class WorksiteChange(
    // v4
    val isWorksiteDataChange: Boolean? = false,
    // v1
    val start: WorksiteSnapshot?,
    val change: WorksiteSnapshot,
    // v2
    val requestWorkTypes: WorkTypeTransfer? = null,
    val releaseWorkTypes: WorkTypeTransfer? = null,
    // v3
    @Deprecated("Photo changes are processed separately from data changes")
    val isPhotoChange: Boolean? = false,
) {
    // v4
    val isWorkTypeTransferChange =
        requestWorkTypes?.hasValue == true || releaseWorkTypes?.hasValue == true

    // v2
    @Deprecated("Use isWorkTypeTransferChange")
    val isWorkTypeTransfer = isWorkTypeTransferChange
}

data class SyncWorksiteChange(
    val id: Long,
    val createdAt: Instant,
    val syncUuid: String,
    val isPartiallySynced: Boolean,
    val worksiteChange: WorksiteChange,
)

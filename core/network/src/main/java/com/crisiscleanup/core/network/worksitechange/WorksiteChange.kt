package com.crisiscleanup.core.network.worksitechange

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * 01 Initial model
 */
const val WorksiteChangeModelVersion = 1

@Serializable
data class WorksiteChange(
    val start: WorksiteSnapshot?,
    val change: WorksiteSnapshot,
)

data class SyncWorksiteChange(
    val id: Long,
    val createdAt: Instant,
    val syncUuid: String,
    val isPartiallySynced: Boolean,
    val worksiteChange: WorksiteChange,
)

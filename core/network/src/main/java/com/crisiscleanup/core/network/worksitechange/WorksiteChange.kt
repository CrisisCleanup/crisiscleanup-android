package com.crisiscleanup.core.network.worksitechange

import kotlinx.serialization.Serializable

/**
 * 01 Initial model
 */
private const val ChangeModelVersion = 1

@Serializable
data class WorksiteChange(
    val start: WorksiteSnapshot?,
    val change: WorksiteSnapshot,
    val modelVersion: Int = ChangeModelVersion,
)

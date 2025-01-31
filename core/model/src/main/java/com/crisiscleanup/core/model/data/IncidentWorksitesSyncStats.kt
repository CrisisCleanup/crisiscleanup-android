package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

data class IncidentWorksitesSyncStats(
    val incidentId: Long,
    val syncTimestamps: SyncTimestamps,
    val fullSyncTimestamps: SyncTimestamps,
    val boundedParameters: SyncBoundedParameters?,
    val boundedSyncTimestamps: SyncTimestamps,
    val appBuildVersionCode: Long,
) {
    data class SyncTimestamps(
        val before: Instant,
        val after: Instant,
    ) {
        companion object {
            fun relative(reference: Instant = Clock.System.now()) = SyncTimestamps(
                reference,
                after = reference - 1.seconds,
            )
        }

        val isDeltaSync: Boolean
            get() = before - Instant.fromEpochSeconds(0) < 1.days
    }

    data class SyncBoundedParameters(
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
        val timestamp: Instant,
    )
}

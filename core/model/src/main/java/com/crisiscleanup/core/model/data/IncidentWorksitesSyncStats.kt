package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

private val epochZero = Instant.fromEpochSeconds(0)

data class IncidentWorksitesSyncStats(
    val incidentId: Long,
    val syncTimestamps: SyncTimestamps,
    val fullSyncTimestamps: SyncTimestamps,
    val boundedParameters: SyncBoundedParameters?,
    val boundedSyncTimestamps: SyncTimestamps,
    val appBuildVersionCode: Long,
) {
    val lastUpdated by lazy {
        var latest = epochZero
        listOf(
            syncTimestamps,
            fullSyncTimestamps,
            boundedSyncTimestamps,
        ).forEach {
            with(it) {
                if (isDeltaSync) {
                    latest = after.coerceAtLeast(latest)
                }
            }
        }

        if (latest - epochZero < 1.days) {
            null
        } else {
            latest
        }
    }

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
            get() = before - epochZero < 1.days
    }

    data class SyncBoundedParameters(
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
        val timestamp: Instant,
    )
}

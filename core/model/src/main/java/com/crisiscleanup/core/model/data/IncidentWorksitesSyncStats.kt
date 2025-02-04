package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

private val epochZero = Instant.fromEpochSeconds(0)

data class IncidentWorksitesSyncStats(
    val incidentId: Long,
    val syncSteps: SyncStepTimestamps,
    val boundedRegion: BoundedRegion?,
    val boundedSyncedAt: Instant,
    val appBuildVersionCode: Long,
) {
    val lastUpdated by lazy {
        var latest = epochZero
        listOf(
            syncSteps.short,
            syncSteps.full,
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

    data class SyncStepTimestamps(
        val short: SyncTimestamps,
        val full: SyncTimestamps,
    ) {
        companion object {
            fun relative(reference: Instant = Clock.System.now()) = SyncStepTimestamps(
                short = SyncTimestamps.relative(reference),
                full = SyncTimestamps.relative(reference),
            )
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

    data class BoundedRegion(
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
    )
}

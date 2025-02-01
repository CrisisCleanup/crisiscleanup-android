package com.crisiscleanup.core.model.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

private val epochZero = Instant.fromEpochSeconds(0)

data class IncidentWorksitesSyncStats(
    val incidentId: Long,
    val syncSteps: SyncStepTimestamps,
    val boundedParameters: SyncBoundedParameters?,
    val boundedSyncSteps: SyncStepTimestamps,
    val appBuildVersionCode: Long,
) {
    companion object {
        fun startingStats(
            incidentId: Long,
            boundedParameters: SyncBoundedParameters?,
            appBuildVersionCode: Long,
            reference: Instant = Clock.System.now(),
        ) = IncidentWorksitesSyncStats(
            incidentId,
            syncSteps = SyncStepTimestamps.relative(reference),
            boundedParameters,
            boundedSyncSteps = SyncStepTimestamps.relative(reference),
            appBuildVersionCode,
        )
    }

    val lastUpdated by lazy {
        var latest = epochZero
        listOf(
            syncSteps.short,
            syncSteps.full,
            boundedSyncSteps.short,
            boundedSyncSteps.full,
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

    data class SyncBoundedParameters(
        val latitude: Double,
        val longitude: Double,
        val radius: Float,
    )
}

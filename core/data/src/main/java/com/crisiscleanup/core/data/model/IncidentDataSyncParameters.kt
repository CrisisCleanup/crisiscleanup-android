package com.crisiscleanup.core.data.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

data class IncidentDataSyncParameters(
    val incidentId: Long,
    val syncDataMeasures: SyncDataMeasure,
    val boundedRegion: BoundedRegion?,
    val boundedSyncedAt: Instant,
) {
    companion object {
        val timeMarkerZero = Instant.fromEpochSeconds(0)
    }

    // TODO Write tests
    val lastUpdated by lazy {
        var latest = boundedSyncedAt
        listOf(
            syncDataMeasures.short,
            syncDataMeasures.full,
        ).forEach {
            with(it) {
                if (isDeltaSync) {
                    latest = after.coerceAtLeast(latest)
                }
            }
        }

        if (latest - timeMarkerZero < 1.days) {
            null
        } else {
            latest
        }
    }

    data class SyncDataMeasure(
        val short: SyncTimeMarker,
        val full: SyncTimeMarker,
    ) {
        companion object {
            fun relative(reference: Instant = Clock.System.now()) = SyncDataMeasure(
                short = SyncTimeMarker.relative(reference),
                full = SyncTimeMarker.relative(reference),
            )
        }
    }

    data class SyncTimeMarker(
        val before: Instant,
        val after: Instant,
    ) {
        companion object {
            fun relative(reference: Instant = Clock.System.now()) = SyncTimeMarker(
                reference,
                after = reference - 1.seconds,
            )
        }

        val isDeltaSync: Boolean
            get() = before - timeMarkerZero < 1.days
    }

    @Serializable
    data class BoundedRegion(
        val latitude: Double,
        val longitude: Double,
        val radius: Double,
    ) {
        val isDefined by lazy {
            radius > 0f &&
                (latitude != 0.0 || longitude != 0.0) &&
                latitude > -90 && latitude < 90 &&
                longitude >= -180 && longitude <= 180
        }

        fun isSignificantChange(
            other: BoundedRegion,
            thresholdMiles: Float = 0.5f,
        ): Boolean {
            // ~69 miles in 1 degree. 1/69 ~= 0.0145 (degrees).
            val thresholdDegrees = 0.0145 * thresholdMiles
            return abs(radius - other.radius) > thresholdMiles ||
                abs(latitude - other.latitude) > thresholdDegrees ||
                abs(longitude.cap360 - other.longitude.cap360) > thresholdDegrees
        }
    }
}

private val Double.cap360: Double
    get() = (this + 360.0) % 360.0

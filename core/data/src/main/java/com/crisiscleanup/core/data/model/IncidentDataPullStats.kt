package com.crisiscleanup.core.data.model

import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

data class IncidentDataPullStats(
    val isStarted: Boolean = false,
    val incidentId: Long = EmptyIncident.id,
    val pullStart: Instant = Clock.System.now(),
    val dataCount: Int = 0,
    val isPagingRequest: Boolean = false,
    val requestedCount: Int = 0,
    val savedCount: Int = 0,
    val isEnded: Boolean = false,
    private val startProgressAmount: Float = 0.01f,
    private val countProgressAmount: Float = 0.05f,
    private val requestStartedAmount: Float = 0.1f,
    val saveStartedAmount: Float = 0.33f,
) {
    val isOngoing: Boolean
        get() = isStarted && !isEnded

    val progress: Float
        get() {
            var progress = 0f
            if (isStarted) {
                // Pull has started
                progress = startProgressAmount
                if (dataCount > 0) {
                    progress = countProgressAmount

                    val remainingProgress = if (isPagingRequest) {
                        (requestedCount + savedCount) * 0.5f / dataCount
                    } else {
                        if (savedCount > 0) {
                            saveStartedAmount + (1 - saveStartedAmount) * savedCount / requestedCount
                        } else if (requestedCount > 0) {
                            saveStartedAmount
                        } else {
                            requestStartedAmount
                        }
                    }

                    progress += (1 - progress) * remainingProgress
                }
            }
            return progress.coerceAtMost(1f)
        }

    val projectedFinish: Instant
        get() {
            val now = Clock.System.now()
            val delta = now - pullStart
            val p = progress
            if (p <= 0 || delta <= 0.seconds) {
                return now.plus(999_999.hours)
            }

            val projectedDeltaSeconds = (delta.inWholeSeconds / p).roundToLong().seconds
            return pullStart.plus(projectedDeltaSeconds)
        }
}

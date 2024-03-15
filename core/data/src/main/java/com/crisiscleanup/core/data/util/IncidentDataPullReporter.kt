package com.crisiscleanup.core.data.util

import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

interface IncidentDataPullReporter {
    val incidentDataPullStats: Flow<IncidentDataPullStats>
    val incidentSecondaryDataPullStats: Flow<IncidentDataPullStats>
    val onIncidentDataPullComplete: Flow<Long>
}

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

internal class IncidentDataPullStatsUpdater(
    private var pullStats: IncidentDataPullStats = IncidentDataPullStats(),
    private val updatePullStats: (IncidentDataPullStats) -> Unit,
) {
    private fun reportChange(pullStats: IncidentDataPullStats) {
        this.pullStats = pullStats
        updatePullStats(pullStats)
    }

    fun beginPull(incidentId: Long) {
        reportChange(
            pullStats.copy(
                isStarted = true,
                incidentId = incidentId,
                pullStart = Clock.System.now(),
            ),
        )
    }

    fun setPagingRequest() {
        reportChange(pullStats.copy(isPagingRequest = true))
    }

    fun updateDataCount(dataCount: Int) {
        reportChange(pullStats.copy(dataCount = dataCount))
    }

    fun updateRequestedCount(requestedCount: Int) {
        reportChange(pullStats.copy(requestedCount = requestedCount))
    }

    fun addSavedCount(savedCount: Int) {
        reportChange(pullStats.copy(savedCount = pullStats.savedCount + savedCount))
    }

    fun endPull() {
        reportChange(pullStats.copy(isEnded = true))
    }
}

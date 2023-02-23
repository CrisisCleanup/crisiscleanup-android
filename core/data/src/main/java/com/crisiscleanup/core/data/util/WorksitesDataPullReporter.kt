package com.crisiscleanup.core.data.util

import com.crisiscleanup.core.model.data.EmptyIncident
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

interface WorksitesDataPullReporter {
    val worksitesDataPullStats: Flow<IncidentWorksitesDataPullStats>
}

data class IncidentWorksitesDataPullStats(
    val isPulling: Boolean = false,
    val incidentId: Long = EmptyIncident.id,
    val pullStart: Instant = Clock.System.now(),
    val worksitesCount: Int = 0,
    val isRequestingData: Boolean = false,
    val isPagingRequest: Boolean = false,
    val requestedCount: Int = 0,
    val savedCount: Int = 0,
    val isEnded: Boolean = false,
    private val startProgressAmount: Float = 0.01f,
    private val countProgressAmount: Float = 0.05f,
) {
    val progress: Float
        get() {
            var progress = 0f
            if (isPulling) {
                // Pull has started
                progress = startProgressAmount
                if (isRequestingData || worksitesCount > 0) {
                    progress = countProgressAmount

                    val remainingProgress = if (isPagingRequest) {
                        (requestedCount + savedCount) * 0.5f / worksitesCount
                    } else {
                        if (savedCount > 0) {
                            0.5f + (savedCount.toFloat() / worksitesCount) * 0.5f
                        } else if (requestedCount > 0) {
                            0.5f
                        } else {
                            0.1f
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

internal class WorksitesDataPullStatsUpdater(
    private var pullStats: IncidentWorksitesDataPullStats = IncidentWorksitesDataPullStats(),
    private val updatePullStats: (IncidentWorksitesDataPullStats) -> Unit,
) {
    private fun reportChange(pullStats: IncidentWorksitesDataPullStats) {
        this.pullStats = pullStats
        updatePullStats(pullStats)
    }

    fun beginPull(incidentId: Long) {
        reportChange(
            pullStats.copy(
                isPulling = true,
                incidentId = incidentId,
                pullStart = Clock.System.now(),
            )
        )
    }

    fun beginRequest() {
        reportChange(pullStats.copy(isRequestingData = true))
    }

    fun setPagingRequest() {
        reportChange(pullStats.copy(isPagingRequest = true))
    }

    fun endRequest() {
        reportChange(pullStats.copy(isRequestingData = false))
    }

    fun updateWorksitesCount(worksitesCount: Int) {
        reportChange(pullStats.copy(worksitesCount = worksitesCount))
    }

    fun updateRequestedCount(requestedCount: Int) {
        reportChange(pullStats.copy(requestedCount = requestedCount))
    }

    fun addSavedCount(savedCount: Int) {
        reportChange(pullStats.copy(savedCount = pullStats.savedCount + savedCount))
    }

    fun endPull() {
        reportChange(
            pullStats.copy(
                isPulling = false,
                isEnded = true,
            )
        )
    }
}
package com.crisiscleanup.core.data.util

import com.crisiscleanup.core.data.model.IncidentDataPullStats
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

interface IncidentDataPullReporter {
    val incidentDataPullStats: Flow<IncidentDataPullStats>
    val incidentSecondaryDataPullStats: Flow<IncidentDataPullStats>
    val onIncidentDataPullComplete: Flow<Long>
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

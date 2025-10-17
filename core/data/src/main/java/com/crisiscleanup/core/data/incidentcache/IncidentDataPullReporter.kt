package com.crisiscleanup.core.data.incidentcache

import com.crisiscleanup.core.data.model.IncidentDataPullStats
import com.crisiscleanup.core.data.model.IncidentPullDataType
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.Instant

interface IncidentDataPullReporter {
    val incidentDataPullStats: Flow<IncidentDataPullStats>
    val onIncidentDataPullComplete: Flow<Long>
}

internal class IncidentDataPullStatsUpdater(
    private var pullStats: IncidentDataPullStats = IncidentDataPullStats(),
    private val updatePullStats: (IncidentDataPullStats) -> Unit,
) {
    val startedAt: Instant
        get() = pullStats.startTime

    private fun reportChange(pullStats: IncidentDataPullStats) {
        this.pullStats = pullStats
        updatePullStats(pullStats)
    }

    fun beginPull(
        incidentId: Long,
        incidentName: String,
        pullType: IncidentPullDataType,
        startTime: Instant = Clock.System.now(),
    ) {
        reportChange(
            pullStats.copy(
                incidentId,
                incidentName,
                pullType,
                isStarted = true,
                startTime = startTime,
            ),
        )
    }

    fun setIndeterminate() {
        reportChange(pullStats.copy(isIndeterminate = true))
    }

    fun setDeterminate() {
        reportChange(pullStats.copy(isIndeterminate = false))
    }

    fun setDataCount(count: Int) {
        reportChange(pullStats.copy(dataCount = count))
    }

    fun addDataCount(count: Int) {
        reportChange(pullStats.copy(dataCount = pullStats.dataCount + count))
    }

    fun addQueryCount(count: Int) {
        reportChange(pullStats.copy(queryCount = pullStats.queryCount + count))
    }

    fun addSavedCount(count: Int) {
        reportChange(pullStats.copy(savedCount = pullStats.savedCount + count))
    }

    fun setStep(current: Int, total: Int) {
        reportChange(
            pullStats.copy(
                currentStep = current,
                stepTotal = total,
            ),
        )
    }

    fun clearStep() {
        setStep(0, 0)
    }

    fun setNotificationMessage(message: String = "") {
        reportChange(pullStats.copy(notificationMessage = message))
    }

    fun clearNotificationMessage() {
        setNotificationMessage()
    }
}

package com.crisiscleanup.core.commoncase.map

import com.crisiscleanup.core.common.epochZero
import com.crisiscleanup.core.data.util.IncidentDataPullStats
import com.crisiscleanup.core.model.data.IncidentIdWorksiteCount
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class MapTileRefresher(
    private val mapTileRenderer: CasesOverviewMapTileRenderer,
    private val casesMapTileManager: CasesMapTileLayerManager,
    private val tileClearRefreshInterval: Duration = 5.seconds,
) {
    private var tileRefreshedInstant: Instant = Instant.epochZero
    private var tileClearWorksitesCount = 0

    // Attempts to clear/refresh map tiles minimally during data loads and incident changes
    // For progressively generating map tiles of larger incidents
    suspend fun refreshTiles(
        idCount: IncidentIdWorksiteCount,
        pullStats: IncidentDataPullStats,
    ) = coroutineScope {
        var refreshTiles = true
        var clearCache = false

        pullStats.run {
            val isIncidentChange = idCount.id != incidentId

            // TODO Stale tiles will flash in certain cases.
            //      Why does the first clear call not take?
            //      Toggle multiple times between (one large) incidents in the same area.
            if (isIncidentChange || idCount.totalCount == 0) {
                tileClearWorksitesCount = 0
                mapTileRenderer.setIncident(incidentId, 0)
                casesMapTileManager.clearTiles()
            }

            if (!isStarted || isIncidentChange) {
                return@run
            }

            refreshTiles = isEnded
            clearCache = isEnded

            if (this.dataCount < 3000) {
                return@run
            }

            val now = Clock.System.now()
            if (!refreshTiles && progress > saveStartedAmount) {
                val sinceLastRefresh = now - tileRefreshedInstant
                val projectedDelta = projectedFinish - now
                refreshTiles = now - pullStart > tileClearRefreshInterval &&
                    sinceLastRefresh > tileClearRefreshInterval &&
                    projectedDelta > tileClearRefreshInterval
                if (idCount.totalCount - tileClearWorksitesCount >= 6000 &&
                    dataCount - tileClearWorksitesCount > 3000
                ) {
                    clearCache = true
                    refreshTiles = true
                }
            }
            if (refreshTiles) {
                tileRefreshedInstant = now
            }
        }

        if (refreshTiles) {
            if (mapTileRenderer.setIncident(idCount.id, idCount.totalCount, clearCache)) {
                clearCache = true
            }
        }

        if (clearCache) {
            tileClearWorksitesCount = idCount.totalCount
            casesMapTileManager.clearTiles()
        } else if (refreshTiles) {
            casesMapTileManager.onTileChange()
        }
    }
}

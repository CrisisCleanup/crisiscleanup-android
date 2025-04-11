package com.crisiscleanup.core.commoncase.map

import com.crisiscleanup.core.common.epochZero
import com.crisiscleanup.core.data.model.IncidentDataPullStats
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
    private var tileRefreshedInstant = Instant.epochZero

    fun resetTiles(incidentId: Long) {
        tileRefreshedInstant = Instant.epochZero
        mapTileRenderer.setIncident(incidentId, 0, true)
        casesMapTileManager.clearTiles()
    }

    // Attempts to clear/refresh map tiles minimally during data loads and incident changes
    // For progressively generating map tiles of larger incidents
    suspend fun refreshTiles(
        idCount: IncidentIdWorksiteCount,
        pullStats: IncidentDataPullStats,
    ) = coroutineScope {
        if (mapTileRenderer.tilesIncident != idCount.id ||
            idCount.id != pullStats.incidentId
        ) {
            return@coroutineScope
        }

        val now = Clock.System.now()

        if (pullStats.isEnded) {
            tileRefreshedInstant = now
            mapTileRenderer.setIncident(idCount.id, idCount.totalCount, true)
            casesMapTileManager.clearTiles()
            return@coroutineScope
        }

        pullStats.apply {
            if (!isStarted || idCount.totalCount == 0) {
                return@coroutineScope
            }
        }

        val sinceLastRefresh = now - tileRefreshedInstant
        val refreshTiles = tileRefreshedInstant == Instant.epochZero ||
            now - pullStats.startTime > tileClearRefreshInterval &&
            sinceLastRefresh > tileClearRefreshInterval
        if (refreshTiles) {
            tileRefreshedInstant = now
            mapTileRenderer.setIncident(idCount.id, idCount.totalCount, true)
            casesMapTileManager.clearTiles()
        }
    }
}

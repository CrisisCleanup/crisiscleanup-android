package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.IncidentDataSyncStats
import com.crisiscleanup.core.model.data.LocalWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.WorksiteSummary
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface WorksitesRepository {
    /**
     * Is loading incidents data
     */
    val isLoading: Flow<Boolean>

    val syncWorksitesFullIncidentId: Flow<Long>

    /**
     * Stream of an incident's [Worksite]s
     */
    fun streamWorksites(incidentId: Long, limit: Int, offset: Int): Flow<List<Worksite>>

    fun streamIncidentWorksitesCount(id: Long): Flow<Int>

    fun streamLocalWorksite(worksiteId: Long): Flow<LocalWorksite?>

    fun streamRecentWorksites(incidentId: Long): Flow<List<WorksiteSummary>>

    fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int
    ): List<WorksiteMapMark>

    fun getWorksitesCount(incidentId: Long): Int

    fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
    ): Int

    suspend fun refreshWorksites(
        incidentId: Long,
        forceQueryDeltas: Boolean = false,
        forceRefreshAll: Boolean = false,
    )

    suspend fun syncWorksitesFull(incidentId: Long): Boolean

    fun getWorksiteSyncStats(incidentId: Long): IncidentDataSyncStats?

    fun getLocalId(networkWorksiteId: Long): Long

    suspend fun syncNetworkWorksite(
        worksite: NetworkWorksiteFull,
        syncedAt: Instant = Clock.System.now(),
    ): Boolean

    suspend fun pullWorkTypeRequests(networkWorksiteId: Long)

    suspend fun setRecentWorksite(
        incidentId: Long,
        worksiteId: Long,
        viewStart: Instant,
    )

    fun getUnsyncedCounts(worksiteId: Long): List<Int>

    suspend fun shareWorksite(
        worksiteId: Long,
        emails: List<String>,
        phoneNumbers: List<String>,
        shareMessage: String,
        noClaimReason: String?,
    ): Boolean
}
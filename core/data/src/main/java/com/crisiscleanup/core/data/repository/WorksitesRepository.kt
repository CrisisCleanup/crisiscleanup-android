package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.IncidentIdWorksiteCount
import com.crisiscleanup.core.model.data.LocalWorksite
import com.crisiscleanup.core.model.data.TableDataWorksite
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.model.data.WorksiteMapMark
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.core.model.data.WorksiteSummary
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

interface WorksitesRepository {
    // Filtering sometimes requires scanning
    val isDeterminingWorksitesCount: Flow<Boolean>
    fun streamIncidentWorksitesCount(
        incidentIdStream: Flow<Long>,
        useTeamFilters: Boolean,
    ): Flow<IncidentIdWorksiteCount>

    fun streamLocalWorksite(worksiteId: Long): Flow<LocalWorksite?>

    suspend fun getWorksite(worksiteId: Long): Worksite

    fun streamRecentWorksites(incidentId: Long): Flow<List<WorksiteSummary>>

    fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int,
        coordinates: Pair<Double, Double>?,
        useTeamFilters: Boolean,
    ): List<WorksiteMapMark>

    fun getWorksitesCount(incidentId: Long): Int

    fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
    ): Int

    fun getLocalId(networkWorksiteId: Long): Long

    suspend fun syncNetworkWorksite(
        worksite: NetworkWorksiteFull,
        syncedAt: Instant = Clock.System.now(),
    ): Boolean

    suspend fun syncNetworkWorksite(networkWorksiteId: Long)

    suspend fun syncNetworkWorksites(networkIds: Collection<Long>)

    suspend fun pullWorkTypeRequests(networkWorksiteId: Long)

    suspend fun setRecentWorksite(
        incidentId: Long,
        worksiteId: Long,
        viewStart: Instant,
    )

    suspend fun getRecentWorksitesCenterLocation(
        incidentId: Long,
        limit: Int = 3,
    ): Pair<Double, Double>?

    fun getUnsyncedCounts(worksiteId: Long): List<Int>

    suspend fun shareWorksite(
        worksiteId: Long,
        emails: List<String>,
        phoneNumbers: List<String>,
        shareMessage: String,
        noClaimReason: String?,
    ): Boolean

    suspend fun getTableData(
        incidentId: Long,
        filters: CasesFilter,
        sortBy: WorksiteSortBy,
        coordinates: Pair<Double, Double>?,
        searchRadius: Float = 100f,
        count: Int = 360,
    ): List<TableDataWorksite>
}

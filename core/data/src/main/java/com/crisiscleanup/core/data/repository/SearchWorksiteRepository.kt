package com.crisiscleanup.core.data.repository

import android.util.LruCache
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.common.kmToMiles
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.fts.asSummary
import com.crisiscleanup.core.database.dao.fts.getMatchingWorksites
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorksiteSummary
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkWorksiteShort
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

interface SearchWorksitesRepository {
    suspend fun searchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<WorksiteSummary>

    suspend fun locationSearchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<WorksiteSummary>

    suspend fun getMatchingLocalWorksites(incidentId: Long, q: String): Collection<WorksiteSummary>

    suspend fun getWorksiteByCaseNumber(
        incidentId: Long,
        caseNumber: String,
    ): WorksiteSummary?
}

class MemoryCacheSearchWorksitesRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val locationProvider: LocationProvider,
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : SearchWorksitesRepository {
    // TODO Make size configurable and consider different size determination
    private val searchCache =
        LruCache<IncidentQuery, Pair<Instant, Collection<WorksiteSummary>>>(30)

    private val staleResultDuration = 10.minutes

    private fun getCacheResults(
        incidentId: Long,
        q: String,
    ): Triple<IncidentQuery, Instant, Collection<WorksiteSummary>?> {
        val incidentQuery = IncidentQuery(incidentId, q)

        val now = Clock.System.now()

        var cacheResults: Collection<WorksiteSummary>? = null
        searchCache.get(incidentQuery)?.let {
            if (now - it.first < staleResultDuration) {
                cacheResults = it.second
            }
        }

        return Triple(incidentQuery, now, cacheResults)
    }

    override suspend fun searchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<WorksiteSummary> = coroutineScope {
        val (incidentQuery, now, cacheResults) = getCacheResults(incidentId, q)

        cacheResults?.let {
            return@coroutineScope it
        }

        try {
            val results = networkDataSource.getSearchWorksites(incidentId, q)
            if (results.isNotEmpty()) {
                val searchResult = results.map { networkWorksite ->
                    val workType = networkWorksite.newestKeyWorkType?.let { keyWorkType ->
                        WorkType(
                            0,
                            statusLiteral = keyWorkType.status,
                            workTypeLiteral = keyWorkType.workType,
                        )
                    }
                    with(networkWorksite) {
                        WorksiteSummary(
                            id = 0,
                            networkId = id,
                            name,
                            address,
                            city,
                            state,
                            postalCode ?: "",
                            county,
                            caseNumber,
                            workType,
                        )
                    }
                }

                searchCache.put(incidentQuery, Pair(now, searchResult))

                return@coroutineScope searchResult
            }
        } catch (e: Exception) {
            logger.logException(e)
        }

        return@coroutineScope emptyList()
    }

    private suspend fun filterResults(
        results: List<NetworkWorksiteShort>,
        filters: CasesFilter,
    ): List<NetworkWorksiteShort> {
        val uniqueIds = mutableSetOf<Long>()
        val uniqueResults = mutableListOf<NetworkWorksiteShort>()

        val coordinates = locationProvider.getLocation()
        val filterByDistance = coordinates != null && filters.hasDistanceFilter
        val locationLatitudeRad = coordinates?.first?.radians ?: 0.0
        val locationLongitudeRad = coordinates?.second?.radians ?: 0.0

        for (result in results) {
            if (uniqueIds.contains(result.id)) {
                continue
            }

            val distance = if (filterByDistance) {
                val (resultLongitude, resultLatitude) = result.location.coordinates
                haversineDistance(
                    locationLatitudeRad,
                    locationLongitudeRad,
                    resultLatitude.radians,
                    resultLongitude.radians,
                ).kmToMiles
            } else {
                null
            }

            if (!filters.passesFilter(
                    result.svi,
                    result.updatedAt,
                    distance,
                )
            ) {
                continue
            }

            uniqueResults.add(result)
            uniqueIds.add(result.id)
        }

        return uniqueResults
    }

    override suspend fun locationSearchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<WorksiteSummary> {
        val (incidentQuery, now, cacheResults) = getCacheResults(incidentId, q)
        cacheResults?.let {
            return it
        }

        try {
            val results = networkDataSource.getLocationSearchWorksites(incidentId, q)
            if (results.isNotEmpty()) {
                val searchResult = results.map { networkWorksite ->
                    val workType = networkWorksite.keyWorkType.let { keyWorkType ->
                        WorkType(
                            0,
                            statusLiteral = keyWorkType.status,
                            workTypeLiteral = keyWorkType.workType,
                        )
                    }
                    with(networkWorksite) {
                        WorksiteSummary(
                            id = 0,
                            networkId = id,
                            name,
                            address,
                            city,
                            state,
                            postalCode ?: "",
                            county,
                            caseNumber,
                            workType,
                        )
                    }
                }

                searchCache.put(incidentQuery, Pair(now, searchResult))
                return searchResult
            }
        } catch (e: Exception) {
            logger.logException(e)
        }

        return emptyList()
    }

    override suspend fun getMatchingLocalWorksites(incidentId: Long, q: String) =
        worksiteDaoPlus.getMatchingWorksites(incidentId, q)

    override suspend fun getWorksiteByCaseNumber(
        incidentId: Long,
        caseNumber: String,
    ): WorksiteSummary? {
        if (caseNumber.isBlank()) {
            return null
        }

        return worksiteDao.getWorksiteByCaseNumber(incidentId, caseNumber)?.asSummary()
    }
}

private data class IncidentQuery(
    val incidentId: Long,
    val q: String,
)

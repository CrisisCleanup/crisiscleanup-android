package com.crisiscleanup.core.data.repository

import android.util.LruCache
import com.crisiscleanup.core.common.LocationProvider
import com.crisiscleanup.core.common.haversineDistance
import com.crisiscleanup.core.common.kmToMiles
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.radians
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.fts.getMatchingWorksites
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorksiteSummary
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkWorksiteShort
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

interface SearchWorksitesRepository {
    suspend fun searchWorksites(
        incidentId: Long,
        q: String,
        applyFilters: Boolean = false,
    ): Collection<WorksiteSummary>

    suspend fun locationSearchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<WorksiteSummary>

    suspend fun getMatchingLocalWorksites(incidentId: Long, q: String): Collection<WorksiteSummary>
}

class MemoryCacheSearchWorksitesRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val filterRepository: CasesFilterRepository,
    private val locationProvider: LocationProvider,
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
        applyFilters: Boolean,
    ): Collection<WorksiteSummary> = coroutineScope {
        val (filters, filterQuery) = filterRepository.filterQuery.first()
        val hasFilters = applyFilters && filters.changeCount > 0

        val (incidentQuery, now, cacheResults) = getCacheResults(incidentId, q)
        val useCache = !hasFilters

        if (useCache) {
            cacheResults?.let {
                return@coroutineScope it
            }
        }

        try {
            val searchFilters = if (hasFilters) filterQuery else emptyMap()
            var results = networkDataSource.getSearchWorksites(incidentId, q, searchFilters)

            if (hasFilters) {
                ensureActive()

                results = filterResults(results, filters)

                ensureActive()
            }

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

                if (useCache) {
                    searchCache.put(incidentQuery, Pair(now, searchResult))
                }

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

        var hasLocation = false
        var locationLatitude = 0.0
        var locationLongitude = 0.0
        locationProvider.getLocation()?.let { currentLocation ->
            locationLatitude = currentLocation.first.radians
            locationLongitude = currentLocation.second.radians
            hasLocation = true
        }

        for (result in results) {
            if (uniqueIds.contains(result.id)) {
                continue
            }

            val distance = if (hasLocation && filters.hasDistanceFilter) {
                val resultCoordinates = result.location.coordinates
                val (resultLongitude, resultLatitude) = resultCoordinates
                val kmDistance = haversineDistance(
                    locationLatitude, locationLongitude,
                    resultLatitude.radians, resultLongitude.radians,
                )
                kmDistance.kmToMiles
            } else {
                null
            }

            if (!filters.passesFilter(
                    result.svi ?: 0f,
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
}

private data class IncidentQuery(
    val incidentId: Long,
    val q: String,
)

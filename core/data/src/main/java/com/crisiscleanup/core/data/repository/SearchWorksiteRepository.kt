package com.crisiscleanup.core.data.repository

import android.util.LruCache
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.WorksiteSummary
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
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
}

class MemoryCacheSearchWorksitesRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : SearchWorksitesRepository {
    // TODO Make size configurable and consider different size determination
    private val searchCache =
        LruCache<IncidentQuery, Pair<Instant, Collection<WorksiteSummary>>>(30)

    private val staleResultDuration = 30.minutes

    private fun getCacheResults(
        incidentId: Long,
        q: String,
    ): Triple<IncidentQuery, Instant, Collection<WorksiteSummary>?> {
        val incidentQuery = IncidentQuery(incidentId, q)

        val now = Clock.System.now()

        // TODO Search local on device data. Will need to change the method of data delivery.

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
    ): Collection<WorksiteSummary> {
        val (incidentQuery, now, cacheResults) = getCacheResults(incidentId, q)
        cacheResults?.let {
            return it
        }

        // TODO Search local as well

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
                return searchResult
            }
        } catch (e: Exception) {
            logger.logException(e)
        }

        return emptyList()
    }

    override suspend fun locationSearchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<WorksiteSummary> {
        val (incidentQuery, now, cacheResults) = getCacheResults(incidentId, q)
        cacheResults?.let {
            return it
        }

        // TODO Search local as well

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
}

private data class IncidentQuery(
    val incidentId: Long,
    val q: String,
)

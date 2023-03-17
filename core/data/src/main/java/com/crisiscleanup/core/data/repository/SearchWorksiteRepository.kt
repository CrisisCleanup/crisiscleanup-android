package com.crisiscleanup.core.data.repository

import android.util.LruCache
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryThrowException
import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

interface SearchWorksitesRepository {
    suspend fun locationSearchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<NetworkWorksiteLocationSearch>
}

class MemoryCacheSearchWorksitesRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val authEventManager: AuthEventManager,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : SearchWorksitesRepository {
    // TODO Make size configurable and consider different size determination
    private val locationSearchCache =
        LruCache<IncidentQuery, Pair<Instant, Collection<NetworkWorksiteLocationSearch>>>(30)

    private val staleResultDuration = 30.minutes

    override suspend fun locationSearchWorksites(
        incidentId: Long,
        q: String,
    ): Collection<NetworkWorksiteLocationSearch> {
        val incidentQuery = IncidentQuery(incidentId, q)

        val now = Clock.System.now()

        // TODO Search local on device data. Will need to change the method of data delivery.

        val cacheResults = locationSearchCache.get(incidentQuery)
        cacheResults?.let {
            if (now - it.first < staleResultDuration) {
                return it.second
            }
        }

        try {
            val result = networkDataSource.getLocationSearchWorksites(incidentId, q)
            tryThrowException(authEventManager, result.errors)

            result.results?.let {
                locationSearchCache.put(incidentQuery, Pair(now, it))
                return it
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

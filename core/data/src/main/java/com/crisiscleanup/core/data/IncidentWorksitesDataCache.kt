package com.crisiscleanup.core.data

import android.content.Context
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.IncidentWorksitesPageRequest
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.days

interface WorksitesNetworkDataCache {
    fun loadWorksitesShort(
        incidentId: Long,
        pageIndex: Int,
        expectedCount: Int,
    ): IncidentWorksitesPageRequest?

    suspend fun saveWorksitesShort(
        incidentId: Long,
        pageCount: Int,
        pageIndex: Int,
        expectedCount: Int,
        updatedAfter: Instant?,
    )

    suspend fun deleteWorksitesShort(
        incidentId: Long,
        pageIndex: Int,
    )
}

class WorksitesNetworkDataFileCache @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val authEventManager: AuthEventManager,
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : WorksitesNetworkDataCache {
    private fun shortWorksitesFileName(incidentId: Long, page: Int) =
        "incident-$incidentId-worksites-short-$page.json"

    @OptIn(ExperimentalSerializationApi::class)
    override fun loadWorksitesShort(
        incidentId: Long,
        pageIndex: Int,
        expectedCount: Int,
    ): IncidentWorksitesPageRequest? {
        val cacheFileName = shortWorksitesFileName(incidentId, pageIndex)
        val cacheFile = File(context.cacheDir, cacheFileName)
        if (cacheFile.exists()) {
            cacheFile.inputStream().use {
                val cachedData: IncidentWorksitesPageRequest = Json.decodeFromStream(it)
                if (cachedData.incidentId == incidentId &&
                    cachedData.page == pageIndex &&
                    cachedData.totalCount == expectedCount &&
                    // TODO Use configurable duration
                    Clock.System.now() - cachedData.requestTime < 4.days
                ) {
                    return cachedData
                }
            }
        }
        return null
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun saveWorksitesShort(
        incidentId: Long,
        pageCount: Int,
        pageIndex: Int,
        expectedCount: Int,
        updatedAfter: Instant?,
    ) = coroutineScope {
        val cacheFileName = shortWorksitesFileName(incidentId, pageIndex)

        try {
            loadWorksitesShort(incidentId, pageIndex, expectedCount)?.let {
                return@coroutineScope
            }
        } catch (e: Exception) {
            logger.logDebug("Error reading cache file ${e.message}")
        }

        val requestTime = Clock.System.now()
        val worksites = networkDataSource.getWorksitesPage(
            incidentId,
            updatedAfter,
            pageCount,
            pageIndex + 1,
        )

        val dataCache = IncidentWorksitesPageRequest(
            incidentId,
            requestTime,
            pageIndex,
            pageIndex * pageCount,
            expectedCount,
            worksites,
        )

        val cacheFile = File(context.cacheDir, cacheFileName)
        cacheFile.outputStream().use {
            Json.encodeToStream(dataCache, it)
        }
    }

    override suspend fun deleteWorksitesShort(
        incidentId: Long,
        pageIndex: Int,
    ) = coroutineScope {
        val cacheFileName = shortWorksitesFileName(incidentId, pageIndex)
        try {
            val cacheFile = File(context.cacheDir, cacheFileName)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (e: Exception) {
            logger.logDebug("Error deleting cache file $cacheFileName. ${e.message}")
        }
    }
}

package com.crisiscleanup.core.data

import android.content.Context
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.IncidentCacheDataPageRequest
import com.crisiscleanup.core.data.model.IncidentWorksitesPageRequest
import com.crisiscleanup.core.data.model.IncidentWorksitesSecondaryDataPageRequest
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

    fun loadWorksitesSecondaryData(
        incidentId: Long,
        pageIndex: Int,
        expectedCount: Int,
    ): IncidentWorksitesSecondaryDataPageRequest?

    suspend fun saveWorksitesSecondaryData(
        incidentId: Long,
        pageCount: Int,
        pageIndex: Int,
        expectedCount: Int,
        updatedAfter: Instant?,
    )

    suspend fun deleteWorksitesSecondaryData(
        incidentId: Long,
        pageIndex: Int,
    )
}

class WorksitesNetworkDataFileCache @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) : WorksitesNetworkDataCache {
    private fun shortWorksitesFileName(incidentId: Long, page: Int) =
        "incident-$incidentId-worksites-short-$page.json"

    @OptIn(ExperimentalSerializationApi::class)
    private inline fun <reified T : IncidentCacheDataPageRequest> loadCacheData(
        cacheFileName: String,
        incidentId: Long,
        pageIndex: Int,
        expectedCount: Int,
    ): T? {
        val cacheFile = File(context.cacheDir, cacheFileName)
        if (cacheFile.exists()) {
            cacheFile.inputStream().use {
                val cachedData: T = Json.decodeFromStream(it)
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

    override fun loadWorksitesShort(
        incidentId: Long,
        pageIndex: Int,
        expectedCount: Int,
    ) = loadCacheData<IncidentWorksitesPageRequest>(
        shortWorksitesFileName(incidentId, pageIndex),
        incidentId,
        pageIndex,
        expectedCount,
    )

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun saveWorksitesShort(
        incidentId: Long,
        pageCount: Int,
        pageIndex: Int,
        expectedCount: Int,
        updatedAfter: Instant?,
    ) = coroutineScope {
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
            pageCount,
            pageIndex + 1,
            updatedAtAfter = updatedAfter,
        )

        val dataCache = IncidentWorksitesPageRequest(
            incidentId,
            requestTime,
            pageIndex,
            pageIndex * pageCount,
            expectedCount,
            worksites,
        )

        val cacheFileName = shortWorksitesFileName(incidentId, pageIndex)
        val cacheFile = File(context.cacheDir, cacheFileName)
        cacheFile.outputStream().use {
            Json.encodeToStream(dataCache, it)
        }
    }

    override suspend fun deleteWorksitesShort(
        incidentId: Long,
        pageIndex: Int,
    ) {
        val cacheFileName = shortWorksitesFileName(incidentId, pageIndex)
        deleteCacheFile(cacheFileName)
    }

    private suspend fun deleteCacheFile(cacheFileName: String) = coroutineScope {
        try {
            val cacheFile = File(context.cacheDir, cacheFileName)
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
        } catch (e: Exception) {
            logger.logDebug("Error deleting cache file $cacheFileName. ${e.message}")
        }
    }

    private fun secondaryDataFileName(incidentId: Long, page: Int) =
        "incident-$incidentId-worksites-secondary-data-$page.json"

    override fun loadWorksitesSecondaryData(
        incidentId: Long,
        pageIndex: Int,
        expectedCount: Int,
    ) = loadCacheData<IncidentWorksitesSecondaryDataPageRequest>(
        secondaryDataFileName(incidentId, pageIndex),
        incidentId,
        pageIndex,
        expectedCount,
    )

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun saveWorksitesSecondaryData(
        incidentId: Long,
        pageCount: Int,
        pageIndex: Int,
        expectedCount: Int,
        updatedAfter: Instant?,
    ) = coroutineScope {
        try {
            loadWorksitesSecondaryData(incidentId, pageIndex, expectedCount)?.let {
                return@coroutineScope
            }
        } catch (e: Exception) {
            logger.logDebug("Error reading cache file ${e.message}")
        }

        val requestTime = Clock.System.now()
        val worksites = networkDataSource.getWorksitesFlagsFormDataPage(
            incidentId,
            pageCount,
            pageIndex + 1,
            updatedAtAfter = updatedAfter,
        )

        val dataCache = IncidentWorksitesSecondaryDataPageRequest(
            incidentId,
            requestTime,
            pageIndex,
            pageIndex * pageCount,
            expectedCount,
            worksites,
        )

        val cacheFileName = secondaryDataFileName(incidentId, pageIndex)
        val cacheFile = File(context.cacheDir, cacheFileName)
        cacheFile.outputStream().use {
            Json.encodeToStream(dataCache, it)
        }
    }

    override suspend fun deleteWorksitesSecondaryData(
        incidentId: Long,
        pageIndex: Int,
    ) {
        val cacheFileName = secondaryDataFileName(incidentId, pageIndex)
        deleteCacheFile(cacheFileName)
    }
}

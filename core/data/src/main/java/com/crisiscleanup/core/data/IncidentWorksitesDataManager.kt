package com.crisiscleanup.core.data

import android.content.Context
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkCrisisCleanupApiError.Companion.tryThrowException
import com.crisiscleanup.core.network.model.NetworkWorksiteShort
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

@Singleton
class IncidentWorksitesDataManager @Inject constructor(
    private val worksiteNetworkDataSource: CrisisCleanupNetworkDataSource,
    private val authEventManager: AuthEventManager,
    @ApplicationContext private val context: Context,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.Worksites) private val logger: AppLogger,
) {
    private fun shortWorksitesFileName(incidentId: Long) =
        "incident-$incidentId-worksites-short.json"

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun getWorksitesShortData(
        incidentId: Long,
        expectedCount: Int,
    ) = with(ioDispatcher) {
        val requestTime: Instant
        var dataCache: WorksitesRequestDataCache? = null
        val cacheFileName = shortWorksitesFileName(incidentId)

        try {
            val cacheFile = File(context.cacheDir, cacheFileName)
            if (cacheFile.exists()) {
                cacheFile.inputStream().use {
                    val cachedData: WorksitesRequestDataCache = Json.decodeFromStream(it)
                    if (cachedData.worksites.isNotEmpty() &&
                        // TODO Use configurable duration
                        Clock.System.now() - cachedData.requestTime < 4.days
                    ) {
                        dataCache = cachedData
                    }
                }
            }
        } catch (e: Exception) {
            logger.logDebug("Error reading cache file ${e.message}")
        }

        if (dataCache == null) {
            requestTime = Clock.System.now()
            val worksitesRequest = worksiteNetworkDataSource.getWorksitesAll(incidentId, null)
            tryThrowException(authEventManager, worksitesRequest.errors)

            dataCache = WorksitesRequestDataCache(
                incidentId,
                requestTime,
                0,
                expectedCount,
                worksitesRequest.results ?: emptyList(),
            )
            try {
                val cacheFile = File(context.cacheDir, cacheFileName)
                cacheFile.outputStream().use {
                    Json.encodeToStream(dataCache, it)
                }
            } catch (e: Exception) {
                logger.logException(e)
            }
        }

        dataCache ?: throw Exception("Failed to load request data cache")
    }

    fun deleteWorksitesShortDataCache(incidentId: Long) = with(ioDispatcher) {
        val cacheFileName = shortWorksitesFileName(incidentId)
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

@Serializable
data class WorksitesRequestDataCache(
    val incidentId: Long,
    val requestTime: Instant,
    // Indicates the number of records coming before this data
    val startCount: Int,
    val totalCount: Int,
    val worksites: List<NetworkWorksiteShort>,
)
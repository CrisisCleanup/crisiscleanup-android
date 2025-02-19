package com.crisiscleanup.core.data.incidentcache

import android.content.Context
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.IncidentOrganizationsPageRequest
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import javax.inject.Inject

interface IncidentOrganizationsDataCache {
    fun loadOrganizations(
        incidentId: Long,
        dataIndex: Int,
        expectedCount: Int,
    ): IncidentOrganizationsPageRequest?

    suspend fun saveOrganizations(
        incidentId: Long,
        dataIndex: Int,
        expectedCount: Int,
        organizations: List<NetworkIncidentOrganization>,
    )

    suspend fun deleteOrganizations(
        incidentId: Long,
        dataIndex: Int,
    )
}

class IncidentOrganizationsDataFileCache @Inject constructor(
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.Incidents) private val logger: AppLogger,
) : IncidentOrganizationsDataCache {
    private fun cacheFileName(incidentId: Long, offset: Int) =
        "incident-$incidentId-organizations-$offset.json"

    @OptIn(ExperimentalSerializationApi::class)
    override fun loadOrganizations(
        incidentId: Long,
        dataIndex: Int,
        expectedCount: Int,
    ): IncidentOrganizationsPageRequest? {
        val cacheFileName = cacheFileName(incidentId, dataIndex)
        val cacheFile = File(context.cacheDir, cacheFileName)
        if (cacheFile.exists()) {
            cacheFile.inputStream().use {
                val cachedData: IncidentOrganizationsPageRequest = Json.decodeFromStream(it)
                if (cachedData.incidentId == incidentId &&
                    cachedData.offset == dataIndex &&
                    cachedData.totalCount == expectedCount
                ) {
                    return cachedData
                }
            }
        }
        return null
    }

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun saveOrganizations(
        incidentId: Long,
        dataIndex: Int,
        expectedCount: Int,
        organizations: List<NetworkIncidentOrganization>,
    ) = coroutineScope {
        val cacheFileName = cacheFileName(incidentId, dataIndex)

        val dataCache = IncidentOrganizationsPageRequest(
            incidentId,
            dataIndex,
            expectedCount,
            organizations,
        )

        val cacheFile = File(context.cacheDir, cacheFileName)
        cacheFile.outputStream().use {
            Json.encodeToStream(dataCache, it)
        }
    }

    override suspend fun deleteOrganizations(
        incidentId: Long,
        dataIndex: Int,
    ) = coroutineScope {
        val cacheFileName = cacheFileName(incidentId, dataIndex)
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

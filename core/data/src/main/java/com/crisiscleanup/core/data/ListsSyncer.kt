package com.crisiscleanup.core.data

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkList
import com.crisiscleanup.core.network.model.tryThrowException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

interface ListsSyncer {
    suspend fun sync()
}

// TODO Test coverage

class AccountListsSyncer @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    @Logger(CrisisCleanupLoggers.Lists) private val logger: AppLogger,
) : ListsSyncer {
    override suspend fun sync() = coroutineScope {
        var networkCount = 0
        var requestingCount = 0
        val cachedLists = mutableListOf<NetworkList>()
        try {
            while (networkCount == 0 || requestingCount < networkCount) {
                logger.logDebug("Get lists $requestingCount $networkCount")
                val result = networkDataSource.getLists(1000, requestingCount)
                result.errors?.tryThrowException()

                if (networkCount == 0) {
                    networkCount = result.count!!
                }

                result.results?.let { lists ->
                    requestingCount += lists.size
                    cachedLists.addAll(lists)
                }

                if ((result.results?.size ?: 0) == 0) {
                    break
                }

                // TODO Cache data to file if gets too large
                if (cachedLists.size > 10000) {
                    logger.logException(Exception("Ignoring lists beyond ${cachedLists.size}"))
                    break
                }

                ensureActive()
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }

            logger.logException(e)
        }

        logger.logDebug("Save lists ${cachedLists.size}")
    }
}
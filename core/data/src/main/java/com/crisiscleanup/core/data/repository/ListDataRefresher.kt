package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.ListsDataSyncer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Singleton
class ListDataRefresher @Inject constructor(
    private val listsSyncer: ListsDataSyncer,
    @Logger(CrisisCleanupLoggers.Lists) private val logger: AppLogger,
) {
    private var dataUpdateTime = Instant.fromEpochSeconds(0)

    suspend fun refreshListData(
        force: Boolean = false,
        cacheTimeSpan: Duration = 1.hours,
    ) {
        if (!force && dataUpdateTime.plus(cacheTimeSpan) > Clock.System.now()) {
            return
        }

        try {
            listsSyncer.sync()
            dataUpdateTime = Clock.System.now()
        } catch (e: Exception) {
            logger.logException(e)
        }
    }
}

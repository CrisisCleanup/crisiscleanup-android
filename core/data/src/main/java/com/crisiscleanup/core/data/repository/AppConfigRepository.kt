package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.AppConfigDataSource
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import javax.inject.Inject

interface AppConfigRepository {
    suspend fun pullAppConfig()
}

class CrisisCleanupAppConfigRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val appConfigDataSource: AppConfigDataSource,
) : AppConfigRepository {
    override suspend fun pullAppConfig() {
        val thresholds = networkDataSource.getClaimThresholds()
        appConfigDataSource.setClaimThresholds(
            thresholds.workTypeCount,
            thresholds.workTypeClosedRatio,
        )
    }
}

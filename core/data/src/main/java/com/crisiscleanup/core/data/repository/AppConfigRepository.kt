package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.AppConfigDataSource
import com.crisiscleanup.core.model.data.AppConfigData
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AppConfigRepository {
    val appConfig: Flow<AppConfigData>

    suspend fun pullAppConfig()
}

class CrisisCleanupAppConfigRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val appConfigDataSource: AppConfigDataSource,
) : AppConfigRepository {
    override val appConfig = appConfigDataSource.appConfigData

    override suspend fun pullAppConfig() {
        val thresholds = networkDataSource.getClaimThresholds()
        appConfigDataSource.setClaimThresholds(
            thresholds.workTypeCount,
            thresholds.workTypeClosedRatio,
        )
    }
}

package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.AppConfigData
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppConfigDataSource @Inject constructor(
    private val appConfig: DataStore<AppConfig>,
) {
    val appConfigData = appConfig.data
        .map {
            AppConfigData(
                claimCountThreshold = it.claimedWorkTypeCountThreshold,
                closedClaimRatioThreshold = it.claimedWorkTypeClosedRatioThreshold,
            )
        }

    suspend fun setClaimThresholds(count: Int, ratio: Float) {
        appConfig.updateData {
            it.copy {
                claimedWorkTypeCountThreshold = count
                claimedWorkTypeClosedRatioThreshold = ratio
            }
        }
    }
}

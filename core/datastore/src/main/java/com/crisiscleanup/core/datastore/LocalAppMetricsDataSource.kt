package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.AppMetricsData
import com.crisiscleanup.core.model.data.AppOpenInstant
import com.crisiscleanup.core.model.data.BuildEndOfLife
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

class LocalAppMetricsDataSource @Inject constructor(
    private val appMetrics: DataStore<AppMetrics>,
) {
    val metrics = appMetrics.data
        .map {
            AppMetricsData(
                earlybirdEndOfLife = BuildEndOfLife(
                    Instant.fromEpochSeconds(it.earlybirdBuildEnd.endSeconds),
                    it.earlybirdBuildEnd.title,
                    it.earlybirdBuildEnd.message,
                    it.earlybirdBuildEnd.appLink,
                ),

                appOpen = AppOpenInstant(
                    it.appOpenVersion,
                    Instant.fromEpochSeconds(it.appOpenSeconds),
                ),

                switchToProductionApiVersion = it.productionApiSwitchVersion,
            )
        }

    suspend fun setEarlybirdEnd(end: BuildEndOfLife) {
        val builder = AppEndUseProto.newBuilder()
        builder.endSeconds = end.endDate.epochSeconds
        builder.title = end.title
        builder.message = end.message
        builder.appLink = end.link
        appMetrics.updateData {
            it.copy {
                earlybirdBuildEnd = builder.build()
            }
        }
    }

    suspend fun setAppOpen(
        appVersion: Long,
        timestamp: Instant = Clock.System.now(),
    ) {
        appMetrics.updateData {
            it.copy {
                appOpenVersion = appVersion
                appOpenSeconds = timestamp.epochSeconds
            }
        }
    }
}

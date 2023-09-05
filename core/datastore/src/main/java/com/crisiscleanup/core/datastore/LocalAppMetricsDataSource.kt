package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.model.data.AppMetricsData
import com.crisiscleanup.core.model.data.AppOpenInstant
import com.crisiscleanup.core.model.data.BuildEndOfLife
import com.crisiscleanup.core.model.data.MinSupportedAppVersion
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

class LocalAppMetricsDataSource @Inject constructor(
    private val appMetrics: DataStore<AppMetrics>,
    private val appVersionProvider: AppVersionProvider,
) {
    val metrics = appMetrics.data
        .map {
            val ebEnd = it.earlybirdBuildEnd
            val buildSupport = it.minBuildSupport
            AppMetricsData(
                earlybirdEndOfLife = BuildEndOfLife(
                    Instant.fromEpochSeconds(ebEnd.endSeconds),
                    ebEnd.title,
                    ebEnd.message,
                    ebEnd.appLink,
                ),

                appOpen = AppOpenInstant(
                    it.appOpenVersion,
                    Instant.fromEpochSeconds(it.appOpenSeconds),
                ),

                switchToProductionApiVersion = it.productionApiSwitchVersion,

                minSupportedAppVersion = MinSupportedAppVersion(
                    minBuild = buildSupport.minVersion,
                    title = buildSupport.title,
                    message = buildSupport.message,
                    link = buildSupport.appLink,
                    isUnsupported = buildSupport.minVersion > appVersionProvider.versionCode,
                ),
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

    suspend fun setMinSupportedAppVersion(supportedAppVersion: MinSupportedAppVersion) {
        val builder = AppMinUseProto.newBuilder()
        builder.minVersion = supportedAppVersion.minBuild
        builder.title = supportedAppVersion.title
        builder.message = supportedAppVersion.message
        builder.appLink = supportedAppVersion.link
        appMetrics.updateData {
            it.copy {
                minBuildSupport = builder.build()
            }
        }
    }

    suspend fun setProductionApiSwitch(appVersion: Long) {
        appMetrics.updateData {
            it.copy {
                productionApiSwitchVersion = appVersion
            }
        }
    }
}

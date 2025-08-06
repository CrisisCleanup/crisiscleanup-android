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

                appInstallVersion = it.appInstallVersion,
                appPublishedVersion = it.appPublishedVersion,
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
        timestamp: Instant = Clock.System.now(),
    ) {
        val appVersion = appVersionProvider.versionCode
        appMetrics.updateData {
            val installedVersion = it.appInstallVersion
            it.copy {
                appOpenVersion = appVersion
                appOpenSeconds = timestamp.epochSeconds
                appInstallVersion = if (installedVersion <= 0) appVersion else installedVersion
            }
        }
    }

    suspend fun setAppVersions(
        supportedAppVersion: MinSupportedAppVersion,
        publishedVersion: Long,
    ) {
        val builder = AppMinUseProto.newBuilder()
        builder.minVersion = supportedAppVersion.minBuild
        builder.title = supportedAppVersion.title
        builder.message = supportedAppVersion.message
        builder.appLink = supportedAppVersion.link
        appMetrics.updateData {
            it.copy {
                minBuildSupport = builder.build()
                appPublishedVersion = publishedVersion
            }
        }
    }

    @Deprecated("From early development publishing a whitelisted app pointing to staging which eventually pointed to production.")
    suspend fun setProductionApiSwitch(appVersion: Long) {
        appMetrics.updateData {
            it.copy {
                productionApiSwitchVersion = appVersion
            }
        }
    }
}

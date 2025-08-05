package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.AppVersionProvider
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.LocalAppMetricsDataSource
import com.crisiscleanup.core.model.data.AppMetricsData
import com.crisiscleanup.core.model.data.BuildEndOfLife
import com.crisiscleanup.core.model.data.MinSupportedAppVersion
import com.crisiscleanup.core.network.appsupport.AppSupportClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

// TODO Rename to AppInfoRepository
interface LocalAppMetricsRepository {
    val metrics: Flow<AppMetricsData>
    val isAppUpdateAvailable: Flow<Boolean>

    suspend fun setEarlybirdEnd(end: BuildEndOfLife)

    suspend fun setAppOpen(
        timestamp: Instant = Clock.System.now(),
    )

    fun saveAppSupportInfo()
}

// TODO Rename to AppInfoRepositoryImpl
@Singleton
class AppMetricsRepository @Inject constructor(
    private val dataSource: LocalAppMetricsDataSource,
    private val appSupportNetworkDataSource: AppSupportClient,
    appVersionProvider: AppVersionProvider,
    private val appEnv: AppEnv,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : LocalAppMetricsRepository {
    override val metrics: Flow<AppMetricsData> = dataSource.metrics

    override val isAppUpdateAvailable = metrics.mapLatest {
        appVersionProvider.versionCode < it.appPublishedVersion
    }

    override suspend fun setEarlybirdEnd(end: BuildEndOfLife) {
        dataSource.setEarlybirdEnd(end)
    }

    override suspend fun setAppOpen(timestamp: Instant) {
        dataSource.setAppOpen(timestamp)
    }

    override fun saveAppSupportInfo() {
        if (appEnv.isProduction) {
            externalScope.launch(ioDispatcher) {
                try {
                    appSupportNetworkDataSource.getAppSupportInfo(appEnv.isNotProduction)
                        ?.let { info ->
                            dataSource.setAppVersions(
                                MinSupportedAppVersion(
                                    minBuild = info.minBuildVersion,
                                    title = info.title ?: "",
                                    message = info.message,
                                    link = info.link ?: "",
                                ),
                                publishedVersion = info.publishedVersion ?: 0,
                            )
                        }
                } catch (e: Exception) {
                    logger.logException(e)
                }
            }
        }
    }
}

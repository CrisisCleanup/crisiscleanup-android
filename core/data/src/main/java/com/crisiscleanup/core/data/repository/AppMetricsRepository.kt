package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.LocalAppMetricsDataSource
import com.crisiscleanup.core.model.data.AppMetricsData
import com.crisiscleanup.core.model.data.BuildEndOfLife
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface LocalAppMetricsRepository {
    val metrics: Flow<AppMetricsData>

    suspend fun setEarlybirdEnd(end: BuildEndOfLife)

    suspend fun setAppOpen(
        appVersion: Long,
        timestamp: Instant = Clock.System.now(),
    )
}

@Singleton
class AppMetricsRepository @Inject constructor(
    private val dataSource: LocalAppMetricsDataSource,
) : LocalAppMetricsRepository {
    override val metrics: Flow<AppMetricsData> = dataSource.metrics

    override suspend fun setEarlybirdEnd(end: BuildEndOfLife) {
        dataSource.setEarlybirdEnd(end)
    }

    override suspend fun setAppOpen(
        appVersion: Long,
        timestamp: Instant,
    ) {
        dataSource.setAppOpen(appVersion, timestamp)
    }
}

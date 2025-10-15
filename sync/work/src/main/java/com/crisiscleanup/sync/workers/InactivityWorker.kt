package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.LocalAppMetricsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

private const val REPEAT_DURATION_DAYS = 2L

@HiltWorker
internal class InactivityWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    appMetricsRepository: LocalAppMetricsRepository,
    private val appDataManagementRepository: AppDataManagementRepository,
    private val appEnv: AppEnv,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {
    private val appMetrics = appMetricsRepository.metrics

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val latestAppOpen = appMetrics.first().appOpen.date
        val delta = Clock.System.now().minus(latestAppOpen)
        val clearDuration = if (appEnv.isProduction) {
            60.days
        } else if (appEnv.isDebuggable) {
            3.days
        } else {
            6.days
        }
        when {
            delta in clearDuration..999.days -> {
                if (appDataManagementRepository.backgroundClearAppData(false)) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }

            else -> {
                val daysToClear = clearDuration.minus(delta)
                logger.logDebug("App will clear in ${daysToClear.inWholeDays} days due to inactivity.")
                Result.success()
            }
        }
    }

    companion object {
        fun periodicWork() = PeriodicWorkRequestBuilder<DelegatingWorker>(
            REPEAT_DURATION_DAYS,
            TimeUnit.DAYS,
        )
            .setInputData(InactivityWorker::class.delegatedData())
            .build()
    }
}

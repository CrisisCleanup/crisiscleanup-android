package com.crisiscleanup.sync.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.AppMetricsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

private val clearDuration = 180.days
private const val REPEAT_DURATION_DAYS = 15L

@HiltWorker
internal class InactivityWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    appMetricsRepository: AppMetricsRepository,
    private val appDataManagementRepository: AppDataManagementRepository,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : CoroutineWorker(appContext, workerParams) {
    private val appMetrics = appMetricsRepository.metrics

    override suspend fun doWork(): Result = withContext(ioDispatcher) {
        val latestAppOpen = appMetrics.first().appOpen.date
        val delta = Clock.System.now().minus(latestAppOpen)
        val halfToClearDuration = clearDuration.times(0.5)
        when {
            delta >= clearDuration -> {
                if (appDataManagementRepository.backgroundClearAppData(false)) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }

            delta >= halfToClearDuration -> {
                // TODO Log halfway to clear
                Result.success()
            }

            else -> Result.success()
        }
    }

    companion object {
        fun periodicWork() = PeriodicWorkRequestBuilder<DelegatingWorker>(
            REPEAT_DURATION_DAYS,
            TimeUnit.DAYS,
        )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(InactivityWorker::class.delegatedData())
            .build()
    }
}

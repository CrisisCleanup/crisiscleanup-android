package com.crisiscleanup

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.data.repository.AppDataManagementRepository
import com.crisiscleanup.core.data.repository.ClearAppDataStep
import com.crisiscleanup.core.data.repository.LocalAppMetricsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.atomic.AtomicBoolean

class SwitchProductionApiManager(
    private val appMetricsRepository: LocalAppMetricsRepository,
    private val appDataRepository: AppDataManagementRepository,
    private val logger: AppLogger,
    coroutineScope: CoroutineScope,
) {
    private val hasRunSwitchover = AtomicBoolean()
    private val productionSwitchStep = appDataRepository.clearingAppDataStep
        .map {
            if (appDataRepository.clearAppDataError != ClearAppDataStep.None) {
                logger.logException(Exception("Switchover failed $it"))
            } else {
                logger.logCapture("Switchover on $it")
            }
            it
        }
    val isSwitchingToProduction = productionSwitchStep
        .map { it != ClearAppDataStep.None }
        .stateIn(
            scope = coroutineScope,
            initialValue = false,
            started = SharingStarted.WhileSubscribed(),
        )

    private val productionSwitchMessageLookup = mapOf(
        ClearAppDataStep.None to "Getting ready...",
        ClearAppDataStep.StopSyncPull to "Stopping background data sync...",
        ClearAppDataStep.SyncPush to "Saving changes to cloud...",
        ClearAppDataStep.ClearData to "Clearing existing data...",
        ClearAppDataStep.DatabaseNotCleared to "Unable to clear data",
        ClearAppDataStep.FinalClear to "Finalizing",
        ClearAppDataStep.Cleared to "App is ready!",
    )
    val productionSwitchMessage = productionSwitchStep
        .map {
            productionSwitchMessageLookup[it] ?: "Something is happening..."
        }
        .stateIn(
            scope = coroutineScope,
            initialValue = "",
            started = SharingStarted.WhileSubscribed(),
        )

    suspend fun switchToProduction() {
        if (!hasRunSwitchover.getAndSet(true) &&
            appMetricsRepository.metrics.first().switchToProductionApiVersion < 143
        ) {
            appDataRepository.clearAppData()
        }
    }
}

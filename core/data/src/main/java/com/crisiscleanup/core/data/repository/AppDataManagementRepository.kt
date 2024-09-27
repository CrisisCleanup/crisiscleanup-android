package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.DatabaseOperator
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteSyncStatDao
import com.crisiscleanup.core.database.dao.fts.rebuildIncidentFts
import com.crisiscleanup.core.database.dao.fts.rebuildOrganizationFts
import com.crisiscleanup.core.database.dao.fts.rebuildWorksiteTextFts
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

interface AppDataManagementRepository {
    val clearingAppDataStep: Flow<ClearAppDataStep>
    val clearAppDataError: ClearAppDataStep
    val isAppDataCleared: Flow<Boolean>

    suspend fun rebuildFts()

    suspend fun clearAppData()

    suspend fun isAppDataCleared(): Boolean
}

enum class ClearAppDataStep {
    None,
    StopSyncPull,
    SyncPush,
    ClearData,
    DatabaseNotCleared,
    FinalClear,
    Cleared,
}

class CrisisCleanupDataManagementRepository @Inject constructor(
    private val incidentDaoPlus: IncidentDaoPlus,
    private val organizationDaoPlus: IncidentOrganizationDaoPlus,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val accountDataRepository: AccountDataRepository,
    private val syncPuller: SyncPuller,
    private val databaseOperator: DatabaseOperator,
    private val incidentsRepository: IncidentsRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val worksiteSyncStatDao: WorksiteSyncStatDao,
    private val appMetricsRepository: AppMetricsRepository,
    private val languageTranslationsRepository: LanguageTranslationsRepository,
    private val workTypeStatusRepository: WorkTypeStatusRepository,
    private val authEventBus: AuthEventBus,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : AppDataManagementRepository {
    private val _clearingAppDataStep = MutableStateFlow(ClearAppDataStep.None)
    override val clearingAppDataStep: Flow<ClearAppDataStep> = _clearingAppDataStep
    override val isAppDataCleared = clearingAppDataStep.map { it == ClearAppDataStep.Cleared }

    override var clearAppDataError: ClearAppDataStep = ClearAppDataStep.None
        private set

    private val isClearingAppData = AtomicBoolean()

    override suspend fun rebuildFts() {
        incidentDaoPlus.rebuildIncidentFts()
        organizationDaoPlus.rebuildOrganizationFts()
        worksiteDaoPlus.rebuildWorksiteTextFts()
    }

    override suspend fun clearAppData() {
        if (isClearingAppData.getAndSet(true)) {
            return
        }

        externalScope.launch(ioDispatcher) {
            clearAppDataError = ClearAppDataStep.None

            try {
                if (incidentsRepository.getTableCount() == 0) {
                    appMetricsRepository.setProductionApiSwitch()
                    return@launch
                }

                accountDataRepository.clearAccountTokens()

                _clearingAppDataStep.value = ClearAppDataStep.StopSyncPull
                externalScope.launch(ioDispatcher) {
                    stopSyncPull()
                }
                withContext(Dispatchers.IO) {
                    TimeUnit.SECONDS.sleep(5)
                }

                _clearingAppDataStep.value = ClearAppDataStep.ClearData
                for (i in 0..<3) {
                    logger.logDebug("Clearing tables attempt $i")
                    databaseOperator.clearBackendDataTables()
                    withContext(Dispatchers.IO) {
                        TimeUnit.SECONDS.sleep(2)
                    }
                    if (isAppDataCleared()) {
                        break
                    }
                }

                _clearingAppDataStep.value = ClearAppDataStep.FinalClear

                withContext(Dispatchers.IO) {
                    TimeUnit.SECONDS.sleep(3)
                }

                ensureActive()

                if (!isAppDataCleared()) {
                    clearAppDataError = ClearAppDataStep.DatabaseNotCleared
                    return@launch
                }

                appMetricsRepository.setProductionApiSwitch()

                authEventBus.onLogout()

                _clearingAppDataStep.value = ClearAppDataStep.Cleared

                languageTranslationsRepository.loadLanguages(true)
                workTypeStatusRepository.loadStatuses(true)
            } finally {
                _clearingAppDataStep.value = ClearAppDataStep.None
                isClearingAppData.getAndSet(false)
            }
        }
    }

    private fun stopSyncPull() {
        syncPuller.stopPullIncident()
        syncPuller.stopPull()
        syncPuller.stopSyncPullWorksitesFull()
    }

    override suspend fun isAppDataCleared(): Boolean {
        return incidentsRepository.getTableCount() == 0 &&
            worksiteChangeRepository.getTableCount() == 0L &&
            worksiteSyncStatDao.getTableCount() == 0L
    }
}

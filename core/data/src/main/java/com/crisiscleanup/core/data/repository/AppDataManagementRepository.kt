package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.DatabaseOperator
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AccountEventBus
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.dao.IncidentDataSyncParameterDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.fts.rebuildIncidentFts
import com.crisiscleanup.core.database.dao.fts.rebuildOrganizationFts
import com.crisiscleanup.core.database.dao.fts.rebuildWorksiteTextFts
import com.crisiscleanup.core.model.data.CasesFilter
import com.crisiscleanup.core.model.data.InitialIncidentWorksitesCachePreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

    fun clearAppData()
    suspend fun backgroundClearAppData(refreshBackendData: Boolean): Boolean
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
    private val incidentDataSyncParameterDao: IncidentDataSyncParameterDao,
    private val incidentCacheRepository: IncidentCacheRepository,
    private val languageTranslationsRepository: LanguageTranslationsRepository,
    private val workTypeStatusRepository: WorkTypeStatusRepository,
    private val casesFilterRepository: CasesFilterRepository,
    private val appMetricsRepository: LocalAppMetricsRepository,
    private val accountEventBus: AccountEventBus,
    @ApplicationScope private val externalScope: CoroutineScope,
    @Dispatcher(CrisisCleanupDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : AppDataManagementRepository {
    override val clearingAppDataStep = MutableStateFlow(ClearAppDataStep.None)
    override val isAppDataCleared = clearingAppDataStep.map { it == ClearAppDataStep.Cleared }

    override var clearAppDataError: ClearAppDataStep = ClearAppDataStep.None
        private set

    private val isClearingAppData = AtomicBoolean()

    override suspend fun rebuildFts() {
        incidentDaoPlus.rebuildIncidentFts()
        organizationDaoPlus.rebuildOrganizationFts()
        worksiteDaoPlus.rebuildWorksiteTextFts()
    }

    override fun clearAppData() {
        externalScope.launch(ioDispatcher) {
            backgroundClearAppData(true)
        }
    }

    override suspend fun backgroundClearAppData(refreshBackendData: Boolean): Boolean =
        withContext(ioDispatcher) {
            if (!isClearingAppData.compareAndSet(false, true)) {
                return@withContext false
            }

            clearAppDataError = ClearAppDataStep.None

            try {
                if (incidentsRepository.incidentCount == 0L) {
                    return@withContext true
                }

                accountDataRepository.clearAccountTokens()

                clearingAppDataStep.value = ClearAppDataStep.StopSyncPull
                stopSyncPull()
                for (i in 0..9) {
                    TimeUnit.SECONDS.sleep(6)
                    if (isSyncPullStopped()) {
                        break
                    }
                }

                clearingAppDataStep.value = ClearAppDataStep.ClearData
                for (i in 0..<3) {
                    clearPersistedAppData()

                    TimeUnit.SECONDS.sleep(2)
                    if (isPersistedAppDataCleared()) {
                        break
                    }
                }

                clearingAppDataStep.value = ClearAppDataStep.FinalClear

                TimeUnit.SECONDS.sleep(3)

                ensureActive()

                if (!isPersistedAppDataCleared()) {
                    clearAppDataError = ClearAppDataStep.DatabaseNotCleared
                    logger.logCapture("Unable to clear app data")
                    return@withContext false
                }

                accountEventBus.onLogout()

                clearingAppDataStep.value = ClearAppDataStep.Cleared

                if (refreshBackendData) {
                    languageTranslationsRepository.loadLanguages(true)
                    workTypeStatusRepository.loadStatuses(true)
                }

                return@withContext true
            } catch (e: Exception) {
                logger.logException(e)
                return@withContext false
            } finally {
                clearingAppDataStep.value = ClearAppDataStep.None
                isClearingAppData.getAndSet(false)
            }
        }

    private fun stopSyncPull() {
        // TODO Stop all including
        //      - teams
        syncPuller.stopPullWorksites()
    }

    private suspend fun isSyncPullStopped(): Boolean {
        val cacheStage = incidentCacheRepository.cacheStage.first()
        return !cacheStage.isSyncingStage
    }

    private suspend fun clearPersistedAppData() {
        databaseOperator.clearBackendDataTables()
        // App preferences resets on logout
        // Account info clears on logout
        casesFilterRepository.changeFilters(CasesFilter())
        incidentCacheRepository.updateCachePreferences(InitialIncidentWorksitesCachePreferences)
        appMetricsRepository.setAppOpen()
    }

    private fun isPersistedAppDataCleared() =
        incidentsRepository.incidentCount == 0L && worksiteChangeRepository.worksiteChangeCount == 0L && incidentDataSyncParameterDao.getSyncStatCount() == 0
}

package com.crisiscleanup.sync

import android.content.Context
import com.crisiscleanup.core.common.KeyResourceTranslator
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.core.data.incidentcache.IncidentDataPullReporter
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentCacheRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.sync.initializers.scheduleSyncMedia
import com.crisiscleanup.sync.initializers.scheduleSyncWorksites
import com.crisiscleanup.sync.notification.IncidentDataSyncNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class AppSyncer @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val incidentCacheRepository: IncidentCacheRepository,
    incidentDataPullReporter: IncidentDataPullReporter,
    private val languageRepository: LanguageTranslationsRepository,
    private val statusRepository: WorkTypeStatusRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val networkMonitor: NetworkMonitor,
    translator: KeyResourceTranslator,
    @ApplicationContext private val context: Context,
    @Logger(CrisisCleanupLoggers.Sync) private val logger: AppLogger,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SyncPuller, SyncPusher {
    private val pullJobLock = Object()
    private var pullJob: Job? = null

    private val languagePullMutex = Mutex()

    private val incidentDataSyncNotifier = IncidentDataSyncNotifier(
        context,
        incidentDataPullReporter,
        this,
        translator,
        logger,
        applicationScope,
    )

    private suspend fun isNotOnline(): SyncResult? {
        val isOnline = networkMonitor.isOnline.first()
        if (!isOnline) {
            return SyncResult.NotAttempted("Not online")
        }

        return null
    }

    private suspend fun isAccountTokensInvalid(): SyncResult? {
        accountDataRepository.updateAccountTokens()
        val hasValidAccountTokens = accountDataRepository.accountData.first().areTokensValid
        if (!hasValidAccountTokens) {
            return SyncResult.InvalidAccountTokens
        }

        return null
    }

    private suspend fun noPushConditions(): SyncResult? {
        isNotOnline()?.let {
            return it
        }

        isAccountTokensInvalid()?.let {
            return it
        }

        return null
    }

    override fun appPullIncidentData(
        cancelOngoing: Boolean,
        forcePullIncidents: Boolean,
        cacheSelectedIncident: Boolean,
        cacheActiveIncidentWorksites: Boolean,
        cacheFullWorksites: Boolean,
        restartCacheCheckpoint: Boolean,
    ) {
        applicationScope.launch(ioDispatcher) {
            syncPullIncidentData(
                cancelOngoing = cancelOngoing,
                forcePullIncidents = forcePullIncidents,
                cacheSelectedIncident = cacheSelectedIncident,
                cacheActiveIncidentWorksites = cacheActiveIncidentWorksites,
                cacheFullWorksites = cacheFullWorksites,
                restartCacheCheckpoint = restartCacheCheckpoint,
            )
        }
    }

    override suspend fun syncPullIncidentData(
        cancelOngoing: Boolean,
        forcePullIncidents: Boolean,
        cacheSelectedIncident: Boolean,
        cacheActiveIncidentWorksites: Boolean,
        cacheFullWorksites: Boolean,
        restartCacheCheckpoint: Boolean,
    ): SyncResult {
        isAccountTokensInvalid()?.let {
            return it
        }

        val isPlanSubmitted = incidentCacheRepository.submitPlan(
            overwriteExisting = cancelOngoing,
            forcePullIncidents = forcePullIncidents,
            cacheSelectedIncident = cacheSelectedIncident,
            cacheActiveIncidentWorksites = cacheActiveIncidentWorksites,
            cacheWorksitesAdditional = cacheFullWorksites,
            restartCacheCheckpoint = restartCacheCheckpoint,
        )
        if (!isPlanSubmitted) {
            return SyncResult.NotAttempted("Sync is redundant or unnecessary")
        }

        return try {
            val job: Deferred<SyncResult>
            synchronized(pullJobLock) {
                stopPullWorksites()

                job = applicationScope.async(ioDispatcher) {
                    return@async incidentDataSyncNotifier.notifySync {
                        incidentCacheRepository.sync()
                    }
                }
                pullJob = job
            }
            return job.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.logException(e)
            SyncResult.Error(e.message ?: "Sync Incidents error")
        }
    }

    override fun stopPullWorksites() {
        synchronized(pullJobLock) {
            pullJob?.cancel()
        }
    }

    private suspend fun languagePull() {
        if (languagePullMutex.tryLock()) {
            try {
                languageRepository.loadLanguages()
            } finally {
                languagePullMutex.unlock()
            }
        }
    }

    override fun appPullLanguage() {
        applicationScope.launch(ioDispatcher) {
            syncPullLanguage()
        }
    }

    override suspend fun syncPullLanguage(): SyncResult {
        isNotOnline()?.let {
            return it
        }

        return try {
            languagePull()
            SyncResult.Success("Language pulled")
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Language pull fail")
        }
    }

    override fun appPullStatuses() {
        applicationScope.launch(ioDispatcher) {
            statusRepository.loadStatuses()
        }
    }

    override suspend fun syncPullStatuses(): SyncResult {
        isNotOnline()?.let {
            return it
        }

        return try {
            statusRepository.loadStatuses()
            SyncResult.Success("Statuses pulled")
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Statuses pull fail")
        }
    }

    override fun appPushWorksite(worksiteId: Long, scheduleMediaSync: Boolean) {
        applicationScope.launch(ioDispatcher) {
            noPushConditions()?.let {
                return@launch
            }

            if (worksiteChangeRepository.trySyncWorksite(worksiteId)) {
                worksiteChangeRepository.syncUnattemptedWorksite(worksiteId)

                if (scheduleMediaSync) {
                    scheduleSyncMedia()
                }
            }
        }
    }

    override suspend fun syncPushWorksitesAsync() = applicationScope.async {
        syncPushWorksites()
    }

    override suspend fun syncPushWorksites(): SyncResult {
        noPushConditions()?.let {
            return it
        }

        val isSyncAttempted = worksiteChangeRepository.syncWorksites()
        return if (isSyncAttempted) {
            SyncResult.Success("")
        } else {
            SyncResult.NotAttempted("Sync not attempted")
        }
    }

    override suspend fun syncPushMedia(): SyncResult {
        noPushConditions()?.let {
            return it
        }

        return try {
            val isSyncAll = worksiteChangeRepository.syncWorksiteMedia()
            return if (isSyncAll) {
                SyncResult.Success("")
            } else {
                SyncResult.Partial("Sync partial worksite media")
            }
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Sync media fail")
        }
    }

    override fun scheduleSyncMedia() = scheduleSyncMedia(context)

    override fun scheduleSyncWorksites() = scheduleSyncWorksites(context)
}

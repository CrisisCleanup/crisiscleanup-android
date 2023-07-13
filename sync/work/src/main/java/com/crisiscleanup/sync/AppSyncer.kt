package com.crisiscleanup.sync

import android.content.Context
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.common.sync.SyncPuller
import com.crisiscleanup.core.common.sync.SyncPusher
import com.crisiscleanup.core.common.sync.SyncResult
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.IncidentsRepository
import com.crisiscleanup.core.data.repository.LanguageTranslationsRepository
import com.crisiscleanup.core.data.repository.WorkTypeStatusRepository
import com.crisiscleanup.core.data.repository.WorksiteChangeRepository
import com.crisiscleanup.core.data.repository.WorksitesRepository
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.sync.SyncPull.determineSyncSteps
import com.crisiscleanup.sync.SyncPull.executePlan
import com.crisiscleanup.sync.initializers.scheduleSyncMedia
import com.crisiscleanup.sync.initializers.scheduleSyncWorksitesFull
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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

@Singleton
class AppSyncer @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val languageRepository: LanguageTranslationsRepository,
    private val statusRepository: WorkTypeStatusRepository,
    private val worksiteChangeRepository: WorksiteChangeRepository,
    private val appPreferences: LocalAppPreferencesDataSource,
    private val syncLogger: SyncLogger,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SyncPuller, SyncPusher {

    private val pullJobLock = Object()
    private var pullJob: Job? = null

    private val incidentPullJobLock = Object()
    private var incidentPullJob: Job? = null

    private val pullWorksitesFullJobLock = Object()
    private var pullWorksitesFullJob: Job? = null

    private val languagePullMutex = Mutex()

    private suspend fun notifyInvalidAccountToken(isInBackground: Boolean) {
        if (isInBackground) {
            val accountData = accountDataRepository.accountData.first()
            if (!accountData.areTokensValid) {
                // if (worksiteChangeRepository.hasPendingChanges()) {
                // TODO If pending changes exist show notification to login for syncing to finish
                // }
            }
        }
    }

    private suspend fun isNotOnline() = networkMonitor.isNotOnline.first()

    private suspend fun onSyncPreconditions(isInBackground: Boolean): SyncResult? {
        notifyInvalidAccountToken(isInBackground)

        if (isNotOnline()) {
            return SyncResult.NotAttempted("Not online")
        }

        // Other constraints are not important.
        // Validity of tokens are determined in the network layer
        // When app is running assume sync is necessary.
        // When app is in background assume Work constraints have been defined.

        return null
    }

    private suspend fun pull(force: Boolean): SyncResult {
        val unforcedPlan = determineSyncSteps(
            incidentsRepository,
            worksitesRepository,
            appPreferences,
        )
        val plan = if (force) unforcedPlan.copy(pullIncidents = true)
        else unforcedPlan
        if (!plan.requiresSync) {
            syncLogger.log("Skipping unforced sync")
            scheduleSyncWorksitesFull()
            return SyncResult.NotAttempted("Unforced sync not necessary")
        }

        try {
            executePlan(
                plan,
                incidentsRepository,
                worksitesRepository,
                syncLogger,
            )

            scheduleSyncWorksitesFull()
        } catch (e: Exception) {
            syncLogger.log("Sync pull fail. ${e.message}".trim())
            return SyncResult.Error(e.message ?: "Sync fail")
        }

        syncLogger.log("Sync pulled. force=$force")
        return SyncResult.Success(if (force) "Force pulled" else "Pulled")
    }

    override fun stopPull() {
        synchronized(pullJobLock) {
            pullJob?.cancel()
        }
    }

    private var incidentDeltaJob: Job? = null

    override fun appPullIncidentWorksitesDelta() {
        incidentDeltaJob?.cancel()
        incidentDeltaJob = applicationScope.launch {
            val incidentId = appPreferences.userData.first().selectedIncidentId
            incidentsRepository.getIncident(incidentId)?.let {
                worksitesRepository.getWorksiteSyncStats(incidentId)?.let { syncStats ->
                    if (syncStats.isDeltaPull) {
                        syncLogger.log("App pull $incidentId delta")
                        try {
                            worksitesRepository.refreshWorksites(
                                incidentId,
                                forceQueryDeltas = true,
                            )
                        } catch (e: Exception) {
                            if (e !is CancellationException) {
                                syncLogger.log("$incidentId delta fail ${e.message}")
                            }
                        } finally {
                            syncLogger.log("App pull $incidentId delta end")
                                .flush()
                        }
                    }
                }
            }
        }
    }

    override suspend fun syncPullAsync(): Deferred<SyncResult> {
        val deferred = applicationScope.async {
            onSyncPreconditions(true)?.let {
                return@async SyncResult.PreconditionsNotMet
            }

            synchronized(pullJobLock) {
                if (pullJob?.isActive == true) {
                    return@async SyncResult.NotAttempted("Pull sync is already in progress")
                }
            }

            return@async pull(false)
        }
        synchronized(pullJobLock) {
            pullJob = deferred
        }
        return deferred
    }

    override fun appPull(force: Boolean, cancelOngoing: Boolean) {
        synchronized(pullJobLock) {
            if (!cancelOngoing && pullJob?.isActive == true) {
                return
            }
        }

        applicationScope.launch {
            onSyncPreconditions(false)?.let { return@launch }

            synchronized(pullJobLock) {
                stopPull()
                pullJob = applicationScope.launch(ioDispatcher) {
                    pull(force)

                    syncLogger
                        .log("App pull end")
                        .flush()
                }
            }
        }
    }

    override fun stopPullIncident() {
        synchronized(incidentPullJobLock) {
            incidentPullJob?.cancel()
        }
    }

    private suspend fun incidentPull(id: Long) {
        // TODO Handle errors properly
        incidentsRepository.pullIncident(id)
        incidentsRepository.pullIncidentOrganizations(id)

        syncLogger.log("Incident $id pulled")
    }

    override suspend fun syncPullIncidentAsync(id: Long): Deferred<SyncResult> {
        val deferred = applicationScope.async {
            if (id == EmptyIncident.id) {
                return@async SyncResult.NotAttempted("Empty incident")
            }

            onSyncPreconditions(true)?.let {
                return@async SyncResult.PreconditionsNotMet
            }

            incidentPull(id)

            return@async SyncResult.Success("Incident $id pulled")
        }
        synchronized(incidentPullJobLock) {
            incidentPullJob = deferred
        }
        return deferred
    }

    override fun appPullIncident(id: Long) {
        if (id == EmptyIncident.id) {
            return
        }

        synchronized(incidentPullJobLock) {
            stopPullIncident()
            incidentPullJob = applicationScope.launch(ioDispatcher) {
                onSyncPreconditions(false)?.let { return@launch }

                try {
                    incidentPull(id)
                } catch (e: Exception) {
                    syncLogger.log("App pull incident fail. ${e.message}".trim())
                }

                syncLogger
                    .log("App pull incident end")
                    .flush()
            }
        }
    }

    override suspend fun syncPullWorksitesFullAsync(): Deferred<SyncResult> {
        synchronized(pullWorksitesFullJobLock) {
            stopSyncPullWorksitesFull()
            val deferred = applicationScope.async {
                onSyncPreconditions(true)?.let {
                    return@async SyncResult.PreconditionsNotMet
                }

                val incidentId = appPreferences.userData.first().selectedIncidentId
                return@async if (incidentId > 0) {
                    val isSynced = try {
                        worksitesRepository.syncWorksitesFull(incidentId)
                    } catch (e: CancellationException) {
                        true
                    }
                    if (isSynced) SyncResult.Success("Incident $incidentId worksites full")
                    else SyncResult.Partial("$incidentId full sync did not finish")
                } else {
                    SyncResult.NotAttempted("Incident not selected")
                }
            }
            pullWorksitesFullJob = deferred
            return deferred
        }
    }

    override fun stopSyncPullWorksitesFull() {
        synchronized(pullWorksitesFullJobLock) {
            pullWorksitesFullJob?.cancel()
        }
    }

    override fun scheduleSyncWorksitesFull() {
        stopSyncPullWorksitesFull()
        scheduleSyncWorksitesFull(context)
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
        if (isNotOnline()) {
            return SyncResult.NotAttempted("not-online")
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
        if (isNotOnline()) {
            return SyncResult.NotAttempted("not-online")
        }

        return try {
            statusRepository.loadStatuses()
            SyncResult.Success("Statuses pulled")
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Statuses pull fail")
        }
    }

    private var pushJob: Job? = null

    override fun stopPushWorksites() {
        pushJob?.cancel()
    }

    override fun appPushWorksite(worksiteId: Long) {
        applicationScope.launch(ioDispatcher) {
            onSyncPreconditions(false)?.let {
                return@launch
            }

            if (worksiteChangeRepository.trySyncWorksite(worksiteId)) {
                worksiteChangeRepository.syncUnattemptedWorksite(worksiteId)
            }
        }
    }

    override suspend fun syncPushWorksitesAsync(): Deferred<SyncResult> {
        val deferred = applicationScope.async {
            onSyncPreconditions(true)?.let {
                return@async SyncResult.PreconditionsNotMet
            }

            val isSyncAttempted = worksiteChangeRepository.syncWorksites()
            return@async if (isSyncAttempted) SyncResult.Success("")
            else SyncResult.NotAttempted("Sync not attempted")
        }
        pushJob = deferred
        return deferred
    }

    override suspend fun syncPushMedia(): SyncResult {
        onSyncPreconditions(true)?.let {
            return SyncResult.PreconditionsNotMet
        }

        return try {
            val isSyncAll = worksiteChangeRepository.syncWorksiteMedia()
            return if (isSyncAll) SyncResult.Success("")
            else SyncResult.Partial("Sync partial worksite media")
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Sync media fail")
        }
    }

    override fun scheduleSyncMedia() = scheduleSyncMedia(context)
}

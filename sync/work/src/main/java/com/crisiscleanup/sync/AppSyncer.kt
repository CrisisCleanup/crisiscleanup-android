package com.crisiscleanup.sync

import com.crisiscleanup.core.common.SyncPuller
import com.crisiscleanup.core.common.SyncPusher
import com.crisiscleanup.core.common.SyncResult
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventManager
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.data.repository.*
import com.crisiscleanup.core.data.util.NetworkMonitor
import com.crisiscleanup.core.datastore.LocalAppPreferencesDataSource
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.sync.SyncPull.determineSyncSteps
import com.crisiscleanup.sync.SyncPull.executePlan
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync service to pull data while the app is in the foreground
 */
@Singleton
class AppSyncer @Inject constructor(
    private val accountDataRepository: AccountDataRepository,
    private val incidentsRepository: IncidentsRepository,
    private val worksitesRepository: WorksitesRepository,
    private val languageRepository: LanguageTranslationsRepository,
    private val appPreferences: LocalAppPreferencesDataSource,
    private val syncLogger: SyncLogger,
    private val authEventManager: AuthEventManager,
    private val networkMonitor: NetworkMonitor,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
) : SyncPuller, SyncPusher {

    private val pullJobLock = Object()
    private var pullJob: Job? = null

    private val incidentPullJobLock = Object()
    private var incidentPullJob: Job? = null

    private val languagePullMutex = Mutex()

    private suspend fun isInvalidAccountToken(): Boolean {
        val accountData = accountDataRepository.accountData.first()
        if (accountData.isTokenInvalid) {
            authEventManager.onExpiredToken()
            return true
        }
        return false
    }

    private suspend fun isNotOnline() = networkMonitor.isNotOnline.first()

    private suspend fun onSyncPreconditions(): SyncResult? {
        if (isInvalidAccountToken()) {
            return SyncResult.NotAttempted("Invalid account token")
        }

        if (isNotOnline()) {
            return SyncResult.NotAttempted("Not online")
        }

        // Other constraints are not important.
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
            return SyncResult.NotAttempted("Unforced sync not necessary")
        }

        try {
            executePlan(
                plan,
                incidentsRepository,
                worksitesRepository,
                syncLogger,
            )
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

    override suspend fun syncPullAsync(): Deferred<SyncResult> {
        val deferred = applicationScope.async {
            onSyncPreconditions()?.let {
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
            onSyncPreconditions()?.let { return@launch }

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
        incidentsRepository.pullIncident(id)

        syncLogger.log("Incident $id pulled")
    }

    override suspend fun syncPullIncidentAsync(id: Long): Deferred<SyncResult> {
        val deferred = applicationScope.async {
            if (id == EmptyIncident.id) {
                return@async SyncResult.NotAttempted("Empty incident")
            }

            onSyncPreconditions()?.let {
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
                onSyncPreconditions()?.let { return@launch }

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

    private suspend fun languagePull() {
        if (languagePullMutex.tryLock()) {
            languageRepository.loadLanguages()
        }
    }

    override fun appPullLanguage() {
        applicationScope.launch(ioDispatcher) {
            syncPullLanguageAsync().await()
        }
    }

    override suspend fun syncPullLanguageAsync(): Deferred<SyncResult> {
        return applicationScope.async {
            if (isNotOnline()) {
                return@async SyncResult.NotAttempted("not-online")
            }

            try {
                languagePull()
                SyncResult.Success("Language pulled")
            } catch (e: Exception) {
                SyncResult.Error(e.message ?: "Language pull fail")
            }
        }
    }

    private val _isSyncPushing = MutableStateFlow(false)
    override val isSyncPushing: Flow<Boolean> = _isSyncPushing

    private val syncPushMutex = Mutex()
    private var pushJob: Job? = null

    override fun stopPushWorksite() {
        pushJob?.cancel()
    }

    private suspend fun pushWorksite(worksiteId: Long): SyncResult {
        onSyncPreconditions()?.let {
            return SyncResult.PreconditionsNotMet
        }

        return if (syncPushMutex.tryLock()) {
            _isSyncPushing.value = true
            try {
                // TODO
                SyncResult.NotAttempted("Not yet implemented")
            } finally {
                _isSyncPushing.value = false
            }
        } else {
            SyncResult.NotAttempted("Push sync is already in progress")
        }
    }

    override fun appPushWorksite(worksiteId: Long) {
        stopPushWorksite()
        pushJob = applicationScope.launch(ioDispatcher) {
            pushWorksite(worksiteId)
        }
    }

    override suspend fun syncPushWorksitesAsync(): Deferred<SyncResult> {
        val deferred = applicationScope.async {
            // TODO Loop through pending local changes one by one newest to oldest and sync each. Check for cancellation after change sync.
            ensureActive()

            return@async SyncResult.NotAttempted("Not yet implemented")
        }
        pushJob = deferred
        return deferred
    }
}

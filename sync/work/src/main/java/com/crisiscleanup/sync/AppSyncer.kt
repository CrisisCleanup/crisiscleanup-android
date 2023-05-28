package com.crisiscleanup.sync

import android.content.Context
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.event.AuthEventBus
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
    private val authEventBus: AuthEventBus,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context,
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
            authEventBus.onExpiredToken()
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
        incidentsRepository.pullIncidentOrganizations(id)

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
            onSyncPreconditions()?.let {
                return@launch
            }

            worksiteChangeRepository.trySyncWorksite(worksiteId)
        }
    }

    override suspend fun syncPushWorksitesAsync(): Deferred<SyncResult> {
        val deferred = applicationScope.async {
            onSyncPreconditions()?.let {
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
        onSyncPreconditions()?.let {
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

    override fun scheduleSyncMedia() {
        scheduleSyncMedia(context)
    }
}

package com.crisiscleanup.core.common.sync

import kotlinx.coroutines.Deferred

interface SyncPuller {
    /**
     * Cache all incidents and active incident worksites as directed
     *
     * @param cacheActiveIncidentWorksites Cache worksites data of the active incident
     * @param cacheFullWorksites Cache the full dataset of all worksites (after caching subset)
     * @param restartCacheCheckpoint Cache all worksites again rather than starting from last checkpoint
     */
    fun appPullIncidentData(
        cancelOngoing: Boolean = false,
        forcePullIncidents: Boolean = false,
        cacheSelectedIncident: Boolean = false,
        cacheActiveIncidentWorksites: Boolean = true,
        cacheFullWorksites: Boolean = false,
        restartCacheCheckpoint: Boolean = false,
    )

    fun appPullIncidents() = appPullIncidentData(
        cancelOngoing = true,
        forcePullIncidents = true,
        cacheSelectedIncident = true,
        cacheActiveIncidentWorksites = false,
    )

    suspend fun syncPullIncidentData(
        cancelOngoing: Boolean = false,
        forcePullIncidents: Boolean = false,
        cacheSelectedIncident: Boolean = false,
        cacheActiveIncidentWorksites: Boolean = true,
        cacheFullWorksites: Boolean = false,
        restartCacheCheckpoint: Boolean = false,
    ): SyncResult

    suspend fun syncPullIncidents() = syncPullIncidentData(
        cancelOngoing = true,
        forcePullIncidents = true,
        cacheSelectedIncident = true,
        cacheActiveIncidentWorksites = false,
    )

    fun stopPullWorksites()

    fun appPullLanguage()
    suspend fun syncPullLanguage(): SyncResult

    fun appPullStatuses()
    suspend fun syncPullStatuses(): SyncResult

    fun appPullAppConfig()
    suspend fun syncPullAppConfig(): SyncResult

    fun appPullEquipment(force: Boolean = false)
    suspend fun syncPullEquipment(): SyncResult
}

interface SyncPusher {
    fun appPushWorksite(worksiteId: Long, scheduleMediaSync: Boolean = false)
    suspend fun syncPushWorksitesAsync(): Deferred<SyncResult>
    suspend fun syncPushMedia(): SyncResult
    suspend fun syncPushWorksites(): SyncResult

    fun scheduleSyncMedia()
    fun scheduleSyncWorksites()
}

sealed interface SyncResult {
    data class NotAttempted(val reason: String) : SyncResult
    data class Success(val notes: String) : SyncResult
    data class Partial(val notes: String) : SyncResult
    data class Error(val message: String) : SyncResult
    data object InvalidAccountTokens : SyncResult
}

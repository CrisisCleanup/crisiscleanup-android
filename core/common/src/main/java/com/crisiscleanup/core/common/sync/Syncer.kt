package com.crisiscleanup.core.common.sync

import kotlinx.coroutines.Deferred

interface SyncPuller {
    fun appPull(force: Boolean, cancelOngoing: Boolean)
    suspend fun syncPullAsync(): Deferred<SyncResult>
    fun stopPull()

    suspend fun syncPullWorksitesFullAsync(): Deferred<SyncResult>
    fun stopSyncPullWorksitesFull()
    fun scheduleSyncWorksitesFull()

    suspend fun pullIncidents()
    fun appPullIncident(id: Long)
    suspend fun syncPullIncidentAsync(id: Long): Deferred<SyncResult>
    fun stopPullIncident()

    fun appPullIncidentWorksitesDelta()

    fun appPullLanguage()
    suspend fun syncPullLanguage(): SyncResult

    fun appPullStatuses()
    suspend fun syncPullStatuses(): SyncResult
}

interface SyncPusher {
    fun appPushWorksite(worksiteId: Long, scheduleMediaSync: Boolean = false)
    suspend fun syncPushWorksitesAsync(): Deferred<SyncResult>
    fun stopPushWorksites()
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
    data object PreconditionsNotMet : SyncResult
}

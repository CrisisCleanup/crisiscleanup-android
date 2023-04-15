package com.crisiscleanup.core.common

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

interface SyncPuller {
    fun appPull(force: Boolean, cancelOngoing: Boolean)
    suspend fun syncPullAsync(): Deferred<SyncResult>
    fun stopPull()

    fun appPullIncident(id: Long)
    suspend fun syncPullIncidentAsync(id: Long): Deferred<SyncResult>
    fun stopPullIncident()

    fun appPullLanguage()
    suspend fun syncPullLanguageAsync(): Deferred<SyncResult>
}

interface SyncPusher {
    val isSyncPushing: Flow<Boolean>

    fun appPushWorksite(worksiteId: Long)
    suspend fun syncPushWorksitesAsync(): Deferred<SyncResult>
    fun stopPushWorksites()
}

sealed interface SyncResult {
    data class NotAttempted(val reason: String) : SyncResult
    data class Success(val notes: String) : SyncResult
    data class Error(val message: String) : SyncResult
    object PreconditionsNotMet : SyncResult
}

interface SyncEventManager {
    fun registerObserver(observer: SyncObserver): Int
    fun unregisterObserver(observerId: Int)
}

data class SyncStats(
    val isSyncing: Boolean,
    val progress: Float,
)

interface SyncObserver {
    fun onSyncUpdate(stats: SyncStats)
}

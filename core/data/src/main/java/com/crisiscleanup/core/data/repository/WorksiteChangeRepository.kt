package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.dao.*
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.EmptyWorksite
import com.crisiscleanup.core.model.data.SavedWorksiteChange
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.worksitechange.WorksiteChangeSyncer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import java.lang.Integer.max
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

interface WorksiteChangeRepository {
    val syncingWorksiteId: StateFlow<Long>

    suspend fun saveWorksiteChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        primaryWorkType: WorkType,
        organizationId: Long,
    ): Long

    /**
     * @return TRUE if sync was attempted or FALSE otherwise
     */
    suspend fun syncWorksites(syncWorksiteCount: Int = 0): Boolean

    suspend fun trySyncWorksite(worksiteId: Long): Boolean
}

private const val MaxSyncTries = 3

@Singleton
class CrisisCleanupWorksiteChangeRepository @Inject constructor(
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteChangeDao: WorksiteChangeDao,
    private val worksiteChangeDaoPlus: WorksiteChangeDaoPlus,
    private val worksiteFlagDao: WorksiteFlagDao,
    private val worksiteNoteDao: WorksiteNoteDao,
    private val workTypeDao: WorkTypeDao,
    private val worksiteChangeSyncer: WorksiteChangeSyncer,
    private val accountDataRepository: AccountDataRepository,
    private val networkMonitor: NetworkMonitor,
    private val appEnv: AppEnv,
    private val syncLogger: SyncLogger,
    @Logger(CrisisCleanupLoggers.App) private val appLogger: AppLogger,
) : WorksiteChangeRepository {
    override val syncingWorksiteId = MutableStateFlow(EmptyWorksite.id)

    private val syncWorksiteMutex = Mutex()
    private val syncingWorksiteIds = ConcurrentHashMap<Long, Boolean>()

    override suspend fun saveWorksiteChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        primaryWorkType: WorkType,
        organizationId: Long
    ) = worksiteChangeDaoPlus.saveChange(
        worksiteStart,
        worksiteChange,
        primaryWorkType,
        organizationId,
    )

    override suspend fun syncWorksites(syncWorksiteCount: Int): Boolean = coroutineScope {
        if (syncWorksiteMutex.tryLock()) {
            var worksiteId: Long
            var isSyncClosed = false
            try {
                var syncCounter = 0
                val syncCountLimit = if (syncWorksiteCount < 1) 20 else syncWorksiteCount
                while (syncCounter++ < syncCountLimit) {
                    val worksiteIds = worksiteDao.getLocallyModifiedWorksites(1)
                    if (worksiteIds.isEmpty()) {
                        break
                    }
                    worksiteId = worksiteIds.first()

                    isSyncClosed = trySyncWorksite(worksiteId, true).second

                    ensureActive()
                }
            } catch (e: Exception) {
                // TODO Indicate error with notification
            } finally {
                syncWorksiteMutex.unlock()
                if (!isSyncClosed) {
                    syncLogger.flush()
                }
            }

            return@coroutineScope true
        }

        return@coroutineScope false
    }

    override suspend fun trySyncWorksite(worksiteId: Long) =
        trySyncWorksite(worksiteId, false).first

    private suspend fun trySyncWorksite(
        worksiteId: Long,
        rethrowError: Boolean,
    ): Pair<Boolean, Boolean> {
        if (networkMonitor.isNotOnline.first()) {
            syncLogger.log("Not syncing. No internet connection.")
            return Pair(false, false)
        }

        val accountData = accountDataRepository.accountData.first()
        if (accountData.isTokenInvalid) {
            syncLogger.log("Not syncing. Invalid account token.")
            return Pair(false, false)
        }

        var isSyncClosed = false
        try {
            synchronized(syncingWorksiteIds) {
                if (syncingWorksiteIds.contains(worksiteId)) {
                    syncLogger.log("Not syncing. Currently being synced.")
                    return Pair(false, false)
                }
                syncingWorksiteIds[worksiteId] = true
            }

            syncingWorksiteId.value = worksiteId

            isSyncClosed = syncWorksite(worksiteId)
        } catch (e: Exception) {
            appLogger.logException(e)

            if (rethrowError) {
                throw e
            } else {
                // TODO Indicate error visually
            }
        } finally {
            synchronized(syncingWorksiteIds) {
                syncingWorksiteIds.remove(worksiteId)
            }
            syncingWorksiteId.value = EmptyWorksite.id
        }
        return Pair(true, isSyncClosed)
    }

    private suspend fun syncWorksite(worksiteId: Long): Boolean {
        syncLogger.type = "syncing-worksite-$worksiteId"

        val sortedChanges = worksiteChangeDao.getOrdered(worksiteId)
        if (sortedChanges.isNotEmpty()) {
            syncLogger.log("${sortedChanges.size} changes.")

            val newestChange = sortedChanges.last()
            val newestChangeOrgId = newestChange.entity.organizationId
            val accountData = accountDataRepository.accountData.first()
            val organizationId = accountData.org.id
            if (newestChangeOrgId != organizationId) {
                syncLogger.log("Not syncing. Org mismatch $organizationId != $newestChangeOrgId.")
                // TODO Insert notice that newest change of worksite was with a different organization
                return false
            }

            syncLogger.log("Sync changes starting.")

            val worksiteChanges = sortedChanges.map { it.asExternalModel(MaxSyncTries) }
            syncWorksiteChanges(worksiteChanges)

            syncLogger.log("Sync changes over.")
        }

        // TODO Query unsynced data (in transaction) if exists and no changes and try syncing.

        val isSyncClosed = worksiteDaoPlus.onSyncEnd(worksiteId)
        val syncEndMessage = if (isSyncClosed) "Worksite fully synced." else "Unsynced data exists."
        syncLogger.log(syncEndMessage)

        // TODO Query and apply latest worksite cleanly or if local modifications exist
        //        merge change by filling and adding where not conflicting

        return isSyncClosed
    }

    // TODO Complete test coverage
    private suspend fun syncWorksiteChanges(sortedChanges: List<SavedWorksiteChange>) {
        if (sortedChanges.isEmpty()) {
            return
        }

        var startingSyncIndex = sortedChanges.size
        while (startingSyncIndex > 0) {
            if (sortedChanges[startingSyncIndex - 1].isArchived) {
                break
            }
            startingSyncIndex--
        }

        var oldestReferenceChangeIndex = max(0, startingSyncIndex - 1)
        while (oldestReferenceChangeIndex > 0) {
            if (sortedChanges[oldestReferenceChangeIndex].isSynced) {
                break
            }
            oldestReferenceChangeIndex--
        }
        val oldestReferenceChange = sortedChanges[oldestReferenceChangeIndex]

        val hasSnapshotChanges = startingSyncIndex < sortedChanges.size
        val newestChange = sortedChanges.last()
        if (hasSnapshotChanges || !newestChange.isArchived) {
            val syncChanges =
                if (hasSnapshotChanges) sortedChanges.subList(startingSyncIndex, sortedChanges.size)
                else listOf(newestChange)
            val hasPriorUnsyncedChanges = startingSyncIndex > oldestReferenceChangeIndex + 1
            val worksiteId = newestChange.worksiteId
            val worksiteNetworkId = worksiteDao.getWorksiteNetworkId(worksiteId)
            val flagIdLookup = worksiteFlagDao.getNetworkedIdMap(worksiteId)
                .associate { it.id to it.networkId }
            val noteIdLookup = worksiteNoteDao.getNetworkedIdMap(worksiteId)
                .associate { it.id to it.networkId }
            val workTypeIdLookup = workTypeDao.getNetworkedIdMap(worksiteId)
                .associate { it.id to it.networkId }
            val syncResult = worksiteChangeSyncer.sync(
                accountDataRepository.accountData.first(),
                oldestReferenceChange,
                syncChanges,
                hasPriorUnsyncedChanges,
                worksiteNetworkId,
                flagIdLookup = flagIdLookup,
                noteIdLookup = noteIdLookup,
                workTypeIdLookup = workTypeIdLookup,
                syncLogger,
            )

            appEnv.runInNonProd {
                syncLogger.log(
                    "Applying sync change results",
                    syncResult.getSummary(sortedChanges.size),
                )
            }

            worksiteChangeDaoPlus.updateSyncIds(worksiteId, syncResult.changeIds)
            worksiteChangeDaoPlus.updateSyncChanges(
                worksiteId,
                syncResult.changeResults,
                MaxSyncTries,
            )
        } else {
            with(newestChange) {
                syncLogger.log("Not syncing. Worksite $worksiteId change is not syncable.")
                // TODO Not worth retrying at this point.
                //      How to handle gracefully?
                //      Wait for user modification, intervention, or prompt?
            }
        }
    }
}

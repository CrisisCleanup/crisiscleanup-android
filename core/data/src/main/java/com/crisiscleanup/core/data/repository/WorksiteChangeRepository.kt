package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.dao.LocalImageDao
import com.crisiscleanup.core.database.dao.LocalImageDaoPlus
import com.crisiscleanup.core.database.dao.WorkTypeDao
import com.crisiscleanup.core.database.dao.WorksiteChangeDao
import com.crisiscleanup.core.database.dao.WorksiteChangeDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteFlagDao
import com.crisiscleanup.core.database.dao.WorksiteNoteDao
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asLookup
import com.crisiscleanup.core.model.data.SavedWorksiteChange
import com.crisiscleanup.core.model.data.WorkType
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.ExpiredTokenException
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.worksitechange.NoInternetConnectionException
import com.crisiscleanup.core.network.worksitechange.WorksiteChangeSyncer
import com.crisiscleanup.core.network.worksitechange.WorksitePhotoChangeSyncer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface WorksiteChangeRepository {
    val syncingWorksiteIds: Flow<Set<Long>>

    val streamWorksitesPendingSync: Flow<List<Worksite>>

    suspend fun saveWorksiteChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        primaryWorkType: WorkType,
        organizationId: Long,
    ): Long

    suspend fun saveWorkTypeTransfer(
        worksite: Worksite,
        organizationId: Long,
        requestReason: String = "",
        requests: List<String> = emptyList(),
        releaseReason: String = "",
        releases: List<String> = emptyList(),
    ): Boolean

    /**
     * @return TRUE if sync was attempted or FALSE otherwise
     */
    suspend fun syncWorksites(syncWorksiteCount: Int = 0): Boolean

    suspend fun saveDeletePhoto(fileId: Long): Long

    suspend fun trySyncWorksite(worksiteId: Long): Boolean
    suspend fun syncUnattemptedWorksite(worksiteId: Long)

    suspend fun syncWorksiteMedia(): Boolean
}

private const val MAX_SYNC_TRIES = 3

@Singleton
class CrisisCleanupWorksiteChangeRepository @Inject constructor(
    private val worksiteDao: WorksiteDao,
    private val worksiteDaoPlus: WorksiteDaoPlus,
    private val worksiteChangeDao: WorksiteChangeDao,
    private val worksiteChangeDaoPlus: WorksiteChangeDaoPlus,
    private val worksiteFlagDao: WorksiteFlagDao,
    private val worksiteNoteDao: WorksiteNoteDao,
    private val workTypeDao: WorkTypeDao,
    private val localImageDao: LocalImageDao,
    private val localImageDaoPlus: LocalImageDaoPlus,
    private val worksiteChangeSyncer: WorksiteChangeSyncer,
    private val worksitePhotoChangeSyncer: WorksitePhotoChangeSyncer,
    private val accountDataRepository: AccountDataRepository,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val worksitesRepository: WorksitesRepository,
    private val organizationsRepository: OrganizationsRepository,
    private val localImageRepository: LocalImageRepository,
    private val networkMonitor: NetworkMonitor,
    private val appEnv: AppEnv,
    private val syncLogger: SyncLogger,
    @Logger(CrisisCleanupLoggers.App) private val appLogger: AppLogger,
) : WorksiteChangeRepository {
    private val _syncingWorksiteIds = mutableSetOf<Long>()
    override val syncingWorksiteIds = MutableStateFlow(emptySet<Long>())

    private val syncWorksiteMutex = Mutex()

    override val streamWorksitesPendingSync = worksiteChangeDao.streamWorksitesPendingSync()
        .map { it.map(PopulatedWorksite::asExternalModel) }

    override suspend fun saveWorksiteChange(
        worksiteStart: Worksite,
        worksiteChange: Worksite,
        primaryWorkType: WorkType,
        organizationId: Long,
    ) = worksiteChangeDaoPlus.saveChange(
        worksiteStart,
        worksiteChange,
        primaryWorkType,
        organizationId,
    )

    override suspend fun saveWorkTypeTransfer(
        worksite: Worksite,
        organizationId: Long,
        requestReason: String,
        requests: List<String>,
        releaseReason: String,
        releases: List<String>,
    ): Boolean {
        if (organizationId <= 0) {
            return false
        }

        if (requestReason.isNotBlank() && requests.isNotEmpty()) {
            worksiteChangeDaoPlus.saveWorkTypeRequests(
                worksite,
                organizationId,
                requestReason,
                requests,
            )
            return true
        } else if (releaseReason.isNotBlank() && releases.isNotEmpty()) {
            worksiteChangeDaoPlus.saveWorkTypeReleases(
                worksite,
                organizationId,
                releaseReason,
                releases,
            )
            return true
        }

        return false
    }

    override suspend fun saveDeletePhoto(fileId: Long): Long {
        return worksiteChangeDaoPlus.saveDeletePhoto(fileId)
    }

    override suspend fun syncWorksites(syncWorksiteCount: Int): Boolean = coroutineScope {
        if (syncWorksiteMutex.tryLock()) {
            var worksiteId: Long
            try {
                var previousWorksiteId = 0L
                var syncCounter = 0
                val syncCountLimit = if (syncWorksiteCount < 1) 20 else syncWorksiteCount
                while (syncCounter++ < syncCountLimit) {
                    var worksiteIds = worksiteDao.getLocallyModifiedWorksites(1)
                    if (worksiteIds.isEmpty()) {
                        worksiteIds = worksiteChangeDao.getWorksitesPendingSync(1)
                        if (worksiteIds.isEmpty()) {
                            break
                        }
                    }
                    worksiteId = worksiteIds.first()

                    if (worksiteId == previousWorksiteId &&
                        worksiteChangeDao.getSaveFailCount(worksiteId) > 0
                    ) {
                        break
                    }
                    previousWorksiteId = worksiteId

                    trySyncWorksite(worksiteId, true)

                    ensureActive()
                }
            } catch (e: Exception) {
                // TODO Indicate error with notification
            } finally {
                syncWorksiteMutex.unlock()
            }

            return@coroutineScope true
        }

        return@coroutineScope false
    }

    override suspend fun trySyncWorksite(worksiteId: Long) = trySyncWorksite(worksiteId, false)

    override suspend fun syncUnattemptedWorksite(worksiteId: Long) {
        if (worksiteChangeDao.getSaveFailCount(worksiteId) == 0) {
            trySyncWorksite(worksiteId, false)
        }
    }

    private suspend fun trySyncWorksite(
        worksiteId: Long,
        rethrowError: Boolean,
    ): Boolean {
        if (networkMonitor.isNotOnline.first()) {
            syncLogger.log("Not attempting. No internet connection.")
            return false
        }

        val accountData = accountDataRepository.accountData.first()
        if (!accountData.areTokensValid) {
            syncLogger.log("Not attempting. Invalid tokens.")
            return false
        }

        try {
            synchronized(_syncingWorksiteIds) {
                if (_syncingWorksiteIds.contains(worksiteId)) {
                    syncLogger.log("Not syncing. Currently being synced.")
                    return false
                }
                _syncingWorksiteIds.add(worksiteId)
                syncingWorksiteIds.value = _syncingWorksiteIds.toSet()
            }

            syncWorksite(worksiteId)
        } catch (e: Exception) {
            var unhandledException: Exception? = null
            when (e) {
                is NoInternetConnectionException -> {}
                else -> {
                    unhandledException = e
                }
            }
            unhandledException?.let { endException ->
                appLogger.logException(endException)

                if (rethrowError) {
                    throw endException
                } else {
                    // TODO Indicate error visually
                    syncLogger.log("Sync failed", endException.message ?: "")
                }
            }
        } finally {
            synchronized(_syncingWorksiteIds) {
                _syncingWorksiteIds.remove(worksiteId)
                syncingWorksiteIds.value = _syncingWorksiteIds.toSet()
            }

            syncLogger.flush()
        }
        return true
    }

    private suspend fun syncWorksite(worksiteId: Long) {
        syncLogger.type = "syncing-worksite-$worksiteId-${Clock.System.now().epochSeconds}"

        var syncException: Exception? = null

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
                return
            }

            syncLogger.log("Sync changes starting.")

            val worksiteChanges = sortedChanges.map { it.asExternalModel(MAX_SYNC_TRIES) }
            try {
                syncWorksiteChanges(worksiteChanges)
            } catch (e: Exception) {
                syncException = e
            }

            syncLogger.log("Sync changes over.")
        }

        syncPhotoChanges(worksiteId)

        // TODO There is a possibility all changes have been synced but there is still unsynced accessory data.
        //      Try to sync in isolation, create a new change, or create notice with options to take action.

        // These fetches are split from the save later because WorksiteDaoPlus.onSyncEnd]
        // must run first as the [worksite_root.is_local_modified] value matters.
        var incidentId = 0L
        var syncNetworkWorksite: NetworkWorksiteFull? = null
        val networkWorksiteId = worksiteDao.getWorksiteNetworkId(worksiteId)
        if (networkWorksiteId > 0) {
            try {
                syncNetworkWorksite = networkDataSource.getWorksite(networkWorksiteId)
                incidentId = worksiteDao.getIncidentId(worksiteId)
            } catch (e: Exception) {
                syncLogger.log("Worksite sync end fail ${e.message}")
            }
        }

        val isFullySynced = worksiteDaoPlus.onSyncEnd(worksiteId, syncLogger)
        if (isFullySynced) {
            syncLogger.clear()
                .log("Worksite fully synced.")
        } else {
            syncLogger.log("Unsynced data exists.")
        }

        if (syncNetworkWorksite != null && incidentId > 0) {
            worksitesRepository.syncNetworkWorksite(syncNetworkWorksite)
        }

        syncException?.let { throw it }
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

        var oldestReferenceChangeIndex = (startingSyncIndex - 1).coerceAtLeast(0)
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
            val networkWorksiteId = worksiteDao.getWorksiteNetworkId(worksiteId)
            val flagIdLookup = worksiteFlagDao.getNetworkedIdMap(worksiteId).asLookup()
            val noteIdLookup = worksiteNoteDao.getNetworkedIdMap(worksiteId).asLookup()
            val workTypeIdLookup = workTypeDao.getNetworkedIdMap(worksiteId).asLookup()
            val organizationId = accountDataRepository.accountData.first().org.id
            val affiliateOrganizations =
                organizationsRepository.getOrganizationAffiliateIds(organizationId)
            val syncResult = worksiteChangeSyncer.sync(
                accountDataRepository.accountData.first(),
                oldestReferenceChange,
                syncChanges,
                hasPriorUnsyncedChanges,
                networkWorksiteId,
                flagIdLookup = flagIdLookup,
                noteIdLookup = noteIdLookup,
                workTypeIdLookup = workTypeIdLookup,
                affiliateOrganizations = affiliateOrganizations,
                syncLogger,
            )

            appEnv.runInNonProd {
                syncLogger.log(
                    "Sync change results",
                    syncResult.getSummary(sortedChanges.size),
                )
            }

            worksiteChangeDaoPlus.updateSyncIds(
                worksiteId,
                organizationId,
                syncResult.changeIds,
            )
            worksiteChangeDaoPlus.updateSyncChanges(
                worksiteId,
                syncResult.changeResults,
                MAX_SYNC_TRIES,
            )

            syncResult.changeResults.mapNotNull { it.exception }
                .forEach {
                    when (it) {
                        is NoInternetConnectionException,
                        is ExpiredTokenException,
                        -> throw it
                    }
                }
        } else {
            with(newestChange) {
                syncLogger.log("Not syncing. Worksite $worksiteId change is not syncable.")
                // TODO Not worth retrying at this point.
                //      How to handle gracefully?
                //      Wait for user modification, intervention, or prompt?
            }
        }
    }

    private suspend fun syncPhotoChanges(worksiteId: Long) {
        try {
            val (networkWorksiteId, deleteFileIds) =
                localImageDaoPlus.getDeletedPhotoNetworkFileIds(worksiteId)
            if (deleteFileIds.isNotEmpty()) {
                worksitePhotoChangeSyncer.deletePhotoFiles(networkWorksiteId, deleteFileIds)
                syncLogger.log("Deleted photos", deleteFileIds.joinToString(", "))
            }
        } catch (e: Exception) {
            syncLogger.log("Delete photo error", e.message ?: "")
        }
    }

    override suspend fun syncWorksiteMedia(): Boolean {
        val worksitesWithImages = localImageDao.getUploadImageWorksiteIds()
        var isSyncedAll = true
        for (worksiteImageUpload in worksitesWithImages) {
            val worksiteId = worksiteImageUpload.id
            val syncCount = localImageRepository.syncWorksiteMedia(worksiteId)
            if (syncCount > 0) {
                syncWorksite(worksiteId)
            }
            val unsyncedCount = worksiteImageUpload.count - syncCount
            isSyncedAll = isSyncedAll && unsyncedCount == 0
        }
        return isSyncedAll
    }
}

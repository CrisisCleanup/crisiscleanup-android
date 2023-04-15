package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.WorksiteSyncResult
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.ExpiredTokenException
import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Instant

class WorksiteChangeProcessor(
    private val changeSetOperator: WorksiteChangeSetOperator,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val writeApiClient: CrisisCleanupWriteApi,
    private val accountData: AccountData,
    private val networkMonitor: NetworkMonitor,
    private val appEnv: AppEnv,
    private val syncLogger: SyncLogger,
    private var hasPriorUnsyncedChanges: Boolean,
    private var worksiteNetworkId: Long,
    flagIdLookup: Map<Long, Long>,
    noteIdLookup: Map<Long, Long>,
    workTypeIdLookup: Map<Long, Long>,
) {
    private val flagIdMap = flagIdLookup.toMutableMap()
    private val noteIdMap = noteIdLookup.toMutableMap()
    private val workTypeIdMap = workTypeIdLookup.toMutableMap()

    private val syncChangeResults = mutableListOf<WorksiteSyncResult.ChangeResult>()

    private val networkWorksiteMutex = Mutex()
    private var _networkWorksite: NetworkWorksiteFull? = null
    private suspend fun getNetworkWorksite(force: Boolean = false): NetworkWorksiteFull {
        networkWorksiteMutex.withLock {
            if (force || _networkWorksite == null) {
                if (worksiteNetworkId <= 0) {
                    error("Attempted to query worksite when not yet created")
                }
                _networkWorksite = networkDataSource.getWorksite(worksiteNetworkId)
            }
        }
        _networkWorksite?.let { return it }
        ensureSyncConditions()
        throw WorksiteNotFoundException(worksiteNetworkId)
    }

    val syncResult: WorksiteSyncResult
        get() = WorksiteSyncResult(
            syncChangeResults,
            WorksiteSyncResult.ChangeIds(
                worksiteNetworkId,
                flagIdMap = flagIdMap,
                noteIdMap = noteIdMap,
                workTypeIdMap = workTypeIdMap,
                workTypeKeyMap = _networkWorksite?.workTypes
                    ?.associate { it.workType to it.id!! }
                    ?: emptyMap()
            ),
        )

    private suspend fun getChangeSet(changes: WorksiteChange) = if (changes.start == null) {
        changeSetOperator.getNewSet(changes.change)
    } else {
        changeSetOperator.getChangeSet(
            getNetworkWorksite(),
            changes.start,
            changes.change,
            flagIdMap,
            noteIdMap,
            workTypeIdMap,
        )
    }

    suspend fun process(
        startingReferenceChange: SyncWorksiteChange,
        sortedChanges: List<SyncWorksiteChange>,
    ) {
        var start: WorksiteSnapshot? = startingReferenceChange.worksiteChange.start

        for (syncChange in sortedChanges) {
            val changes = if (hasPriorUnsyncedChanges) WorksiteChange(
                start,
                syncChange.worksiteChange.change,
            )
            else syncChange.worksiteChange
            val changeSet = getChangeSet(changes)
            val isPartiallySynced = syncChange.isPartiallySynced && worksiteNetworkId > 0
            val syncResult = syncChangeSet(
                syncChange.createdAt,
                syncChange.syncUuid,
                isPartiallySynced,
                changeSet,
            )

            syncChangeResults.add(
                WorksiteSyncResult.ChangeResult(
                    syncChange.id,
                    isSuccessful = syncResult.isFullySynced,
                    isPartiallySuccessful = syncResult.isPartiallySynced,
                    isFail = syncResult.hasError,
                )
            )

            hasPriorUnsyncedChanges = !syncResult.isFullySynced
            if (syncResult.isFullySynced) {
                start = syncChange.worksiteChange.change
            }

            appEnv.runInNonProd {
                val syncExceptionSummary = syncResult.exceptionSummary
                if (syncExceptionSummary.isNotBlank()) {
                    syncLogger.log("Sync change exceptions.", syncExceptionSummary)
                }
            }

            if (!syncResult.canContinueSyncing) {
                break
            }

            if (syncResult.isPartiallySynced) {
                getNetworkWorksite(true)
            }
        }
    }

    private suspend fun syncChangeSet(
        changeCreatedAt: Instant,
        changeSyncUuid: String,
        isPartiallySynced: Boolean,
        changeSet: WorksiteChangeSet,
    ): SyncChangeSetResult {
        var result = SyncChangeSetResult(isPartiallySynced, isFullySynced = false)
        var worksite = _networkWorksite
        try {
            if (isPartiallySynced) {
                syncLogger.log("Partially synced. Skipping core.")
            } else {
                changeSet.worksite?.let {
                    worksite = writeApiClient.saveWorksite(changeCreatedAt, changeSyncUuid, it)
                    worksiteNetworkId = worksite!!.id
                    _networkWorksite = worksite

                    result = result.copy(isPartiallySynced = true)

                    syncLogger.log("Synced core $worksiteNetworkId.")
                }
            }

            changeSet.isOrgMember?.let { isOrgMember ->
                val favoriteId = worksite?.favorite?.id
                result = syncFavorite(isOrgMember, favoriteId, result)
            }

            result = syncFlags(changeSet.flagChanges, result)

            result = syncNotes(changeSet.extraNotes, result)

            result = syncWorkTypes(changeSet.workTypeChanges, result)

            result = result.copy(isFullySynced = true)

            if (changeSet.hasNonCoreChanges) {
                worksite = getNetworkWorksite(true)
                result = result.copy(worksite = worksite)
            }

        } catch (e: Exception) {
            result = when (e) {
                is NoInternetConnection -> result.copy(isConnectedToInternet = false)
                is ExpiredTokenException -> result.copy(isValidToken = false)
                else -> result.copy(exception = e)
            }
        }

        return result
    }

    private suspend fun syncFavorite(
        favorite: Boolean,
        favoriteId: Long?,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        try {
            if (favorite) {
                writeApiClient.favoriteWorksite(worksiteNetworkId)
            } else {
                if (favoriteId != null) {
                    writeApiClient.unfavoriteWorksite(worksiteNetworkId, favoriteId)
                }
            }

            syncLogger.log("Synced favorite.")
        } catch (e: Exception) {
            ensureSyncConditions()
            return baseResult.copy(favoriteException = e)
        }
        return baseResult
    }

    private suspend fun syncFlags(
        flagChanges: Pair<List<Pair<Long, NetworkFlag>>, Collection<Long>>,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        val addFlagExceptions = mutableMapOf<Long, Exception>()
        val (newFlags, deleteFlagIds) = flagChanges
        for ((localId, flag) in newFlags) {
            try {
                val syncedFlag = writeApiClient.addFlag(worksiteNetworkId, flag)
                flagIdMap[localId] = syncedFlag.id!!
                syncLogger.log("Synced flag $localId (${syncedFlag.id}).")

            } catch (e: Exception) {
                ensureSyncConditions()
                addFlagExceptions[localId] = e
            }
        }

        val deleteFlagExceptions = mutableMapOf<Long, Exception>()
        for (flagId in deleteFlagIds) {
            try {
                writeApiClient.deleteFlag(worksiteNetworkId, flagId)
                syncLogger.log("Synced delete flag $flagId.")
            } catch (e: Exception) {
                ensureSyncConditions()
                deleteFlagExceptions[flagId] = e
            }
        }
        return baseResult.copy(
            addFlagExceptions = addFlagExceptions,
            deleteFlagExceptions = deleteFlagExceptions,
        )
    }

    private suspend fun syncNotes(
        notes: List<Pair<Long, NetworkNote>>,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        val noteExceptions = mutableMapOf<Long, Exception>()
        for ((localId, note) in notes) {
            note.note?.let { noteContent ->
                try {
                    val syncedNote = writeApiClient.addNote(worksiteNetworkId, noteContent)
                    noteIdMap[localId] = syncedNote.id!!
                    syncLogger.log("Synced note $localId (${syncedNote.id}).")
                } catch (e: Exception) {
                    ensureSyncConditions()
                    noteExceptions[localId] = e
                }
            }
        }
        return baseResult.copy(noteExceptions = noteExceptions)
    }

    private suspend fun syncWorkTypes(
        workTypeChanges: List<WorkTypeChange>,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        val workTypeStatusExceptions = mutableMapOf<Long, Exception>()
        val claimWorkTypes = mutableSetOf<String>()
        val unclaimWorkTypes = mutableSetOf<String>()
        for (workTypeChange in workTypeChanges) {
            val localId = workTypeChange.localId
            val workType = workTypeChange.workType
            if (workTypeChange.isStatusChange) {
                try {
                    val syncedWorkType =
                        writeApiClient.updateWorkTypeStatus(workType.id, workType.status)
                    workTypeIdMap[localId] = syncedWorkType.id!!
                    syncLogger.log("Synced work type status $localId (${syncedWorkType.id}).")
                } catch (e: Exception) {
                    ensureSyncConditions()
                    workTypeStatusExceptions[localId] = e
                }
            } else if (workTypeChange.isClaimChange) {
                val isClaiming = workType.orgClaim == null
                if (isClaiming) {
                    claimWorkTypes.add(workType.workType)
                } else {
                    unclaimWorkTypes.add(workType.workType)
                }
            }
        }

        var workTypeClaimException: Exception? = null
        var workTypeUnclaimException: Exception? = null
        // Do not call to API with empty arrays as it may indicate all.
        if (claimWorkTypes.isNotEmpty()) {
            try {
                writeApiClient.claimWorkTypes(worksiteNetworkId, claimWorkTypes)
                syncLogger.log("Synced work type claim ${claimWorkTypes.joinToString(", ")}.")
            } catch (e: Exception) {
                ensureSyncConditions()
                workTypeClaimException = e
            }
        }
        // Do not call to API with empty arrays as it may indicate all.
        if (unclaimWorkTypes.isNotEmpty()) {
            try {
                writeApiClient.unclaimWorkTypes(worksiteNetworkId, unclaimWorkTypes)
                syncLogger.log("Synced work type unclaim ${unclaimWorkTypes.joinToString(", ")}.")
            } catch (e: Exception) {
                ensureSyncConditions()
                workTypeUnclaimException = e
            }
        }

        return baseResult.copy(
            hasClaimChange = claimWorkTypes.isNotEmpty() || unclaimWorkTypes.isNotEmpty(),
            workTypeStatusExceptions = workTypeStatusExceptions,
            workTypeClaimException = workTypeClaimException,
            workTypeUnclaimException = workTypeUnclaimException,
        )
    }

    private suspend fun ensureSyncConditions() {
        if (networkMonitor.isNotOnline.first()) {
            throw NoInternetConnection()
        }
        if (accountData.isTokenInvalid) {
            throw ExpiredTokenException()
        }
    }
}

class WorksiteNotFoundException(val worksiteNetworkId: Long) : Exception()
private class NoInternetConnection : Exception("No internet")

internal data class SyncChangeSetResult(
    val isPartiallySynced: Boolean,
    val isFullySynced: Boolean,

    val worksite: NetworkWorksiteFull? = null,
    val hasClaimChange: Boolean = false,

    val isConnectedToInternet: Boolean = true,
    val isValidToken: Boolean = true,

    val exception: Exception? = null,
    val favoriteException: Exception? = null,
    val addFlagExceptions: Map<Long, Exception> = emptyMap(),
    val deleteFlagExceptions: Map<Long, Exception> = emptyMap(),
    val noteExceptions: Map<Long, Exception> = emptyMap(),
    val workTypeStatusExceptions: Map<Long, Exception> = emptyMap(),
    val workTypeClaimException: Exception? = null,
    val workTypeUnclaimException: Exception? = null,
) {
    val hasError: Boolean
        get() = exception != null ||
                favoriteException != null ||
                addFlagExceptions.isNotEmpty() ||
                deleteFlagExceptions.isNotEmpty() ||
                noteExceptions.isNotEmpty() ||
                workTypeStatusExceptions.isNotEmpty() ||
                workTypeClaimException != null ||
                workTypeUnclaimException != null

    private fun summarizeExceptions(key: String, exceptions: Map<Long, Exception>): String {
        if (exceptions.isNotEmpty()) {
            val summary = exceptions
                .map { "  ${it.key}: ${it.value.message}" }
                .joinToString("\n")
            return "$key\n$summary"
        }
        return ""
    }

    val exceptionSummary: String
        get() = listOf(
            exception?.let { "Overall: ${it.message}" },
            favoriteException?.let { "Favorite: ${it.message}" },
            summarizeExceptions("Flags", addFlagExceptions),
            summarizeExceptions("Delete flags", deleteFlagExceptions),
            summarizeExceptions("Notes", noteExceptions),
            summarizeExceptions("Work type status", workTypeStatusExceptions),
            workTypeClaimException?.let { "Claim work types: ${it.message}" },
            workTypeUnclaimException?.let { "Unclaim work types: ${it.message}" },
        )
            .filter { it?.isNotBlank() == true }
            .joinToString("\n")

    val canContinueSyncing: Boolean
        get() = isConnectedToInternet && isValidToken
}
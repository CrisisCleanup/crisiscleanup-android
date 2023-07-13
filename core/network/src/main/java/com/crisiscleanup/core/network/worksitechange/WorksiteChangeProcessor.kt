package com.crisiscleanup.core.network.worksitechange

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.model.data.AccountData
import com.crisiscleanup.core.model.data.WorksiteSyncResult
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.CrisisCleanupNetworkException
import com.crisiscleanup.core.network.model.ExpiredTokenException
import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkWorkType
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
    private var networkWorksiteId: Long,
    flagIdLookup: Map<Long, Long>,
    noteIdLookup: Map<Long, Long>,
    workTypeIdLookup: Map<Long, Long>,
    private val affiliateOrganizations: Set<Long>,
) {
    private val flagIdMap = flagIdLookup.toMutableMap()
    private val noteIdMap = noteIdLookup.toMutableMap()
    private val workTypeIdMap = workTypeIdLookup.toMutableMap()
    private val workTypeRequestIdMap = mutableMapOf<String, Long>()

    private val syncChangeResults = mutableListOf<WorksiteSyncResult.ChangeResult>()

    private val networkWorksiteMutex = Mutex()
    private var _networkWorksite: NetworkWorksiteFull? = null
    private suspend fun getNetworkWorksite(force: Boolean = false): NetworkWorksiteFull {
        networkWorksiteMutex.withLock {
            if (force || _networkWorksite == null) {
                if (networkWorksiteId <= 0) {
                    error("Attempted to query worksite when not yet created")
                }
                _networkWorksite = networkDataSource.getWorksite(networkWorksiteId)
            }
        }
        _networkWorksite?.let { return it }
        ensureSyncConditions()
        throw WorksiteNotFoundException(networkWorksiteId)
    }

    private suspend fun updateWorkTypeIdMap(
        lookup: Map<String, Long>,
        forceQueryWorksite: Boolean,
    ) {
        val networkWorksite = getNetworkWorksite(forceQueryWorksite)
        networkWorksite.newestWorkTypes.forEach { networkWorkType ->
            lookup[networkWorkType.workType]?.let { localId ->
                workTypeIdMap[localId] = networkWorkType.id!!
            }
        }
    }

    val syncResult: WorksiteSyncResult
        get() = WorksiteSyncResult(
            syncChangeResults,
            WorksiteSyncResult.ChangeIds(
                networkWorksiteId,
                flagIdMap = flagIdMap,
                noteIdMap = noteIdMap,
                workTypeIdMap = workTypeIdMap,
                workTypeKeyMap = _networkWorksite?.newestWorkTypes
                    ?.associate { it.workType to it.id!! }
                    ?: emptyMap(),
                workTypeRequestIdMap = workTypeRequestIdMap,
            ),
        )

    suspend fun process(
        startingReferenceChange: SyncWorksiteChange,
        sortedChanges: List<SyncWorksiteChange>,
    ) {
        var start = startingReferenceChange.worksiteChange.start

        val lastLoopIndex = sortedChanges.size - 1
        sortedChanges.forEachIndexed { index, syncChange ->
            val changes =
                if (hasPriorUnsyncedChanges) syncChange.worksiteChange.copy(start = start)
                else syncChange.worksiteChange

            val syncResult = syncChangeDelta(syncChange, changes)
            val hasError = syncResult.hasError
            syncChangeResults.add(
                WorksiteSyncResult.ChangeResult(
                    syncChange.id,
                    isSuccessful = !hasError && syncResult.isFullySynced,
                    isPartiallySuccessful = syncResult.isPartiallySynced,
                    isFail = hasError,
                    exception = syncResult.primaryException,
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
                return@forEachIndexed
            }

            if ((syncResult.isFullySynced || syncResult.isPartiallySynced) &&
                syncResult.worksite == null &&
                index < lastLoopIndex
            ) {
                getNetworkWorksite(true)
            }
        }
    }

    private suspend fun syncChangeDelta(
        syncChange: SyncWorksiteChange,
        deltaChange: WorksiteChange,
    ) = if (deltaChange.isWorkTypeTransferChange) {
        syncWorkTypeTransfer(
            syncChange,
            deltaChange,
        )
    } else if (deltaChange.isWorksiteDataChange == true) {
        val isNewChange = deltaChange.start == null || networkWorksiteId <= 0
        val changeSet = if (isNewChange) {
            changeSetOperator.getNewSet(deltaChange.change)
        } else {
            changeSetOperator.getChangeSet(
                getNetworkWorksite(),
                deltaChange.start!!,
                deltaChange.change,
                flagIdMap,
                noteIdMap,
                workTypeIdMap,
            )
        }

        val isPartiallySynced = syncChange.isPartiallySynced && networkWorksiteId > 0
        syncChangeSet(
            syncChange.createdAt,
            // TODO New changes should use a constant sync ID even if previous changes are skipped
            syncChange.syncUuid,
            isPartiallySynced,
            changeSet,
        )
    } else {
        syncLogger.log("Skipping unsupported change", "Is photo? ${deltaChange.isPhotoChange}.")
        SyncChangeSetResult(
            isPartiallySynced = false,
            isFullySynced = false,
            exception = Exception("Unsupported sync change"),
        )
    }

    private suspend fun syncWorkTypeTransfer(
        syncChange: SyncWorksiteChange,
        deltaChange: WorksiteChange
    ): SyncChangeSetResult {
        var result = SyncChangeSetResult(false, isFullySynced = false)
        val changeCreatedAt = syncChange.createdAt
        if (deltaChange.requestWorkTypes?.hasValue == true) {
            result = syncRequestWorkTypes(
                changeCreatedAt,
                deltaChange.requestWorkTypes,
                result,
            )
        } else if (deltaChange.releaseWorkTypes?.hasValue == true) {
            result = syncReleaseWorkTypes(
                changeCreatedAt,
                deltaChange.change,
                deltaChange.releaseWorkTypes,
                result,
            )
        }

        return result.copy(isFullySynced = true)
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
                    networkWorksiteId = worksite!!.id
                    _networkWorksite = worksite

                    result = result.copy(isPartiallySynced = true)

                    syncLogger.log("Synced core $networkWorksiteId.")
                }
            }

            if ((_networkWorksite?.id ?: -1) <= 0) {
                throw WorksiteNotFoundException(networkWorksiteId)
            }

            changeSet.isOrgMember?.let { isOrgMember ->
                val favoriteId = worksite?.favorite?.id
                result = syncFavorite(changeCreatedAt, isOrgMember, favoriteId, result)
            }

            result = syncFlags(changeCreatedAt, changeSet.flagChanges, result)

            result = syncNotes(changeCreatedAt, changeSet.extraNotes, result)

            result = syncWorkTypes(changeCreatedAt, changeSet.workTypeChanges, result)

            result = result.copy(isFullySynced = true)

            if (changeSet.hasNonCoreChanges || result.hasClaimChange) {
                worksite = getNetworkWorksite(true)
                result = result.copy(worksite = worksite)
            }

            if (result.hasClaimChange) {
                val workTypeLocalIdLookup =
                    changeSet.workTypeChanges.associate { it.workType.workType to it.localId }
                updateWorkTypeIdMap(workTypeLocalIdLookup, false)
            }

        } catch (e: Exception) {
            result = when (e) {
                is NoInternetConnectionException -> result.copy(isConnectedToInternet = false)
                is ExpiredTokenException -> result.copy(isValidToken = false)
                else -> result.copy(exception = e)
            }
        }

        return result
    }

    private suspend fun syncFavorite(
        changeAt: Instant,
        favorite: Boolean,
        favoriteId: Long?,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        return try {
            if (favorite) {
                writeApiClient.favoriteWorksite(changeAt, networkWorksiteId)
            } else {
                if (favoriteId != null) {
                    writeApiClient.unfavoriteWorksite(changeAt, networkWorksiteId, favoriteId)
                }
            }

            syncLogger.log("Synced favorite.")

            baseResult
        } catch (e: Exception) {
            ensureSyncConditions()
            baseResult.copy(favoriteException = e)
        }
    }

    private suspend fun syncFlags(
        changeAt: Instant,
        flagChanges: Pair<List<Pair<Long, NetworkFlag>>, Collection<Long>>,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        val addFlagExceptions = mutableMapOf<Long, Exception>()
        val (newFlags, deleteFlagIds) = flagChanges
        for ((localId, flag) in newFlags) {
            try {
                val syncedFlag = writeApiClient.addFlag(changeAt, networkWorksiteId, flag)
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
                writeApiClient.deleteFlag(changeAt, networkWorksiteId, flagId)
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
        changeAt: Instant,
        notes: List<Pair<Long, NetworkNote>>,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        val noteExceptions = mutableMapOf<Long, Exception>()
        for ((localId, note) in notes) {
            note.note?.let { noteContent ->
                try {
                    val syncedNote =
                        writeApiClient.addNote(note.createdAt, networkWorksiteId, noteContent)
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
        changeAt: Instant,
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
                        writeApiClient.updateWorkTypeStatus(changeAt, workType.id, workType.status)
                    workTypeIdMap[localId] = syncedWorkType.id!!
                    syncLogger.log("Synced work type status $localId (${syncedWorkType.id}).")
                } catch (e: Exception) {
                    ensureSyncConditions()
                    workTypeStatusExceptions[localId] = e
                }
            } else if (workTypeChange.isClaimChange) {
                val isClaiming = workType.orgClaim != null
                if (isClaiming) {
                    claimWorkTypes.add(workType.workType)
                } else {
                    unclaimWorkTypes.add(workType.workType)
                }
            }
        }

        var hasClaimChange = false
        val workTypeOrgLookup =
            getNetworkWorksite().newestWorkTypes.associate { it.workType to it.orgClaim }

        var workTypeClaimException: Exception? = null
        val networkClaimWorkTypes = claimWorkTypes.filter { workTypeOrgLookup[it] == null }
        // Do not call to API with empty arrays as it may indicate all.
        if (networkClaimWorkTypes.isNotEmpty()) {
            try {
                writeApiClient.claimWorkTypes(changeAt, networkWorksiteId, networkClaimWorkTypes)
                hasClaimChange = true
                syncLogger.log("Synced work type claim ${networkClaimWorkTypes.joinToString(", ")}.")
            } catch (e: Exception) {
                ensureSyncConditions()
                workTypeClaimException = e
            }
        }


        var workTypeUnclaimException: Exception? = null
        val networkUnclaimWorkTypes = unclaimWorkTypes.filter {
            val claimOrgId = workTypeOrgLookup[it]
            claimOrgId != null && affiliateOrganizations.contains(claimOrgId)
        }
        // Do not call to API with empty arrays as it may indicate all.
        if (networkUnclaimWorkTypes.isNotEmpty()) {
            try {
                writeApiClient.unclaimWorkTypes(
                    changeAt,
                    networkWorksiteId,
                    networkUnclaimWorkTypes,
                )
                hasClaimChange = true
                syncLogger.log("Synced work type unclaim ${networkUnclaimWorkTypes.joinToString(", ")}.")
            } catch (e: Exception) {
                ensureSyncConditions()
                workTypeUnclaimException = e
            }
        }

        return baseResult.copy(
            hasClaimChange = hasClaimChange,
            workTypeStatusExceptions = workTypeStatusExceptions,
            workTypeClaimException = workTypeClaimException,
            workTypeUnclaimException = workTypeUnclaimException,
        )
    }

    private fun NetworkWorksiteFull.matchOtherOrgWorkTypes(transfer: WorkTypeTransfer): List<String> {
        val otherOrgClaimed = newestWorkTypes
            .filter {
                it.orgClaim != null && !affiliateOrganizations.contains(it.orgClaim)
            }
            .map(NetworkWorkType::workType)
            .toSet()
        return transfer.workTypes.filter { otherOrgClaimed.contains(it) }
    }

    private suspend fun syncRequestWorkTypes(
        changeAt: Instant,
        transferRequest: WorkTypeTransfer,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        val networkWorksite = getNetworkWorksite()
        val requestWorkTypes = networkWorksite.matchOtherOrgWorkTypes(transferRequest)
        return if (requestWorkTypes.isEmpty()) baseResult
        else try {
            writeApiClient.requestWorkTypes(
                changeAt,
                networkWorksite.id,
                requestWorkTypes,
                transferRequest.reason,
            )

            val workTypeRequests = networkDataSource.getWorkTypeRequests(networkWorksiteId)
            workTypeRequests.forEach {
                workTypeRequestIdMap[it.workType.workType] = it.id
            }

            baseResult
        } catch (e: Exception) {
            ensureSyncConditions()
            baseResult.copy(workTypeRequestException = e)
        }
    }

    private suspend fun syncReleaseWorkTypes(
        changeAt: Instant,
        worksite: WorksiteSnapshot,
        transferRelease: WorkTypeTransfer,
        baseResult: SyncChangeSetResult,
    ): SyncChangeSetResult {
        val networkWorksite = getNetworkWorksite()
        val releaseWorkTypes = networkWorksite.matchOtherOrgWorkTypes(transferRelease)
        return if (releaseWorkTypes.isEmpty()) baseResult
        else try {
            writeApiClient.releaseWorkTypes(
                changeAt,
                networkWorksite.id,
                releaseWorkTypes,
                transferRelease.reason,
            )

            val workTypeLocalIdLookup = worksite.workTypes.associate {
                it.workType.workType to it.localId
            }
            updateWorkTypeIdMap(workTypeLocalIdLookup, true)

            baseResult
        } catch (e: Exception) {
            // TODO Failure likely introduces inconsistencies on downstream changes and local state.
            //      How to manage properly and completely?
            //      Local state at this point likely has unclaimed work types.
            //      Further operations may introduce and propagate additional inconsistencies.
            ensureSyncConditions()
            baseResult.copy(workTypeReleaseException = e)
        }
    }

    private suspend fun ensureSyncConditions() {
        if (networkMonitor.isNotOnline.first()) {
            throw NoInternetConnectionException()
        }
        if (!accountData.areTokensValid) {
            throw ExpiredTokenException()
        }
    }
}

class WorksiteNotFoundException(networkWorksiteId: Long) :
    Exception("Worksite $networkWorksiteId not found/created")

class NoInternetConnectionException : Exception("No internet")

internal data class SyncChangeSetResult(
    /**
     * Indicates syncing of core worksite data was successful
     *
     * Is not indicative of non-core data in any way.
     */
    val isPartiallySynced: Boolean,
    /**
     * Indicates syncing ended without aborting (with or without error)
     *
     * Abort occurs when
     * - Internet connection is lost
     * - Token becomes invalid
     *
     * Errors can be ascertained through [hasError] and exceptions.
     */
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
    val workTypeRequestException: Exception? = null,
    val workTypeReleaseException: Exception? = null,
) {
    private val dataException: Exception?
        get() {
            return exception
                ?: favoriteException
                ?: addFlagExceptions.values.firstOrNull()
                ?: deleteFlagExceptions.values.firstOrNull()
                ?: noteExceptions.values.firstOrNull()
                ?: workTypeStatusExceptions.values.firstOrNull()
                ?: workTypeClaimException
                ?: workTypeUnclaimException
                ?: workTypeRequestException
                ?: workTypeReleaseException
        }

    val primaryException: Exception?
        get() {
            return if (!isConnectedToInternet) NoInternetConnectionException()
            else if (!isValidToken) ExpiredTokenException()
            else dataException
        }

    val hasError: Boolean
        get() = dataException != null

    private val Exception.errorMessage: String
        get() = (this as? CrisisCleanupNetworkException)
            ?.errors?.firstOrNull()
            ?.message?.joinToString(", ")
            ?: message ?: ""

    private fun summarizeExceptions(key: String, exceptions: Map<Long, Exception>): String {
        if (exceptions.isNotEmpty()) {
            val summary = exceptions
                .map { "  ${it.key}: ${it.value.errorMessage}" }
                .joinToString("\n")
            return "$key\n$summary"
        }
        return ""
    }

    val exceptionSummary: String
        get() = listOf(
            exception?.let { "Overall: ${it.errorMessage}" },
            favoriteException?.let { "Favorite: ${it.errorMessage}" },
            summarizeExceptions("Flags", addFlagExceptions),
            summarizeExceptions("Delete flags", deleteFlagExceptions),
            summarizeExceptions("Notes", noteExceptions),
            summarizeExceptions("Work type status", workTypeStatusExceptions),
            workTypeClaimException?.let { "Claim work types: ${it.errorMessage}" },
            workTypeUnclaimException?.let { "Unclaim work types: ${it.errorMessage}" },
            workTypeRequestException?.let { "Request work types: ${it.errorMessage}" },
            workTypeReleaseException?.let { "Release work types: ${it.errorMessage}" },
        )
            .filter { it?.isNotBlank() == true }
            .joinToString("\n")

    val canContinueSyncing: Boolean
        get() = isConnectedToInternet && isValidToken
}
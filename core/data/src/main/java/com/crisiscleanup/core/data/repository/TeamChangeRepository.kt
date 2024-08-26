package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.AppEnv
import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.dao.TeamDao
import com.crisiscleanup.core.database.dao.TeamDaoPlus
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkTeam
import com.crisiscleanup.core.network.worksitechange.NoInternetConnectionException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

interface TeamChangeRepository {
    val syncingTeamIds: Flow<Set<Long>>

//    suspend fun saveTeamChange(
//        teamStart: CleanupTeam,
//        teamChange: CleanupTeam,
//        organizationId: Long,
//    ): Long

    /**
     * @return TRUE if sync was attempted or FALSE otherwise
     */
//    suspend fun syncTeams(syncTeamCount: Int = 0): Boolean

    suspend fun trySyncTeam(teamId: Long): Boolean
//    suspend fun syncUnattemptedTeam(teamId: Long)
}

@Singleton
class CrisisCleanupTeamChangeRepository @Inject constructor(
    private val teamDao: TeamDao,
    private val teamDaoPlus: TeamDaoPlus,
//    private val teamChangeDao: TeamChangeDao,
    private val teamsRepository: TeamsRepository,
    private val accountDataRepository: AccountDataRepository,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val networkMonitor: NetworkMonitor,
    appEnv: AppEnv,
    private val syncLogger: SyncLogger,
    @Logger(CrisisCleanupLoggers.Sync) private val appLogger: AppLogger,
) : TeamChangeRepository {
    private val _syncingTeamIds = mutableSetOf<Long>()
    override val syncingTeamIds = MutableStateFlow(emptySet<Long>())

    private val syncTeamMutex = Mutex()

    override suspend fun trySyncTeam(teamId: Long) = trySyncTeam(teamId, false)

    private suspend fun trySyncTeam(
        teamId: Long,
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
            synchronized(_syncingTeamIds) {
                if (_syncingTeamIds.contains(teamId)) {
                    syncLogger.log("Team $teamId sync in progress.")
                    return false
                }
                _syncingTeamIds.add(teamId)
                syncingTeamIds.value = _syncingTeamIds.toSet()
            }

            syncTeam(teamId)
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
            synchronized(_syncingTeamIds) {
                _syncingTeamIds.remove(teamId)
                syncingTeamIds.value = _syncingTeamIds.toSet()
            }

            syncLogger.flush()
        }
        return true
    }

    private suspend fun syncTeam(teamId: Long) {
        val syncStart = Clock.System.now()
        syncLogger.type = "syncing-team-$teamId-${syncStart.epochSeconds}"

//        var syncException: Exception? = null

//        val sortedChanges = teamChangeDao.getOrdered(teamId)
//        if (sortedChanges.isNotEmpty()) {
//            syncLogger.log("${sortedChanges.size} changes.")
//
//            val newestChange = sortedChanges.last()
//            val newestChangeOrgId = newestChange.entity.organizationId
//            val accountData = accountDataRepository.accountData.first()
//            val organizationId = accountData.org.id
//            if (newestChangeOrgId != organizationId) {
//                syncLogger.log("Not syncing. Org mismatch $organizationId != $newestChangeOrgId.")
//                // TODO Insert notice that newest change of team was with a different organization
//                return
//            }
//
//            syncLogger.log("Sync changes starting.")
//
//            val teamChanges = sortedChanges.map { it.asExternalModel(MAX_SYNC_TRIES) }
//            try {
//                syncTeamChanges(teamChanges)
//            } catch (e: Exception) {
//                syncException = e
//            }
//
//            syncLogger.log("Sync changes over.")
//        }

        // These fetches are split from the save(s) below because *.onSyncEnd
        // must run first as the [*_root.is_local_modified] value matters.
        var incidentId = 0L
        val networkPullAt = Clock.System.now()
        var syncNetworkTeam: NetworkTeam? = null
        val networkTeamId = teamDao.getTeamNetworkId(teamId)
        if (networkTeamId > 0) {
            try {
                syncNetworkTeam = networkDataSource.getTeam(networkTeamId)
                incidentId = teamDao.getIncidentId(teamId)
            } catch (e: Exception) {
                syncLogger.log("Team sync end fail ${e.message}")
            }
        }

//        val isFullySynced = teamDaoPlus.onSyncEnd(
//            teamId,
//            MAX_SYNC_TRIES,
//            syncLogger,
//            syncStart,
//        )
//        if (isFullySynced) {
//            syncLogger.clear()
//                .log("Team fully synced.")
//        } else {
//            val teamName = syncNetworkTeam?.name ?: "?"
//            syncLogger.log("Unsynced data exists for $networkTeamId $teamName.")
//        }

        if (syncNetworkTeam != null && incidentId > 0) {
            teamsRepository.syncNetworkTeam(syncNetworkTeam, networkPullAt)
        }

//        syncException?.let { throw it }
    }
}

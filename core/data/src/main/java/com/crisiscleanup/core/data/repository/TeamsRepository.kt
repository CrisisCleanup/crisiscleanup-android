package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.split
import com.crisiscleanup.core.database.dao.TeamDao
import com.crisiscleanup.core.database.dao.TeamDaoPlus
import com.crisiscleanup.core.database.model.PopulatedTeam
import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.closedWorkTypeStatuses
import com.crisiscleanup.core.model.data.statusFromLiteral
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkTeam
import com.crisiscleanup.core.network.model.tryThrowException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

interface TeamsRepository {
    suspend fun streamIncidentTeams(incidentId: Long): Flow<IncidentTeams>

    suspend fun syncTeams(incidentId: Long)
}

class CrisisCleanupTeamsRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val teamDao: TeamDao,
    private val teamDaoPlus: TeamDaoPlus,
    private val accountDataRepository: AccountDataRepository,
    @Logger(CrisisCleanupLoggers.Team) private val logger: AppLogger,
) : TeamsRepository {

    override suspend fun streamIncidentTeams(incidentId: Long): Flow<IncidentTeams> {
        val accountId = accountDataRepository.accountData.first().id
        return teamDao.streamTeams(incidentId)
            .mapLatest {
                val (myTeams, otherTeams) = it.map(PopulatedTeam::asExternalModel)
                    .split { team ->
                        val accountIds = team.members.map { user -> user.id }.toSet()
                        accountIds.contains(accountId)
                    }
                IncidentTeams(myTeams.toList(), otherTeams.toList())
            }
    }

    private suspend fun syncTeams(
        incidentId: Long,
        networkTeams: List<NetworkTeam>,
        syncedAt: Instant,
    ) {
        val teamEntities = networkTeams.map { networkTeam ->
            val workTypes = networkTeam.assignedWork ?: emptyList()
            val workTypeStatuses = workTypes.map { statusFromLiteral(it.status) }
            val completeCount = workTypeStatuses.filter { closedWorkTypeStatuses.contains(it) }.size
            TeamEntity(
                id = 0,
                networkId = networkTeam.id,
                incidentId = incidentId,
                name = networkTeam.name,
                notes = networkTeam.notes ?: "",
                color = "",
                caseCount = networkTeam.assignedWork?.size ?: 0,
                completeCount = completeCount,
            )
        }
        val teamMemberLookup = mutableMapOf<Long, List<Long>>()
        networkTeams.forEach { networkTeam ->
            teamMemberLookup[networkTeam.id] = networkTeam.users ?: emptyList()
        }
        teamDaoPlus.syncTeams(teamEntities, teamMemberLookup, syncedAt)
    }

    override suspend fun syncTeams(incidentId: Long) {
        val queryLimit = 1000
        val teamLimit = 100
        var teamOffset = 0
        val syncedAt = Clock.System.now()
        try {
            while (teamOffset < queryLimit) {
                val teamsResult = networkDataSource.getTeams(incidentId, teamLimit, teamOffset)
                teamsResult.errors?.tryThrowException()

                if ((teamsResult.results?.size ?: 0) == 0) {
                    break
                }

                val teams = teamsResult.results!!
                syncTeams(incidentId, teams, syncedAt)

                teamOffset += teams.size
                if (teamOffset >= queryLimit.coerceAtMost(teamsResult.count ?: 0)) {
                    break
                }
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }
}

data class IncidentTeams(
    val myTeams: List<CleanupTeam>,
    val otherTeams: List<CleanupTeam>,
)

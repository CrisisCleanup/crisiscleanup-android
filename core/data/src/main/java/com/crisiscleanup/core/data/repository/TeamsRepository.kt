package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.split
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.TeamDao
import com.crisiscleanup.core.database.dao.TeamDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDao
import com.crisiscleanup.core.database.model.PopulatedTeam
import com.crisiscleanup.core.database.model.PopulatedTeamMemberEquipment
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.LocalTeam
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkTeam
import com.crisiscleanup.core.network.model.NetworkUserEquipment
import com.crisiscleanup.core.network.model.NetworkWorkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import javax.inject.Inject

interface TeamsRepository {
    suspend fun streamIncidentTeams(incidentId: Long): Flow<IncidentTeams>

    fun streamLocalTeam(teamId: Long): Flow<LocalTeam?>

    suspend fun syncNetworkTeam(
        team: NetworkTeam,
        syncedAt: Instant = Clock.System.now(),
    ): Boolean

    suspend fun syncTeams(incidentId: Long)
}

class CrisisCleanupTeamsRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val worksiteDao: WorksiteDao,
    private val teamDao: TeamDao,
    private val teamDaoPlus: TeamDaoPlus,
    private val accountDataRepository: AccountDataRepository,
    private val worksitesRepository: WorksitesRepository,
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

    override fun streamLocalTeam(teamId: Long): Flow<LocalTeam?> = combine(
        teamDao.streamLocalTeam(teamId),
        teamDao.streamTeamMemberEquipment(teamId),
        ::Pair,
    )
        .mapLatest { (team, teamMemberEquipment) ->
            val memberEquipment =
                teamMemberEquipment.map(PopulatedTeamMemberEquipment::asExternalModel)
                    .sortedWith { a, b ->
                        val nameCompare = a.userName.compareTo(b.userName, true)
                        if (nameCompare == 0) {
                            val equipmentCompare =
                                a.equipmentData.equipment.compareTo(b.equipmentData.equipment)
                            return@sortedWith equipmentCompare
                        }
                        nameCompare
                    }
            team?.asExternalModel(memberEquipment)
        }

    private suspend fun syncTeams(
        networkTeams: List<NetworkTeam>,
        syncedAt: Instant,
    ) {
        val teamEntities = networkTeams.map(NetworkTeam::asEntity)
        val teamMemberLookup = mutableMapOf<Long, List<Long>>()
        val teamEquipmentLookup = mutableMapOf<Long, Set<Long>>()
        val memberEquipmentLookup = mutableMapOf<Long, MutableSet<Long>>()
        networkTeams.forEach { networkTeam ->
            teamMemberLookup[networkTeam.id] = networkTeam.users ?: emptyList()

            teamEquipmentLookup[networkTeam.id] =
                networkTeam.userEquipment.flatMap(NetworkUserEquipment::equipmentIds).toSet()
            networkTeam.userEquipment.forEach { userEquipment ->
                if (!memberEquipmentLookup.contains(userEquipment.userId)) {
                    memberEquipmentLookup[userEquipment.userId] = mutableSetOf()
                }
                memberEquipmentLookup[userEquipment.userId]!!.addAll(userEquipment.equipmentIds)
            }
        }
        teamDaoPlus.syncTeams(
            teamEntities,
            teamMemberLookup,
            teamEquipmentLookup,
            memberEquipmentLookup,
            syncedAt,
        )
    }

    override suspend fun syncTeams(incidentId: Long) {
        val queryLimit = 1000
        val teamLimit = 100
        var teamOffset = 0
        val syncedAt = Clock.System.now()
        try {
            while (teamOffset < queryLimit) {
                val teamsResult = networkDataSource.getTeams(incidentId, teamLimit, teamOffset)

                if ((teamsResult.results?.size ?: 0) == 0) {
                    break
                }

                val teams = teamsResult.results!!
                syncTeams(teams, syncedAt)

                teamOffset += teams.size
                if (teamOffset >= queryLimit.coerceAtMost(teamsResult.count ?: 0)) {
                    break
                }
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun syncNetworkTeam(
        team: NetworkTeam,
        syncedAt: Instant,
    ): Boolean {
        val teamMembers = team.users ?: emptyList()
        val teamEquipment = team.userEquipment.flatMap(NetworkUserEquipment::equipmentIds).toSet()
        val memberEquipmentLookup = team.userEquipment.associate { userEquipment ->
            userEquipment.userId to userEquipment.equipmentIds
        }

        team.assignedWork?.let { pullMissingWorksites(it) }

        return teamDaoPlus.syncTeam(
            team.asEntity(),
            teamMembers,
            teamEquipment,
            memberEquipmentLookup,
            syncedAt,
        )
    }

    private suspend fun pullMissingWorksites(workTypes: List<NetworkWorkType>) {
        try {
            val workTypeNetworkIds = workTypes.mapNotNull { it.id }
            val idLookup = worksiteDao.getWorkTypeWorksites(workTypeNetworkIds)
                .associate { it.networkId to it.worksiteId }
            val missingWorkTypeIds = workTypeNetworkIds.filter { !idLookup.contains(it) }
            val worksiteIds = networkDataSource.getWorkTypeWorksiteLookup(missingWorkTypeIds)
            for (entry in worksiteIds) {
                val worksiteId = entry.value
                worksitesRepository.syncNetworkWorksite(worksiteId)
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

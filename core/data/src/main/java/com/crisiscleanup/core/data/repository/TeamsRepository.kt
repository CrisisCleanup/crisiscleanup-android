package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.split
import com.crisiscleanup.core.data.model.asEntity
import com.crisiscleanup.core.database.dao.TeamDao
import com.crisiscleanup.core.database.dao.TeamDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteWorkTypeIds
import com.crisiscleanup.core.database.dao.fts.streamMatchingTeams
import com.crisiscleanup.core.database.model.PopulatedTeam
import com.crisiscleanup.core.database.model.PopulatedTeamMemberEquipment
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.CleanupTeam
import com.crisiscleanup.core.model.data.ExistingUserCodeInviteAccept
import com.crisiscleanup.core.model.data.JoinOrgResult
import com.crisiscleanup.core.model.data.LocalTeam
import com.crisiscleanup.core.model.data.OrgUserInviteInfo
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupRegisterApi
import com.crisiscleanup.core.network.model.NetworkTeam
import com.crisiscleanup.core.network.model.NetworkTeamWork
import com.crisiscleanup.core.network.model.NetworkUserEquipment
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

    suspend fun streamMatchingOtherTeams(q: String, incidentId: Long): Flow<List<CleanupTeam>>

    suspend fun acceptPersistentInvitation(invite: ExistingUserCodeInviteAccept): JoinOrgResult
    suspend fun getInvitationInfo(invite: UserPersistentInvite): OrgUserInviteInfo?
}

class CrisisCleanupTeamsRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val teamDao: TeamDao,
    private val teamDaoPlus: TeamDaoPlus,
    private val accountDataRepository: AccountDataRepository,
    private val worksitesRepository: WorksitesRepository,
    private val registerApi: CrisisCleanupRegisterApi,
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
        teamDao.streamTeamWorksites(teamId),
        ::Triple,
    )
        .mapLatest { (team, teamMemberEquipment, teamWorksites) ->
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
            val worksites = teamWorksites.map(PopulatedWorksite::asExternalModel)
            val workIdLookup = worksites
                .flatMap { worksite ->
                    worksite.workTypes.map {
                        Pair(it.id, worksite.id)
                    }
                }
                .associate { it.first to it.second }
            team?.asExternalModel(memberEquipment, worksites, workIdLookup)
        }

    private suspend fun syncTeams(
        networkTeams: List<NetworkTeam>,
        syncedAt: Instant,
    ) {
        val teamEntities = networkTeams.map(NetworkTeam::asEntity)
        val teamMemberLookup = mutableMapOf<Long, List<Long>>()
        val teamEquipmentLookup = mutableMapOf<Long, Set<Long>>()
        val teamWorkLookup = mutableMapOf<Long, List<WorksiteWorkTypeIds>>()
        val memberEquipmentLookup = mutableMapOf<Long, MutableSet<Long>>()
        networkTeams.forEach { networkTeam ->
            val teamId = networkTeam.id
            teamMemberLookup[teamId] = networkTeam.users ?: emptyList()

            teamEquipmentLookup[teamId] =
                networkTeam.userEquipment.flatMap(NetworkUserEquipment::equipmentIds).toSet()

            teamWorkLookup[teamId] = networkTeam.assignedWork?.mapWorkIds() ?: emptyList()

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
            teamWorkLookup,
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
        val teamWork = team.assignedWork?.mapWorkIds() ?: emptyList()
        val memberEquipmentLookup = team.userEquipment.associate { userEquipment ->
            userEquipment.userId to userEquipment.equipmentIds
        }

        team.assignedWork?.let { pullWorksites(it) }

        return teamDaoPlus.syncTeam(
            team.asEntity(),
            teamMembers,
            teamEquipment,
            teamWork,
            memberEquipmentLookup,
            syncedAt,
        )
    }

    private suspend fun pullWorksites(
        workTypes: List<NetworkTeamWork>,
    ) {
        try {
            val worksiteIds = workTypes.map(NetworkTeamWork::worksite).toSet()
            worksitesRepository.syncNetworkWorksites(worksiteIds)
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun streamMatchingOtherTeams(
        q: String,
        incidentId: Long,
    ): Flow<List<CleanupTeam>> {
        val accountId = accountDataRepository.accountData.first().id
        return teamDaoPlus.streamMatchingTeams(q, incidentId)
            .mapLatest { teams ->
                teams.filter {
                    !it.memberIds.contains(accountId)
                }
            }
    }

    override suspend fun acceptPersistentInvitation(invite: ExistingUserCodeInviteAccept) =
        registerApi.acceptPersistentInvitation(invite)

    override suspend fun getInvitationInfo(invite: UserPersistentInvite) =
        registerApi.getInvitationInfo(invite)
}

data class IncidentTeams(
    val myTeams: List<CleanupTeam>,
    val otherTeams: List<CleanupTeam>,
)

fun List<NetworkTeamWork>.mapWorkIds() = map {
    WorksiteWorkTypeIds(
        worksiteId = it.worksite,
        workTypeNetworkId = it.id,
    )
}

package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.common.UuidGenerator
import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.common.sync.SyncLogger
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonEquipmentCrossRef
import com.crisiscleanup.core.database.model.PopulatedLocalModifiedAt
import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.database.model.TeamEquipmentCrossRef
import com.crisiscleanup.core.database.model.TeamMemberCrossRef
import kotlinx.datetime.Instant
import javax.inject.Inject

class TeamDaoPlus @Inject constructor(
    internal val db: CrisisCleanupDatabase,
    private val uuidGenerator: UuidGenerator,
    private val syncLogger: SyncLogger,
    @Logger(CrisisCleanupLoggers.Team) private val appLogger: AppLogger,
) {
    private suspend fun getTeamLocalModifiedAt(networkIds: Set<Long>) = db.withTransaction {
        val teamsModifiedAt = db.teamDao().getTeamsLocalModifiedAt(networkIds)
        teamsModifiedAt.associateBy(PopulatedLocalModifiedAt::networkId)
    }

    private suspend fun syncMemberEquipment(
        memberEquipment: Map<Long, Set<Long>>,
    ) = db.withTransaction {
        val personDao = db.personContactDao()
        for ((userId, userEquipment) in memberEquipment) {
            personDao.deleteUnspecifiedEquipment(userId, userEquipment)

            val personEquipments = userEquipment.map {
                PersonEquipmentCrossRef(
                    id = userId,
                    equipmentId = it,
                )
            }
            personDao.insertIgnore(personEquipments)
        }
    }

    // TODO Write tests
    suspend fun syncTeams(
        teams: List<TeamEntity>,
        teamMemberLookup: Map<Long, List<Long>>,
        teamEquipmentLookup: Map<Long, Set<Long>>,
        memberEquipmentLookup: Map<Long, Set<Long>>,
        syncedAt: Instant,
    ) {
        val networkTeamIds = teams.map(TeamEntity::networkId).toSet()
        db.withTransaction {
            val modifiedAtLookup = getTeamLocalModifiedAt(networkTeamIds)

            for (team in teams) {
                val networkId = team.networkId
                val teamMembers = teamMemberLookup[networkId] ?: emptyList()
                val teamEquipment = teamEquipmentLookup[networkId] ?: emptySet()
                val modifiedAt = modifiedAtLookup[networkId]
                syncTeam(
                    team,
                    modifiedAt,
                    teamMembers,
                    teamEquipment,
                    syncedAt,
                )
            }

            syncMemberEquipment(memberEquipmentLookup)
        }
    }

    private suspend fun syncMembers(
        teamId: Long,
        memberIds: Collection<Long>,
    ) = db.withTransaction {
        val teamDao = db.teamDao()

        teamDao.deleteUnspecifiedMembers(teamId, memberIds)

        val memberContacts = memberIds.map {
            PersonContactEntity(
                id = it,
                "",
                "",
                "",
                "",
                "",
            )
        }
        db.personContactDao().insertIgnore(memberContacts)

        val teamMembersRefs = memberIds.map {
            TeamMemberCrossRef(
                teamId = teamId,
                contactId = it,
            )
        }
        teamDao.upsert(teamMembersRefs)
    }

    private suspend fun syncEquipment(
        teamId: Long,
        equipmentIds: Set<Long>,
    ) = db.withTransaction {
        val teamDao = db.teamDao()
        teamDao.deleteUnspecifiedEquipment(teamId, equipmentIds)

        val teamEquipments = equipmentIds.map {
            TeamEquipmentCrossRef(
                teamId = teamId,
                equipmentId = it,
            )
        }
        teamDao.insertIgnore(teamEquipments)
    }

    private suspend fun syncTeam(
        team: TeamEntity,
        modifiedAt: PopulatedLocalModifiedAt?,
        members: List<Long>,
        equipment: Set<Long>,
        syncedAt: Instant,
    ) = db.withTransaction {
        val teamDao = db.teamDao()

        val isLocallyModified = modifiedAt?.isLocallyModified ?: false
        if (modifiedAt == null) {
            val id = teamDao.insertOrRollbackTeamRoot(
                syncedAt,
                networkId = team.networkId,
                incidentId = team.incidentId,
            )
            teamDao.insert(team.copy(id = id))

            syncMembers(id, members)
            syncEquipment(id, equipment)

            return@withTransaction true
        } else if (!isLocallyModified) {
            if (teamDao.getRootCount(
                    id = modifiedAt.id,
                    expectedLocalModifiedAt = modifiedAt.localModifiedAt,
                    networkId = team.networkId,
                ) == 0
            ) {
                throw Exception("Worksite has been changed since local modified state was fetched")
            }

            teamDao.syncUpdateWorksiteRoot(
                id = modifiedAt.id,
                expectedLocalModifiedAt = modifiedAt.localModifiedAt,
                syncedAt = syncedAt,
                networkId = team.networkId,
                incidentId = team.incidentId,
            )
            with(team) {
                teamDao.syncUpdateTeam(
                    id = modifiedAt.id,
                    networkId = networkId,
                    incidentId = incidentId,
                    name = name,
                    notes = notes,
                    color = color,
                    caseCount = caseCount,
                    completeCount = completeCount,
                )
            }

            // Should return a valid ID if UPDATE OR ROLLBACK query succeeded
            val teamId = teamDao.getTeamId(team.networkId)

            syncMembers(teamId, members)
            syncEquipment(teamId, equipment)

            return@withTransaction true
        } else {
            // Resolving changes at this point is not worth the complexity.
            // Defer to worksite (snapshot) changes resolving successfully and completely.
            syncLogger.log("Skip sync overwriting locally modified team ${team.id} (${team.networkId})")
        }

        false
    }
}

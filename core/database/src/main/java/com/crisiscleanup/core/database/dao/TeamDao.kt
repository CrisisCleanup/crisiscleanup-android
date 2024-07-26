package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.PopulatedLocalModifiedAt
import com.crisiscleanup.core.database.model.PopulatedTeam
import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.database.model.TeamMemberCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface TeamDao {
    @Transaction
    @Query("SELECT id FROM teams_root WHERE network_id=:networkId AND local_global_uuid=''")
    fun getTeamId(networkId: Long): Long

    @Transaction
    @Query(
        """
        SELECT *
        FROM teams
        WHERE incident_id=:incidentId
        ORDER BY name
        """,
    )
    fun streamTeams(incidentId: Long): Flow<List<PopulatedTeam>>

    @Transaction
    @Query(
        """
        SELECT id, network_id, local_modified_at, is_local_modified
        FROM teams_root
        WHERE network_id IN (:networkIds)
        """,
    )
    fun getTeamsLocalModifiedAt(networkIds: Collection<Long>): List<PopulatedLocalModifiedAt>

    @Transaction
    @Query(
        """
        INSERT OR ROLLBACK INTO teams_root (
            synced_at,
            network_id,
            incident_id
        )
        VALUES (
            :syncedAt,
            :networkId,
            :incidentId
        )
        """,
    )
    fun insertOrRollbackTeamRoot(
        syncedAt: Instant,
        networkId: Long,
        incidentId: Long,
    ): Long

    @Insert
    fun insert(team: TeamEntity)

    @Transaction
    @Query(
        """
        SELECT COUNT(id) FROM teams_root
        WHERE id=:id AND
              network_id=:networkId AND
              local_modified_at=:expectedLocalModifiedAt
        """,
    )
    fun getRootCount(
        id: Long,
        expectedLocalModifiedAt: Instant,
        networkId: Long,
    ): Int

    @Transaction
    @Query(
        """
        UPDATE OR ROLLBACK teams_root
        SET synced_at=:syncedAt,
            sync_attempt=0,
            is_local_modified=0,
            incident_id=:incidentId
        WHERE id=:id AND
              network_id=:networkId AND
              local_modified_at=:expectedLocalModifiedAt
        """,
    )
    fun syncUpdateWorksiteRoot(
        id: Long,
        expectedLocalModifiedAt: Instant,
        syncedAt: Instant,
        networkId: Long,
        incidentId: Long,
    )

    @Transaction
    @Query(
        """
        UPDATE OR ROLLBACK teams
        SET
        incident_id =:incidentId,
        name        =:name,
        notes       =:notes,
        color       =:color,
        case_count  = :caseCount,
        case_complete_count =:completeCount
        WHERE id=:id AND network_id=:networkId
        """,
    )
    fun syncUpdateTeam(
        id: Long,
        networkId: Long,
        incidentId: Long,
        name: String,
        notes: String,
        color: String,
        caseCount: Int,
        completeCount: Int,
    )

    @Transaction
    @Query(
        """
        DELETE FROM team_to_primary_contact
        WHERE team_id=:teamId AND contact_id NOT IN(:memberIds)
        """,
    )
    fun deleteUnspecifiedMembers(teamId: Long, memberIds: Collection<Long>)

    @Upsert
    fun upsert(teamMembers: Collection<TeamMemberCrossRef>)
}

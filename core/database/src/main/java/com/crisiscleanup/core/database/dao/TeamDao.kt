package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.dao.fts.PopulatedTeamMatchInfo
import com.crisiscleanup.core.database.model.PopulatedLocalModifiedAt
import com.crisiscleanup.core.database.model.PopulatedLocalTeam
import com.crisiscleanup.core.database.model.PopulatedTeam
import com.crisiscleanup.core.database.model.PopulatedTeamMemberEquipment
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.database.model.TeamEquipmentCrossRef
import com.crisiscleanup.core.database.model.TeamMemberCrossRef
import com.crisiscleanup.core.database.model.TeamWorkEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface TeamDao {
    @Transaction
    @Query("SELECT COUNT(*) FROM teams")
    fun getTeamCount(): Int

    @Transaction
    @Query("SELECT id FROM teams_root WHERE network_id=:networkId AND local_global_uuid=''")
    fun getTeamId(networkId: Long): Long

    @Transaction
    @Query("SELECT network_id FROM teams_root WHERE id=:id")
    fun getTeamNetworkId(id: Long): Long

    @Transaction
    @Query("SELECT incident_id FROM teams_root WHERE id=:id")
    fun getIncidentId(id: Long): Long

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
    @Query("SELECT * FROM teams WHERE id=:id")
    fun streamLocalTeam(id: Long): Flow<PopulatedLocalTeam?>

    @Transaction
    @Query(
        """
        SELECT p.id as userId, p.first_name as userFirstName, p.last_name as userLastName,
        e.id as equipmentId, e.name_t as equipmentKey
        FROM person_contacts p INNER JOIN team_to_primary_contact t2p ON t2p.contact_id=p.id
        INNER JOIN person_to_equipment p2e ON p.id=p2e.id
        INNER JOIN cleanup_equipment e on p2e.equipment_id=e.id
        WHERE t2p.team_id=:id
        """,
    )
    fun streamTeamMemberEquipment(id: Long): Flow<List<PopulatedTeamMemberEquipment>>

    @Transaction
    @Query(
        """
        SELECT * FROM worksites WHERE id IN (
        SELECT wt.worksite_id
        FROM team_work tw INNER JOIN work_types wt ON tw.work_type_network_id=wt.network_id
        WHERE tw.id=:id
        )
        """,
    )
    fun streamTeamWorksites(id: Long): Flow<List<PopulatedWorksite>>

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
    fun syncUpdateTeamRoot(
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
    fun upsertMembers(teamMembers: Collection<TeamMemberCrossRef>)

    @Transaction
    @Query(
        """
        DELETE FROM team_to_equipment
        WHERE team_id=:teamId AND equipment_id NOT IN(:equipmentIds)
        """,
    )
    fun deleteUnspecifiedEquipment(teamId: Long, equipmentIds: Collection<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreEquipment(teamEquipments: Collection<TeamEquipmentCrossRef>)

    @Transaction
    @Query(
        """
        DELETE FROM team_work
        WHERE id=:teamId AND work_type_network_id NOT IN(:workTypeNetworkIds)
        """,
    )
    fun deleteUnspecifiedWork(teamId: Long, workTypeNetworkIds: Collection<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreWork(teamWork: Collection<TeamWorkEntity>)

    @Transaction
    @Query("SELECT COUNT(*) FROM team_fts")
    fun getTeamFtsCount(): Int

    @Transaction
    @Query("INSERT INTO team_fts(team_fts) VALUES ('rebuild')")
    fun rebuildTeamFts()

    @Transaction
    @Query(
        """
        SELECT t.*,
        matchinfo(team_fts, 'pcnalx') AS match_info
        FROM team_fts f
        INNER JOIN teams t ON f.docid=t.id
        WHERE team_fts MATCH :query AND
        incident_id=:incidentId
        """,
    )
    fun streamMatchingTeams(
        query: String,
        incidentId: Long,
    ): Flow<List<PopulatedTeamMatchInfo>>
}

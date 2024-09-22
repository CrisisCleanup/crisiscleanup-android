package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.dao.fts.PopulatedPersonContactIdNameMatchInfo
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonEquipmentCrossRef
import com.crisiscleanup.core.database.model.PersonOrganizationCrossRef
import com.crisiscleanup.core.database.model.PopulatedPersonContactOrganization
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonContactDao {
    @Transaction
    @Query("SELECT COUNT(*) FROM person_contacts")
    fun getPersonContactCount(): Int

    @Upsert
    fun upsert(contacts: Collection<PersonContactEntity>)

    @Upsert
    fun upsertPersonOrganizations(personOrganizations: Collection<PersonOrganizationCrossRef>)

    @Transaction
    @Query(
        """
        DELETE FROM person_contacts
        WHERE id IN(
            SELECT pc.id
            FROM person_contacts pc
            LEFT JOIN organization_to_primary_contact o2pc
            ON pc.id=o2pc.contact_id
            WHERE o2pc.contact_id IS NULL
        )
        """,
    )
    fun trimIncidentOrganizationContacts()

    @Transaction
    @Query("SELECT * FROM person_contacts WHERE id=:id")
    fun getContact(id: Long): PopulatedPersonContactOrganization?

    @Transaction
    @Query("SELECT * FROM person_contacts WHERE id IN(:ids)")
    fun getContacts(ids: Collection<Long>): List<PopulatedPersonContactOrganization>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(contacts: List<PersonContactEntity>)

    @Transaction
    @Query(
        """
        DELETE FROM person_to_equipment
        WHERE id=:personId AND equipment_id NOT IN(:equipmentIds)
        """,
    )
    fun deleteUnspecifiedEquipment(personId: Long, equipmentIds: Collection<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(personEquipments: Collection<PersonEquipmentCrossRef>)

    @Transaction
    @Query(
        """
        SELECT pc.*
        FROM person_contacts pc
        INNER JOIN person_to_organization p2o ON pc.id=p2o.id
        INNER JOIN organization_to_incident o2i on p2o.organization_id=o2i.id
        WHERE o2i.incident_id=:incidentId AND (
            p2o.organization_id=:organizationId OR p2o.organization_id IN (
                SELECT affiliate_id
                FROM organization_to_affiliate
                WHERE id=:organizationId
            )
        )
        """,
    )
    fun streamTeamMembersDeployedToIncident(
        incidentId: Long,
        organizationId: Long,
    ): Flow<List<PopulatedPersonContactOrganization>>

    @Transaction
    @Query("SELECT COUNT(*) FROM person_contact_fts")
    fun getPersonContactFtsCount(): Int

    @Transaction
    @Query("INSERT INTO person_contact_fts(person_contact_fts) VALUES ('rebuild')")
    fun rebuildPersonContactFts()

    // TODO Profile
    @Transaction
    @Query(
        """
        SELECT
            p.id,
            p.first_name,
            p.last_name,
            p.email,
            p.mobile,
            p.profilePictureUri AS profile_picture,
            p.activeRoles AS active_roles,
            io.id AS organization_id,
            io.name AS organization_name,
            matchinfo(person_contact_fts, 'pcnalx') AS match_info
        FROM person_contact_fts f
        INNER JOIN person_contacts p ON f.docid=p.id
        INNER JOIN person_to_organization p2o ON p.id=p2o.id
        INNER JOIN organization_to_incident o2i on p2o.organization_id=o2i.id
        INNER JOIN incident_organizations io on p2o.organization_id=io.id
        WHERE person_contact_fts MATCH :query AND
        o2i.incident_id=:incidentId AND (
            p2o.organization_id=:organizationId OR p2o.organization_id IN (
                SELECT affiliate_id
                FROM organization_to_affiliate
                WHERE id=:organizationId
            )
        )
        """,
    )
    fun matchIncidentOrganizationPersonContactTokens(
        query: String,
        incidentId: Long,
        organizationId: Long,
    ): List<PopulatedPersonContactIdNameMatchInfo>
}

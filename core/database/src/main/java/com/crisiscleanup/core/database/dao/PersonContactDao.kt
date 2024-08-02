package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonEquipmentCrossRef
import com.crisiscleanup.core.database.model.PersonOrganizationCrossRef
import com.crisiscleanup.core.database.model.PopulatedPersonContactOrganization

@Dao
interface PersonContactDao {
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
}

package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.PersonContactEntity

@Dao
interface PersonContactDao {
    @Upsert
    fun upsert(contacts: Collection<PersonContactEntity>)

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
        """
    )
    fun trimIncidentOrganizationContacts()
}
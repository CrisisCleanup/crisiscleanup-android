package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PersonContactEntity
import javax.inject.Inject

class IncidentOrganizationDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
) {
    suspend fun saveOrganizations(
        organizations: Collection<IncidentOrganizationEntity>,
        contacts: Collection<PersonContactEntity>,
        organizationContactCrossRefs: Collection<OrganizationPrimaryContactCrossRef>,
    ) {
        db.withTransaction {
            db.incidentOrganizationDao().upsert(organizations)
            db.personContactDao().upsert(contacts)
            db.incidentOrganizationDao()
                .insertIgnorePrimaryContactCrossRefs(organizationContactCrossRefs)
        }
    }
}
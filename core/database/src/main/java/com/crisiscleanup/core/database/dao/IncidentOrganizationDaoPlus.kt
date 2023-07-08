package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PersonContactEntity
import javax.inject.Inject

class IncidentOrganizationDaoPlus @Inject constructor(
    internal val db: CrisisCleanupDatabase,
) {
    suspend fun saveOrganizations(
        organizations: Collection<IncidentOrganizationEntity>,
        contacts: Collection<PersonContactEntity>,
    ) = db.withTransaction {
        db.incidentOrganizationDao().upsert(organizations)
        db.personContactDao().upsert(contacts)
    }

    suspend fun saveOrganizationReferences(
        organizations: Collection<IncidentOrganizationEntity>,
        organizationContactCrossRefs: Collection<OrganizationPrimaryContactCrossRef>,
        organizationAffiliates: Collection<OrganizationAffiliateEntity>,
    ) = db.withTransaction {
        val organizationDao = db.incidentOrganizationDao()
        // TODO Test coverage. Only specified organization data is deleted and updated.
        val organizationIds = organizations.map(IncidentOrganizationEntity::id).toSet()
        organizationDao.deletePrimaryContactCrossRefs(organizationIds)
        organizationDao.insertIgnorePrimaryContactCrossRefs(organizationContactCrossRefs)
        organizationDao.deleteOrganizationAffiliates(organizationIds)
        organizationDao.insertIgnoreAffiliateOrganization(organizationAffiliates)
    }
}
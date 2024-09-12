package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationIncidentCrossRef
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PersonContactEntity
import javax.inject.Inject

class IncidentOrganizationDaoPlus @Inject constructor(
    internal val db: CrisisCleanupDatabase,
) {
    suspend fun saveOrganizations(
        organizations: Collection<IncidentOrganizationEntity>,
        contacts: Collection<PersonContactEntity>,
        organizationIncidentLookup: Map<Long, Collection<Long>>,
    ) = db.withTransaction {
        val organizationDao = db.incidentOrganizationDao()
        organizationDao.upsert(organizations)
        db.personContactDao().upsert(contacts)

        for ((organizationId, incidentIds) in organizationIncidentLookup) {
            organizationDao.deleteUnspecifiedOrganizationIncidents(organizationId, incidentIds)
            val organizationIncidents = incidentIds.map {
                OrganizationIncidentCrossRef(organizationId, it)
            }
            organizationDao.upsertOrganizationIncidents(organizationIncidents)
        }
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

    suspend fun saveMissing(
        organizations: List<IncidentOrganizationEntity>,
        affiliateIds: List<Collection<Long>>,
    ) = db.withTransaction {
        val organizationsDao = db.incidentOrganizationDao()
        val newOrganizations = mutableListOf<IncidentOrganizationEntity>()
        val newAffiliates = mutableListOf<OrganizationAffiliateEntity>()
        for (i in organizations.indices) {
            val organization = organizations[i]
            if (organizationsDao.findOrganization(organization.id) == null) {
                newOrganizations.add(organization)
                val affiliates = affiliateIds[i].map {
                    OrganizationAffiliateEntity(organization.id, it)
                }
                newAffiliates.addAll(affiliates)
            }
        }
        organizationsDao.upsert(newOrganizations)
        organizationsDao.insertIgnoreAffiliateOrganization(newAffiliates)
    }
}

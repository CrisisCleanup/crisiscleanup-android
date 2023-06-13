package com.crisiscleanup.core.database.dao

import androidx.room.withTransaction
import com.crisiscleanup.core.database.CrisisCleanupDatabase
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PopulatedOrganizationIdNameMatchInfo
import com.crisiscleanup.core.database.util.ftsGlobEnds
import com.crisiscleanup.core.database.util.ftsSanitize
import com.crisiscleanup.core.database.util.ftsSanitizeAsToken
import com.crisiscleanup.core.model.data.OrganizationIdName
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import javax.inject.Inject

class IncidentOrganizationDaoPlus @Inject constructor(
    private val db: CrisisCleanupDatabase,
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
        // TODO Write tests that only specific organization data is deleted and updated
        val organizationIds = organizations.map(IncidentOrganizationEntity::id).toSet()
        organizationDao.deletePrimaryContactCrossRefs(organizationIds)
        organizationDao.insertIgnorePrimaryContactCrossRefs(organizationContactCrossRefs)
        organizationDao.deleteOrganizationAffiliates(organizationIds)
        organizationDao.insertIgnoreAffiliateOrganization(organizationAffiliates)
    }

    suspend fun getOrganizations(matching: String): List<OrganizationIdName> = coroutineScope {
        db.withTransaction {
            val results = db.incidentOrganizationDao()
                .matchOrganizationName(matching.ftsSanitize.ftsGlobEnds)

            ensureActive()

            // TODO ensureActive() between (strides of) score computations
            results
                .sortedByDescending { it.sortScore }
                .map(PopulatedOrganizationIdNameMatchInfo::idName)
        }
    }

    suspend fun rebuildOrganizationFts(force: Boolean = false) = db.withTransaction {
        with(db.incidentOrganizationDao()) {
            var rebuild = force
            if (!force) {
                getRandomOrganizationName()?.let { orgName ->
                    val ftsMatch = matchOrganizationName(orgName.ftsSanitizeAsToken)
                    rebuild = ftsMatch.isEmpty()
                }
            }
            if (rebuild) {
                rebuildOrganizationFts()
            }
        }
    }
}
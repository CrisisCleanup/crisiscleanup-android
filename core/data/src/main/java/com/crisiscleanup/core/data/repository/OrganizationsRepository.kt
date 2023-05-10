package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.model.PopulatedIncidentOrganization
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asLookup
import com.crisiscleanup.core.model.data.IncidentOrganization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface OrganizationsRepository {
    val organizationNameLookup: Flow<Map<Long, String>>

    val organizationLookup: Flow<Map<Long, IncidentOrganization>>

    fun getOrganizationAffiliateIds(organizationId: Long): Set<Long>
}

class OfflineFirstOrganizationsRepository @Inject constructor(
    private val incidentOrganizationDao: IncidentOrganizationDao,
) : OrganizationsRepository {
    override val organizationNameLookup =
        incidentOrganizationDao.streamOrganizationNames().mapLatest { it.asLookup() }

    override val organizationLookup = incidentOrganizationDao.streamOrganizations()
        .mapLatest {
            it.map(PopulatedIncidentOrganization::asExternalModel)
                .associateBy(IncidentOrganization::id)
        }

    override fun getOrganizationAffiliateIds(organizationId: Long) =
        incidentOrganizationDao.getAffiliateOrganizationIds(organizationId).toMutableSet()
            .apply { add(organizationId) }
}
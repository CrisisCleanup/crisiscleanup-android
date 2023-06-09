package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.PopulatedIncidentOrganization
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asLookup
import com.crisiscleanup.core.model.data.IncidentOrganization
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface OrganizationsRepository {
    val organizationNameLookup: Flow<Map<Long, String>>

    val organizationLookup: Flow<Map<Long, IncidentOrganization>>

    fun getOrganizationAffiliateIds(organizationId: Long): Set<Long>

    suspend fun getNearbyClaimingOrganizations(
        latitude: Double,
        longitude: Double
    ): List<IncidentOrganization>
}

class OfflineFirstOrganizationsRepository @Inject constructor(
    private val incidentOrganizationDao: IncidentOrganizationDao,
    private val incidentOrganizationDaoPlus: IncidentOrganizationDaoPlus,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
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

    override suspend fun getNearbyClaimingOrganizations(
        latitude: Double,
        longitude: Double,
    ): List<IncidentOrganization> {
        val networkOrganizations = networkDataSource.getNearbyOrganizations(latitude, longitude)
        val (
            organizations,
            primaryContacts,
            organizationContactCrossRefs,
            organizationAffiliates,
        ) = networkOrganizations.asEntities(getContacts = true, getReferences = true)
        incidentOrganizationDaoPlus.saveOrganizations(
            organizations,
            primaryContacts,
        )
        incidentOrganizationDaoPlus.saveOrganizationReferences(
            organizations,
            organizationContactCrossRefs,
            organizationAffiliates,
        )

        val organizationIds = organizations.map(IncidentOrganizationEntity::id)
        return incidentOrganizationDao.getOrganizations(organizationIds)
            .map(PopulatedIncidentOrganization::asExternalModel)
    }
}
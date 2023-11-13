package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.model.asEntitySource
import com.crisiscleanup.core.database.dao.IncidentOrganizationDao
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.LocationDao
import com.crisiscleanup.core.database.dao.LocationDaoPlus
import com.crisiscleanup.core.database.dao.fts.getMatchingOrganizations
import com.crisiscleanup.core.database.model.PopulatedIncidentOrganization
import com.crisiscleanup.core.database.model.PopulatedLocation
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.database.model.asLookup
import com.crisiscleanup.core.model.data.IncidentOrganization
import com.crisiscleanup.core.model.data.LocationBoundsConverter
import com.crisiscleanup.core.model.data.OrganizationIdName
import com.crisiscleanup.core.model.data.OrganizationLocationAreaBounds
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkIncidentOrganization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface OrganizationsRepository {
    val organizationNameLookup: Flow<Map<Long, String>>

    val organizationLookup: Flow<Map<Long, IncidentOrganization>>

    suspend fun syncOrganization(
        organizationId: Long,
        force: Boolean = false,
        updateLocations: Boolean = false,
    )

    fun getOrganizationAffiliateIds(organizationId: Long, addOrganizationId: Boolean): Set<Long>

    suspend fun getNearbyClaimingOrganizations(
        latitude: Double,
        longitude: Double,
    ): List<IncidentOrganization>

    fun streamPrimarySecondaryAreas(organizationId: Long): Flow<OrganizationLocationAreaBounds>

    suspend fun getMatchingOrganizations(q: String): List<OrganizationIdName>
}

class OfflineFirstOrganizationsRepository @Inject constructor(
    private val incidentOrganizationDao: IncidentOrganizationDao,
    private val incidentOrganizationDaoPlus: IncidentOrganizationDaoPlus,
    private val locationDao: LocationDao,
    private val locationDaoPlus: LocationDaoPlus,
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val locationBoundsConverter: LocationBoundsConverter,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : OrganizationsRepository {
    override val organizationNameLookup =
        incidentOrganizationDao.streamOrganizationNames().mapLatest { it.asLookup() }

    override val organizationLookup = incidentOrganizationDao.streamOrganizations()
        .mapLatest {
            it.map(PopulatedIncidentOrganization::asExternalModel)
                .associateBy(IncidentOrganization::id)
        }

    private suspend fun saveOrganizations(networkOrganizations: Collection<NetworkIncidentOrganization>) {
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
    }

    override suspend fun syncOrganization(
        organizationId: Long,
        force: Boolean,
        updateLocations: Boolean,
    ) {
        if (force || incidentOrganizationDao.findOrganization(organizationId) == null) {
            try {
                val networkOrganizations =
                    networkDataSource.getOrganizations(listOf(organizationId))
                saveOrganizations(networkOrganizations)

                if (updateLocations) {
                    val locationIds = networkOrganizations
                        .flatMap {
                            listOf(it.primaryLocation, it.secondaryLocation)
                        }
                        .filterNotNull()
                    if (locationIds.isNotEmpty()) {
                        val locations = networkDataSource.getIncidentLocations(locationIds)
                            .asEntitySource()
                        locationDaoPlus.saveLocations(locations)
                    }
                }
            } catch (e: Exception) {
                logger.logException(e)
            }
        }
    }

    override fun getOrganizationAffiliateIds(organizationId: Long, addOrganizationId: Boolean) =
        incidentOrganizationDao.getAffiliateOrganizationIds(organizationId).toMutableSet()
            .apply {
                if (addOrganizationId) {
                    add(organizationId)
                }
            }

    override suspend fun getNearbyClaimingOrganizations(
        latitude: Double,
        longitude: Double,
    ): List<IncidentOrganization> {
        try {
            val networkOrganizations = networkDataSource.getNearbyOrganizations(latitude, longitude)
            saveOrganizations(networkOrganizations)
            val organizationIds = networkOrganizations.map(NetworkIncidentOrganization::id)
            return incidentOrganizationDao.getOrganizations(organizationIds)
                .map(PopulatedIncidentOrganization::asExternalModel)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return emptyList()
    }

    override fun streamPrimarySecondaryAreas(organizationId: Long) =
        incidentOrganizationDao.streamLocationIds(organizationId).map {
            it?.let {
                try {
                    val ids = listOf(it.primary, it.secondary).mapNotNull { id -> id }
                    // TODO Stream bounds
                    val bounds = locationDao.getLocations(ids)
                        .map(PopulatedLocation::asExternalModel)
                        .map { location -> locationBoundsConverter.convert(location) }
                    val primary = if (it.primary == null || bounds.isEmpty()) null else bounds[0]
                    val secondary =
                        if (it.secondary == null || bounds.size < ids.size) null else bounds[bounds.size - 1]
                    return@map OrganizationLocationAreaBounds(primary, secondary)
                } catch (e: Exception) {
                    logger.logException(e)
                }
            }

            OrganizationLocationAreaBounds()
        }

    override suspend fun getMatchingOrganizations(q: String) =
        incidentOrganizationDaoPlus.getMatchingOrganizations(q)
}

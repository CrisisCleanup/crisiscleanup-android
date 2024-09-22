package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.common.log.AppLogger
import com.crisiscleanup.core.common.log.CrisisCleanupLoggers
import com.crisiscleanup.core.common.log.Logger
import com.crisiscleanup.core.data.model.PersonContactEntities
import com.crisiscleanup.core.data.model.asEntities
import com.crisiscleanup.core.data.model.asExternalModel
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.PersonContactDao
import com.crisiscleanup.core.database.dao.PersonContactDaoPlus
import com.crisiscleanup.core.database.dao.UserRoleDao
import com.crisiscleanup.core.database.model.UserRoleEntity
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.OrganizationIdName
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.model.data.PersonOrganization
import com.crisiscleanup.core.model.data.UserRole
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkPersonContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

interface UsersRepository {
    val streamUserRoleLookup: Flow<Map<Int, UserRole>>

    suspend fun getMatchingUsers(
        q: String,
        organization: Long,
        limit: Int = 10,
    ): List<PersonContact>

    suspend fun queryUpdateUsers(userIds: Collection<Long>)

    suspend fun getUserProfiles(
        userIds: Collection<Long>,
        forceUpdateProfiles: Boolean = false,
    ): List<PersonContact>

    suspend fun loadUserRoles()

    fun streamTeamMembers(incidentId: Long, organizationId: Long): Flow<List<PersonOrganization>>
}

class OfflineFirstUsersRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val personContactDao: PersonContactDao,
    private val personContactDaoPlus: PersonContactDaoPlus,
    private val incidentOrganizationDaoPlus: IncidentOrganizationDaoPlus,
    private val userRoleDao: UserRoleDao,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : UsersRepository {
    override val streamUserRoleLookup = userRoleDao.streamUserRoles()
        .mapLatest { roles ->
            roles.associate {
                it.id to UserRole(
                    it.id,
                    nameKey = it.nameKey,
                    descriptionKey = it.descriptionKey,
                    level = it.level,
                )
            }
        }

    override suspend fun getMatchingUsers(
        q: String,
        organization: Long,
        limit: Int,
    ): List<PersonContact> {
        try {
            return networkDataSource.searchUsers(q, organization, limit)
                .map(NetworkPersonContact::asExternalModel)
        } catch (e: Exception) {
            logger.logException(e)
        }
        return emptyList()
    }

    override suspend fun queryUpdateUsers(userIds: Collection<Long>) {
        try {
            for (subset in userIds.chunked(100)) {
                val networkUsers = networkDataSource.getUsers(subset)
                val entities = networkUsers.mapNotNull(NetworkPersonContact::asEntities)

                val organizations = entities.map(PersonContactEntities::organization)
                val affiliates = entities.map(PersonContactEntities::organizationAffiliates)
                incidentOrganizationDaoPlus.saveMissing(organizations, affiliates)

                val persons = entities.map(PersonContactEntities::personContact)
                val personOrganizations = entities.map(PersonContactEntities::personToOrganization)
                personContactDaoPlus.savePersons(persons, personOrganizations)
            }
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override suspend fun getUserProfiles(
        userIds: Collection<Long>,
        forceUpdateProfiles: Boolean,
    ): List<PersonContact> {
        var profiles = personContactDao.getContacts(userIds)

        if (forceUpdateProfiles ||
            userIds.size != profiles.size ||
            profiles.any { it.entity.profilePictureUri.isBlank() }
        ) {
            queryUpdateUsers(userIds)
            profiles = personContactDao.getContacts(userIds)
        }

        return profiles.map { it.entity.asExternalModel() }
    }

    override suspend fun loadUserRoles() {
        try {
            val userRoles = networkDataSource.getNetworkUserRoles()
            val entities = userRoles.map {
                with(it) {
                    UserRoleEntity(
                        id = id,
                        nameKey = nameKey,
                        descriptionKey = descriptionKey,
                        level = level,
                    )
                }
            }
            userRoleDao.upsertRoles(entities)
        } catch (e: Exception) {
            logger.logException(e)
        }
    }

    override fun streamTeamMembers(
        incidentId: Long,
        organizationId: Long,
    ) = personContactDao.streamTeamMembersDeployedToIncident(incidentId, organizationId)
        .mapLatest { data ->
            data.filter { it.organization != null }
                .map { personOrganization ->
                    val person = personOrganization.entity.asExternalModel()
                    val organization = with(personOrganization.organization!!) {
                        OrganizationIdName(id, name)
                    }
                    PersonOrganization(
                        person,
                        organization,
                    )
                }
                .sortedBy { it.person.fullName }
        }
}

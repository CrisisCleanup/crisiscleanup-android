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
import com.crisiscleanup.core.database.model.asExternalModel
import com.crisiscleanup.core.model.data.PersonContact
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkPersonContact
import javax.inject.Inject

interface UsersRepository {
    suspend fun getMatchingUsers(
        q: String,
        organization: Long,
        limit: Int = 10,
    ): List<PersonContact>

    suspend fun queryUpdateUsers(userIds: Collection<Long>)

    suspend fun getUserProfiles(
        userIds: Collection<Long>,
        updateProfilePics: Boolean = false,
    ): List<PersonContact>
}

class OfflineFirstUsersRepository @Inject constructor(
    private val networkDataSource: CrisisCleanupNetworkDataSource,
    private val personContactDao: PersonContactDao,
    private val personContactDaoPlus: PersonContactDaoPlus,
    private val incidentOrganizationDaoPlus: IncidentOrganizationDaoPlus,
    @Logger(CrisisCleanupLoggers.App) private val logger: AppLogger,
) : UsersRepository {
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
        updateProfilePics: Boolean,
    ): List<PersonContact> {
        var profiles = personContactDao.getContacts(userIds)

        // TODO Query update as profile pictures expire
        if (updateProfilePics &&
            profiles.any { it.entity.profilePictureUri.isBlank() }
        ) {
            queryUpdateUsers(userIds)
            profiles = personContactDao.getContacts(userIds)
        }

        return profiles.map { it.entity.asExternalModel() }
    }
}

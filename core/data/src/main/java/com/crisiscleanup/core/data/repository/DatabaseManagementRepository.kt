package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import javax.inject.Inject

interface DatabaseManagementRepository {
    suspend fun rebuildFts()
}

class CrisisCleanupDatabaseManagementRepository @Inject constructor(
    private val organizationDaoPlus: IncidentOrganizationDaoPlus,
) : DatabaseManagementRepository {
    override suspend fun rebuildFts() {
        organizationDaoPlus.rebuildOrganizationFts()
    }
}
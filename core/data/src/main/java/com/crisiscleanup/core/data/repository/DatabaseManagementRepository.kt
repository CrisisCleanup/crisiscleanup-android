package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.database.dao.IncidentDaoPlus
import com.crisiscleanup.core.database.dao.IncidentOrganizationDaoPlus
import com.crisiscleanup.core.database.dao.WorksiteDaoPlus
import com.crisiscleanup.core.database.dao.fts.rebuildIncidentFts
import com.crisiscleanup.core.database.dao.fts.rebuildOrganizationFts
import com.crisiscleanup.core.database.dao.fts.rebuildWorksiteTextFts
import javax.inject.Inject

interface DatabaseManagementRepository {
    suspend fun rebuildFts()
}

class CrisisCleanupDatabaseManagementRepository @Inject constructor(
    private val incidentDaoPlus: IncidentDaoPlus,
    private val organizationDaoPlus: IncidentOrganizationDaoPlus,
    private val worksiteDaoPlus: WorksiteDaoPlus,
) : DatabaseManagementRepository {
    override suspend fun rebuildFts() {
        incidentDaoPlus.rebuildIncidentFts()
        organizationDaoPlus.rebuildOrganizationFts()
        worksiteDaoPlus.rebuildWorksiteTextFts()
    }
}
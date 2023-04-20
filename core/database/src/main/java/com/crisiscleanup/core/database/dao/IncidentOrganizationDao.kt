package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.IncidentOrganizationSyncStatsEntity
import com.crisiscleanup.core.database.model.OrganizationIdName
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentOrganizationDao {
    @Upsert
    fun upsert(organization: Collection<IncidentOrganizationEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnorePrimaryContactCrossRefs(
        contactCrossRefs: Collection<OrganizationPrimaryContactCrossRef>
    )

    @Transaction
    @Query("SELECT id, name FROM incident_organizations")
    fun getOrganizations(): Flow<List<OrganizationIdName>>

    @Transaction
    @Query(
        """
        SELECT *
        FROM incident_organization_sync_stats
        WHERE incident_id==:incidentId
        """
    )
    fun getSyncStats(incidentId: Long): IncidentOrganizationSyncStatsEntity?

    @Upsert
    fun upsertStats(stats: IncidentOrganizationSyncStatsEntity)
}
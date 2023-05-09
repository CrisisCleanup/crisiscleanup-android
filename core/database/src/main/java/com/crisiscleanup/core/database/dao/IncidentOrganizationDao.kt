package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.IncidentOrganizationSyncStatsEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationIdName
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentOrganizationDao {
    @Upsert
    fun upsert(organization: Collection<IncidentOrganizationEntity>)

    @Transaction
    @Query("DELETE FROM organization_to_primary_contact WHERE organization_id IN(:orgIds)")
    fun deletePrimaryContactCrossRefs(orgIds: Collection<Long>)

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

    @Transaction
    @Query("DELETE FROM organization_to_affiliate WHERE id IN(:orgIds)")
    fun deleteOrganizationAffiliates(orgIds: Collection<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreAffiliateOrganization(affiliates: Collection<OrganizationAffiliateEntity>)

    @Transaction
    @Query("SELECT affiliate_id FROM organization_to_affiliate WHERE id=:orgId")
    fun streamAffiliateOrganizationIds(orgId: Long): List<Long>
}
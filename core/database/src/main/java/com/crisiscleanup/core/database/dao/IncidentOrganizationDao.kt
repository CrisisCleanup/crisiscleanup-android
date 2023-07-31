package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.dao.fts.PopulatedOrganizationIdNameMatchInfo
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.IncidentOrganizationSyncStatsEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PopulatedIncidentOrganization
import com.crisiscleanup.core.model.data.OrganizationIdName
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
    fun streamOrganizationNames(): Flow<List<OrganizationIdName>>

    @Transaction
    @Query("SELECT * FROM incident_organizations")
    fun streamOrganizations(): Flow<List<PopulatedIncidentOrganization>>

    @Transaction
    @Query("SELECT * FROM incident_organizations WHERE id IN(:ids)")
    fun getOrganizations(ids: Collection<Long>): List<PopulatedIncidentOrganization>

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
    fun getAffiliateOrganizationIds(orgId: Long): List<Long>

    @Transaction
    @Query("SELECT name FROM incident_organizations ORDER BY RANDOM() LIMIT 1")
    fun getRandomOrganizationName(): String?

    @Transaction
    @Query("INSERT INTO incident_organization_fts(incident_organization_fts) VALUES ('rebuild')")
    fun rebuildOrganizationFts()

    @Transaction
    @Query(
        """
        SELECT io.id, f.name,
        matchinfo(incident_organization_fts, 'pcnalx') AS match_info
        FROM incident_organization_fts f
        INNER JOIN incident_organizations io ON f.docid=io.id
        WHERE incident_organization_fts MATCH :query
        """
    )
    fun matchOrganizationName(query: String): List<PopulatedOrganizationIdNameMatchInfo>

    @Transaction
    @Query("SELECT id FROM incident_organizations WHERE id=:id")
    fun findOrganization(id: Long): Long?
}
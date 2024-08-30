package com.crisiscleanup.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverters
import com.crisiscleanup.core.database.dao.fts.IncidentFtsEntity
import com.crisiscleanup.core.database.dao.fts.IncidentOrganizationFtsEntity
import com.crisiscleanup.core.database.dao.fts.TeamFtsEntity
import com.crisiscleanup.core.database.dao.fts.WorksiteTextFtsEntity
import com.crisiscleanup.core.database.model.CaseHistoryEventAttrEntity
import com.crisiscleanup.core.database.model.CaseHistoryEventEntity
import com.crisiscleanup.core.database.model.EquipmentEntity
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentFormFieldEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.IncidentOrganizationEntity
import com.crisiscleanup.core.database.model.IncidentOrganizationSyncStatsEntity
import com.crisiscleanup.core.database.model.IncidentWorksitesFullSyncStatsEntity
import com.crisiscleanup.core.database.model.IncidentWorksitesSecondarySyncStatsEntity
import com.crisiscleanup.core.database.model.LanguageTranslationEntity
import com.crisiscleanup.core.database.model.ListEntity
import com.crisiscleanup.core.database.model.LocationEntity
import com.crisiscleanup.core.database.model.NetworkFileEntity
import com.crisiscleanup.core.database.model.NetworkFileLocalImageEntity
import com.crisiscleanup.core.database.model.OrganizationAffiliateEntity
import com.crisiscleanup.core.database.model.OrganizationPrimaryContactCrossRef
import com.crisiscleanup.core.database.model.PersonContactEntity
import com.crisiscleanup.core.database.model.PersonEquipmentCrossRef
import com.crisiscleanup.core.database.model.PersonOrganizationCrossRef
import com.crisiscleanup.core.database.model.PopulatedIdNetworkId
import com.crisiscleanup.core.database.model.PopulatedLocalWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.RecentWorksiteEntity
import com.crisiscleanup.core.database.model.SyncLogEntity
import com.crisiscleanup.core.database.model.TeamEntity
import com.crisiscleanup.core.database.model.TeamEquipmentCrossRef
import com.crisiscleanup.core.database.model.TeamMemberCrossRef
import com.crisiscleanup.core.database.model.TeamRootEntity
import com.crisiscleanup.core.database.model.UserRoleEntity
import com.crisiscleanup.core.database.model.WorkTypeEntity
import com.crisiscleanup.core.database.model.WorkTypeStatusEntity
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteFlagEntity
import com.crisiscleanup.core.database.model.WorksiteFormDataEntity
import com.crisiscleanup.core.database.model.WorksiteLocalImageEntity
import com.crisiscleanup.core.database.model.WorksiteNetworkFileCrossRef
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import com.crisiscleanup.core.database.model.WorksiteSyncStatsEntity
import com.crisiscleanup.core.database.util.InstantConverter
import kotlinx.datetime.Instant

@Database(
    entities = [
        WorkTypeStatusEntity::class,
        IncidentEntity::class,
        IncidentLocationEntity::class,
        IncidentIncidentLocationCrossRef::class,
        IncidentFormFieldEntity::class,
        LocationEntity::class,
        WorksiteSyncStatsEntity::class,
        WorksiteRootEntity::class,
        WorksiteEntity::class,
        WorkTypeEntity::class,
        WorksiteFormDataEntity::class,
        WorksiteFlagEntity::class,
        WorksiteNoteEntity::class,
        LanguageTranslationEntity::class,
        SyncLogEntity::class,
        WorksiteChangeEntity::class,
        IncidentOrganizationEntity::class,
        OrganizationAffiliateEntity::class,
        PersonContactEntity::class,
        OrganizationPrimaryContactCrossRef::class,
        IncidentOrganizationSyncStatsEntity::class,
        RecentWorksiteEntity::class,
        WorkTypeTransferRequestEntity::class,
        NetworkFileEntity::class,
        WorksiteNetworkFileCrossRef::class,
        NetworkFileLocalImageEntity::class,
        WorksiteLocalImageEntity::class,
        IncidentWorksitesFullSyncStatsEntity::class,
        IncidentFtsEntity::class,
        IncidentOrganizationFtsEntity::class,
        CaseHistoryEventEntity::class,
        CaseHistoryEventAttrEntity::class,
        PersonOrganizationCrossRef::class,
        WorksiteTextFtsEntity::class,
        IncidentWorksitesSecondarySyncStatsEntity::class,
        ListEntity::class,
        TeamRootEntity::class,
        TeamEntity::class,
        TeamMemberCrossRef::class,
        EquipmentEntity::class,
        TeamEquipmentCrossRef::class,
        PersonEquipmentCrossRef::class,
        TeamFtsEntity::class,
        UserRoleEntity::class,
    ],
    version = 1,
)
@TypeConverters(
    InstantConverter::class,
)
abstract class TestCrisisCleanupDatabase : CrisisCleanupDatabase() {
    abstract fun testIncidentDao(): TestIncidentDao
    abstract fun testWorksiteDao(): TestWorksiteDao
    abstract fun testFlagDao(): TestFlagDao
    abstract fun testFormDataDao(): TestFormDataDao
    abstract fun testNoteDao(): TestNoteDao
    abstract fun testWorkTypeDao(): TestWorkTypeDao
    abstract fun testWorksiteChangeDao(): TestWorksiteChangeDao
    abstract fun testWorkTypeRequestDao(): TestWorkTypeRequestDao
}

@Dao
interface TestIncidentDao {
    @Transaction
    @Query(
        """
        UPDATE incidents
        SET is_archived=1
        WHERE id NOT IN(:unarchivedIds)
        """,
    )
    suspend fun setExcludedArchived(unarchivedIds: Set<Long>)
}

@Dao
interface TestWorksiteDao {
    @Transaction
    @Query(
        """
        UPDATE worksites_root
        SET local_modified_at=:modifiedAt, is_local_modified=1
        WHERE id=:id
        """,
    )
    fun setLocallyModified(id: Long, modifiedAt: Instant)

    @Transaction
    @Query("SELECT * FROM worksites WHERE id=:id")
    fun getLocalWorksite(id: Long): PopulatedLocalWorksite

    @Transaction
    @Query("SELECT * FROM worksites_root WHERE id=:id")
    fun getRootEntity(id: Long): WorksiteRootEntity?

    @Transaction
    @Query("SELECT * FROM worksites WHERE id=:id")
    fun getWorksiteEntity(id: Long): WorksiteEntity?

    @Transaction
    @Query(
        """
        SELECT *
        FROM worksites
        WHERE incident_id=:incidentId
        ORDER BY updated_at DESC, id DESC
        LIMIT :limit
        OFFSET :offset
        """,
    )
    fun getWorksites(
        incidentId: Long,
        limit: Int,
        offset: Int = 0,
    ): List<PopulatedWorksite>
}

@Dao
interface TestFlagDao {
    @Transaction
    @Query("SELECT * FROM worksite_flags WHERE worksite_id=:worksiteId ORDER BY id")
    fun getEntities(worksiteId: Long): List<WorksiteFlagEntity>
}

@Dao
interface TestFormDataDao {
    @Transaction
    @Query("SELECT * FROM worksite_form_data WHERE worksite_id=:worksiteId ORDER BY field_key")
    fun getEntities(worksiteId: Long): List<WorksiteFormDataEntity>
}

@Dao
interface TestNoteDao {
    @Transaction
    @Query("SELECT * FROM worksite_notes WHERE worksite_id=:worksiteId ORDER BY id")
    fun getEntities(worksiteId: Long): List<WorksiteNoteEntity>

    @Transaction
    @Query(
        """
        UPDATE worksite_notes
        SET network_id=:networkId,
            local_global_uuid=:localGlobalUuid
        WHERE id=:id
        """,
    )
    fun updateNetworkId(id: Long, networkId: Long, localGlobalUuid: String = "")
}

@Dao
interface TestWorkTypeDao {
    @Transaction
    @Query(
        """
        SELECT *
        FROM work_types
        WHERE worksite_id=:worksiteId
        ORDER BY work_type ASC, id ASC
        """,
    )
    fun getEntities(worksiteId: Long): List<WorkTypeEntity>
}

@Dao
interface TestWorksiteChangeDao {
    @Transaction
    @Query(
        """
        SELECT *
        FROM worksite_changes
        WHERE worksite_id=:worksiteId
        ORDER BY created_at DESC
        """,
    )
    fun getEntities(worksiteId: Long): List<WorksiteChangeEntity>

    @Transaction
    @Query(
        """
        SELECT *
        FROM worksite_changes
        WHERE worksite_id=:worksiteId
        ORDER BY id ASC
        """,
    )
    fun getEntitiesOrderId(worksiteId: Long): List<WorksiteChangeEntity>
}

@Dao
interface TestWorkTypeRequestDao {
    @Transaction
    @Query(
        """
        SELECT *
        FROM worksite_work_type_requests
        WHERE worksite_id=:worksiteId
        """,
    )
    fun getEntities(worksiteId: Long): List<WorkTypeTransferRequestEntity>

    @Transaction
    @Query("SELECT * FROM worksite_work_type_requests")
    fun getEntities(): List<WorkTypeTransferRequestEntity>

    @Transaction
    @Query("SELECT id, network_id FROM worksite_work_type_requests WHERE worksite_id=:worksiteId")
    fun getNetworkedIdMap(worksiteId: Long): List<PopulatedIdNetworkId>
}

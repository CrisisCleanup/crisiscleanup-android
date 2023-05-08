package com.crisiscleanup.core.database

import androidx.room.*
import com.crisiscleanup.core.database.dao.*
import com.crisiscleanup.core.database.model.*
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
        """
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
        """
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
}

@Dao
interface TestFlagDao {
    @Transaction
    @Query("SELECT * FROM worksite_flags WHERE worksite_id=:worksiteId ORDER BY id")
    fun getEntities(worksiteId: Long): List<WorksiteFlagEntity>

    @Transaction
    @Query(
        """
        UPDATE worksite_flags
        SET network_id=:networkId,
            local_global_uuid=:localGlobalUuid
        WHERE id=:id
        """
    )
    fun updateNetworkId(id: Long, networkId: Long, localGlobalUuid: String = "")
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
        """
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
        """
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
        """
    )
    fun getEntities(worksiteId: Long): List<WorksiteChangeEntity>

    @Transaction
    @Query(
        """
        SELECT *
        FROM worksite_changes
        WHERE worksite_id=:worksiteId
        """
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
        """
    )
    fun getEntities(worksiteId: Long): List<WorkTypeTransferRequestEntity>
}
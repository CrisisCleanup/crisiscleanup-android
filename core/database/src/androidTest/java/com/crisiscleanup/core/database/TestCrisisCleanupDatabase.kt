package com.crisiscleanup.core.database

import androidx.room.*
import com.crisiscleanup.core.database.dao.*
import com.crisiscleanup.core.database.model.*
import com.crisiscleanup.core.database.util.InstantConverter
import kotlinx.datetime.Instant

@Database(
    entities = [
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
    ],
    version = 1,
)
@TypeConverters(
    InstantConverter::class,
)
abstract class TestCrisisCleanupDatabase : CrisisCleanupDatabase() {
    abstract fun testIncidentDao(): TestIncidentDao
    abstract fun testWorksiteDao(): TestWorksiteDao
    abstract fun testWorkTypeDao(): TestWorkTypeDao
    abstract fun testWorksiteFlagDao(): TestWorksiteFlagDao
    abstract fun testWorksiteNoteDao(): TestWorksiteNoteDao
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
    fun getWorksiteWorkTypes(worksiteId: Long): List<WorkTypeEntity>

    @Insert
    fun insertWorkTypes(workTypes: Collection<WorkTypeEntity>)
}

@Dao
interface TestWorksiteFlagDao {
    @Insert
    fun insertFlags(flags: Collection<WorksiteFlagEntity>)
}

@Dao
interface TestWorksiteNoteDao {
    @Insert
    fun insertNotes(notes: Collection<WorksiteNoteEntity>)
}
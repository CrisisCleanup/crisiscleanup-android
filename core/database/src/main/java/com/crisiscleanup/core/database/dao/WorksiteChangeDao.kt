package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksiteChange
import com.crisiscleanup.core.database.model.WorksiteChangeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Dao
interface WorksiteChangeDao {
    @Insert
    fun insert(change: WorksiteChangeEntity)

    @Transaction
    @Query("SELECT * FROM worksite_changes WHERE worksite_id=:worksiteId ORDER BY created_at ASC")
    fun getOrdered(worksiteId: Long): List<PopulatedWorksiteChange>

    @Transaction
    @Query(
        """
        UPDATE worksite_changes
        SET archive_action  =:action,
            save_attempt    =save_attempt+1,
            save_attempt_at =:savedAt
        WHERE id=:id
        """
    )
    fun updateAction(id: Long, action: String, savedAt: Instant = Clock.System.now())

    @Transaction
    @Query(
        """
        UPDATE worksite_changes
        SET save_attempt    =save_attempt+1,
            save_attempt_at =:savedAt
        WHERE id=:id
        """
    )
    fun updateSyncAttempt(id: Long, savedAt: Instant = Clock.System.now())

    @Transaction
    @Query("DELETE FROM worksite_changes WHERE id IN(:ids)")
    fun delete(ids: Collection<Long>)

    @Transaction
    @Query("SELECT COUNT(id) FROM worksite_changes WHERE worksite_id=:worksiteId")
    fun getChangeCount(worksiteId: Long): Int

    @Transaction
    @Query(
        """
        SELECT *
        FROM worksites w
        INNER JOIN (SELECT DISTINCT worksite_id FROM worksite_changes ORDER BY created_at) wc
        ON w.id = wc.worksite_id
        """
    )
    fun getWorksitesPendingSync(): Flow<List<PopulatedWorksite>>
}

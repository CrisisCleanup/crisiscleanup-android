package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.WorkTypeEntity
import kotlinx.datetime.Instant

@Dao
interface WorkTypeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreWorkType(workType: WorkTypeEntity): Long

    @Transaction
    @Query(
        """
        UPDATE work_types SET
        created_at      =COALESCE(:createdAt, created_at),
        claimed_by      =:orgClaim,
        next_recur_at   =:nextRecurAt,
        phase           =:phase,
        recur           =:recur,
        status          =:status,
        work_type       =:workType
        WHERE worksite_id=:worksiteId AND network_id=:networkId AND local_global_uuid=""
        """
    )
    fun syncUpdateWorkType(
        worksiteId: Long,
        networkId: Long,
        orgClaim: Long?,
        status: String,
        workType: String,
        createdAt: Instant?,
        nextRecurAt: Instant?,
        phase: Int?,
        recur: String?,
    )

    @Transaction
    @Query(
        """
        DELETE FROM work_types
        WHERE worksite_id=:worksiteId AND network_id NOT IN(:networkIds)
        """
    )
    fun syncDeleteUnspecifiedWorkTypes(worksiteId: Long, networkIds: Collection<Long>)

}

@Dao
interface TestTargetWorkTypeDao {
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
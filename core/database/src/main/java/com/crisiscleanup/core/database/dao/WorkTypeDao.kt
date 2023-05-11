package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.PopulatedIdNetworkId
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
        network_id      =:networkId,
        next_recur_at   =:nextRecurAt,
        phase           =:phase,
        recur           =:recur,
        status          =:status
        WHERE worksite_id=:worksiteId AND work_type=:workType
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
    fun syncDeleteUnspecified(worksiteId: Long, networkIds: Collection<Long>)

    @Transaction
    @Query("DELETE FROM work_types WHERE worksite_id=:worksiteId AND work_type NOT IN(:workTypes)")
    fun deleteUnspecified(worksiteId: Long, workTypes: Collection<String>)

    @Transaction
    @Query("DELETE FROM work_types WHERE worksite_id=:worksiteId AND work_type IN(:workTypes)")
    fun deleteSpecified(worksiteId: Long, workTypes: Set<String>)

    @Transaction
    @Query(
        """
        SELECT id, network_id
        FROM work_types
        WHERE worksite_id=:worksiteId AND network_id>-1
        """
    )
    fun getNetworkedIdMap(worksiteId: Long): List<PopulatedIdNetworkId>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(workTypes: Collection<WorkTypeEntity>): List<Long>

    @Update
    fun update(workTypes: Collection<WorkTypeEntity>)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE work_types
        SET network_id =:networkId
        WHERE id=:id
        """
    )
    fun updateNetworkId(id: Long, networkId: Long)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE work_types
        SET network_id =:networkId
        WHERE worksite_id=:worksiteId AND work_type=:workType
        """
    )
    fun updateNetworkId(worksiteId: Long, workType: String, networkId: Long)

    @Transaction
    @Query("SELECT COUNT(id) FROM work_types WHERE worksite_id=:worksiteId AND network_id<=0")
    fun getUnsyncedCount(worksiteId: Long): Int

    @Transaction
    @Query("SELECT DISTINCT work_type FROM work_types WHERE worksite_id=:worksiteId")
    fun getWorkTypes(worksiteId: Long): List<String>
}

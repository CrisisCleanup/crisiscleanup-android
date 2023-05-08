package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.WorkTypeTransferRequestEntity
import kotlinx.datetime.Instant

@Dao
interface WorkTypeTransferRequestDao {
    @Transaction
    @Query(
        """
        DELETE FROM worksite_work_type_requests
        WHERE worksite_id=:worksiteId AND work_type NOT IN(:workTypes) AND network_id>0
        """
    )
    fun deleteUnspecified(worksiteId: Long, workTypes: Set<String>)

    @Transaction
    @Query(
        """
        DELETE FROM worksite_work_type_requests
        WHERE worksite_id=:worksiteId AND network_id<=0
        """
    )
    fun deleteUnsynced(worksiteId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreRequest(request: WorkTypeTransferRequestEntity): Long

    @Transaction
    @Query(
        """
        UPDATE worksite_work_type_requests
        SET
        network_id  =:networkId,
        to_org      =:toOrg,
        created_at  =:createdAt,
        approved_at =:approvedAt,
        rejected_at =:rejectedAt,
        approved_rejected_reason=:approvedRejectedReason
        WHERE worksite_id=:worksiteId AND work_type=:workType AND by_org=:byOrg
        """
    )
    fun syncUpdateRequest(
        worksiteId: Long,
        networkId: Long,
        workType: String,
        byOrg: Long,
        toOrg: Long,
        createdAt: Instant,
        approvedAt: Instant?,
        rejectedAt: Instant?,
        approvedRejectedReason: String,
    )
}
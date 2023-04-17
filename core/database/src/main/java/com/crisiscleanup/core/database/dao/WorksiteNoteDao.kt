package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.PopulatedIdNetworkId
import com.crisiscleanup.core.database.model.WorksiteNoteEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours

@Dao
interface WorksiteNoteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnoreNote(note: WorksiteNoteEntity): Long

    @Transaction
    @Query(
        """
        UPDATE worksite_notes SET
        created_at  =:createdAt,
        is_survivor =:isSurvivor,
        note        =:note
        WHERE worksite_id=:worksiteId AND network_id=:networkId AND local_global_uuid=''
        """
    )
    fun syncUpdateNote(
        worksiteId: Long,
        networkId: Long,
        createdAt: Instant,
        isSurvivor: Boolean,
        note: String,
    )

    @Transaction
    @Query(
        """
        DELETE FROM worksite_notes
        WHERE worksite_id=:worksiteId AND network_id NOT IN(:networkIds)
        """
    )
    fun syncDeleteUnspecified(worksiteId: Long, networkIds: Collection<Long>)

    @Transaction
    @Query(
        """
        SELECT id, network_id
        FROM worksite_notes
        WHERE worksite_id=:worksiteId AND network_id>-1
        """
    )
    fun getNetworkedIdMap(worksiteId: Long): List<PopulatedIdNetworkId>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertIgnore(notes: Collection<WorksiteNoteEntity>)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE worksite_notes
        SET network_id          =:networkId,
            local_global_uuid   =''
        WHERE id=:id
        """
    )
    fun updateNetworkId(id: Long, networkId: Long)

    @Transaction
    @Query("SELECT COUNT(id) FROM worksite_notes WHERE worksite_id=:worksiteId AND network_id<=0")
    fun getUnsyncedCount(worksiteId: Long): Int

    @Transaction
    @Query("SELECT note FROM worksite_notes WHERE worksite_id=:worksiteId AND created_at>:createdAt")
    fun getNotes(
        worksiteId: Long,
        createdAt: Instant = Clock.System.now().minus(12.hours),
    ): List<String>
}

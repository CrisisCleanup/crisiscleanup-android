package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.CaseHistoryEventAttrEntity
import com.crisiscleanup.core.database.model.CaseHistoryEventEntity
import com.crisiscleanup.core.database.model.PopulatedCaseHistoryEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseHistoryDao {
    @Transaction
    @Query("DELETE FROM case_history_events WHERE worksite_id=:worksiteId AND id NOT IN(:ids)")
    fun deleteUnspecified(worksiteId: Long, ids: Collection<Long>)

    @Upsert
    fun upsertEvents(events: Collection<CaseHistoryEventEntity>)

    @Upsert
    fun upsertAttrs(eventAttrs: Collection<CaseHistoryEventAttrEntity>)

    @Transaction
    @Query(
        """
        SELECT *
        FROM case_history_events
        WHERE worksite_id=:worksiteId
        ORDER BY created_by, created_at
        """,
    )
    fun streamEvents(worksiteId: Long): Flow<List<PopulatedCaseHistoryEvent>>
}
package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.PopulatedRecentWorksite
import com.crisiscleanup.core.database.model.RecentWorksiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentWorksiteDao {
    @Upsert
    fun upsert(recentWorksite: RecentWorksiteEntity)

    @Transaction
    @Query(
        """
        SELECT *
        FROM recent_worksites
        WHERE incident_id=:incidentId
        ORDER BY viewed_at DESC
        LIMIT :limit
        OFFSET :offset
        """,
    )
    fun streamRecentWorksites(
        incidentId: Long,
        limit: Int = 16,
        offset: Int = 0,
    ): Flow<List<PopulatedRecentWorksite>>
}

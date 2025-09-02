package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.PopulatedRecentWorksite
import com.crisiscleanup.core.database.model.RecentWorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentWorksiteDao {
    @Transaction
    @Query(
        """
        SELECT r.*
        FROM recent_worksites r
        INNER JOIN worksites w ON r.id=w.id
        WHERE r.incident_id=:incidentId AND w.incident_id=:incidentId
        ORDER BY viewed_at DESC
        LIMIT :limit
        OFFSET :offset
        """,
    )
    fun streamRecentWorksites(
        incidentId: Long,
        limit: Int = 30,
        offset: Int = 0,
    ): Flow<List<PopulatedRecentWorksite>>

    @Transaction
    @Query(
        """
        SELECT w.*
        FROM recent_worksites r
        INNER JOIN worksites w ON r.id=w.id
        WHERE r.incident_id=:incidentId AND w.incident_id=:incidentId
        ORDER BY viewed_at DESC
        LIMIT :limit
        """,
    )
    fun getRecentWorksiteCoordinates(
        incidentId: Long,
        limit: Int = 3,
    ): List<WorksiteEntity>

    @Upsert
    fun upsert(recentWorksite: RecentWorksiteEntity)
}

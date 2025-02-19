package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.IncidentDataSyncParametersEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface IncidentDataSyncParameterDao {
    @Transaction
    @Query("SELECT * FROM incident_data_sync_parameters WHERE id=:id")
    fun streamWorksitesSyncStats(id: Long): Flow<IncidentDataSyncParametersEntity?>

    @Transaction
    @Query(
        """
        SELECT *
        FROM incident_data_sync_parameters
        WHERE id==:incidentId
        """,
    )
    fun getSyncStats(incidentId: Long): IncidentDataSyncParametersEntity?

    @Insert
    fun insertSyncStats(entity: IncidentDataSyncParametersEntity)

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE incident_data_sync_parameters
        SET updated_before=:updatedBefore
        WHERE id=:incidentId
        """,
    )
    fun updateUpdatedBefore(
        incidentId: Long,
        updatedBefore: Instant,
    )

    @Transaction
    @Query(
        """
        UPDATE OR IGNORE incident_data_sync_parameters
        SET updated_after=:updatedAfter
        WHERE id=:incidentId
        """,
    )
    fun updateUpdatedAfter(
        incidentId: Long,
        updatedAfter: Instant,
    )

    @Transaction
    @Query("DELETE FROM incident_data_sync_parameters WHERE id=:id")
    fun deleteSyncStats(id: Long)
}

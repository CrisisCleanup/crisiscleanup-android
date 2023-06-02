package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.model.IncidentEntity
import com.crisiscleanup.core.database.model.IncidentFormFieldEntity
import com.crisiscleanup.core.database.model.IncidentIncidentLocationCrossRef
import com.crisiscleanup.core.database.model.IncidentLocationEntity
import com.crisiscleanup.core.database.model.PopulatedFormFieldsIncident
import com.crisiscleanup.core.database.model.PopulatedIncident
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    // Update IncidentDaoTest in conjunction with below

    @Transaction
    @Query("SELECT COUNT(*) FROM incidents")
    fun getIncidentCount(): Int

    @Transaction
    @Query(
        """
        SELECT *
        FROM incidents
        WHERE is_archived==0
        ORDER BY start_at DESC, id DESC
        """
    )
    fun streamIncidents(): Flow<List<PopulatedIncident>>

    @Transaction
    @Query("SELECT * FROM incidents WHERE id=:id")
    fun getIncident(id: Long): PopulatedIncident?

    @Transaction
    @Query("SELECT * FROM incidents WHERE start_at>:startAtMillis ORDER BY start_at DESC")
    fun getIncidents(startAtMillis: Long): List<PopulatedIncident>

    @Transaction
    @Query("SELECT * FROM incidents WHERE id=:id")
    fun getFormFieldsIncident(id: Long): PopulatedFormFieldsIncident?

    @Transaction
    @Query("SELECT * FROM incidents WHERE id=:id")
    fun streamFormFieldsIncident(id: Long): Flow<PopulatedFormFieldsIncident?>

    @Upsert
    suspend fun upsertIncidents(incidents: Collection<IncidentEntity>)

    @Upsert
    suspend fun upsertIncidentLocations(locations: Collection<IncidentLocationEntity>)

    @Transaction
    @Query(
        """
        DELETE FROM incident_to_incident_location
        WHERE incident_id IN (:incidentIds)
        """
    )
    suspend fun deleteIncidentLocationCrossRefs(incidentIds: Collection<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoreIncidentLocationCrossRefs(
        incidentCrossRefs: Collection<IncidentIncidentLocationCrossRef>
    )

    @Transaction
    @Query(
        """
        UPDATE incident_form_fields
        SET is_invalidated=1
        WHERE incident_id=:incidentId
        """
    )
    suspend fun invalidateFormFields(incidentId: Long)

    @Upsert
    suspend fun upsertFormFields(formFields: Collection<IncidentFormFieldEntity>)
}

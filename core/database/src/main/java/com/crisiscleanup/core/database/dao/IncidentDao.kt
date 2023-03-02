package com.crisiscleanup.core.database.dao

import androidx.room.*
import com.crisiscleanup.core.database.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {

    // Update IncidentDaoTest in conjunction with below

    @Transaction
    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun getIncidentCount(): Int

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
    suspend fun getIncident(id: Long): List<PopulatedIncident>

    @Transaction
    @Query("SELECT * FROM incidents WHERE id=:id")
    suspend fun getFormFieldsIncident(id: Long): PopulatedFormFieldsIncident?

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

@Dao
interface TestTargetIncidentDao {
    @Transaction
    @Query(
        """
        UPDATE incidents
        SET is_archived=1
        WHERE id NOT IN(:unarchivedIds)
        """
    )
    suspend fun setExcludedArchived(unarchivedIds: Set<Long>)
}
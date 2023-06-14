package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.crisiscleanup.core.database.dao.fts.PopulatedIncidentIdNameMatchInfo
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

    @Transaction
    @Query("SELECT name FROM incidents ORDER BY RANDOM() LIMIT 1")
    fun getRandomIncidentName(): String?

    @Transaction
    @Query("INSERT INTO incident_fts(incident_fts) VALUES ('rebuild')")
    fun rebuildIncidentFts()

    @Transaction
    @Query(
        """
        SELECT i.id, i.name, i.short_name, i.incident_type,
        matchinfo(incident_fts, 'pcnalx') as match_info
        FROM incident_fts f
        INNER JOIN incidents i ON f.name=i.name
        WHERE incident_fts MATCH :query
        """
    )
    fun matchIncidentTokens(query: String): List<PopulatedIncidentIdNameMatchInfo>
}

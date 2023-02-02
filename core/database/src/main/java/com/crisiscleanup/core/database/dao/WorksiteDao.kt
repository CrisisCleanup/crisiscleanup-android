package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteLocalModifiedAt
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

@Dao
interface WorksiteDao {
    @Transaction
    @Query(
        """
    SELECT *
    FROM worksites
    WHERE incident_id=:incidentId
    """
    )
    fun getWorksites(incidentId: Long): Flow<List<PopulatedWorksite>>

    @Transaction
    @Query(
        """
    SELECT id, latitude, longitude, key_work_type_type
    FROM worksites
    WHERE incident_id=:incidentId
    """
    )
    fun getWorksitesMapVisual(incidentId: Long): Flow<List<PopulatedWorksiteMapVisual>>

    @Transaction
    @Query(
        """
    SELECT network_id, local_modified_at
    FROM worksites
    WHERE incident_id=:incidentId AND network_id IN (:worksiteIds)
    """
    )
    fun getWorksitesLocalModifiedAt(
        incidentId: Long,
        worksiteIds: Set<Long>,
    ): Flow<List<WorksiteLocalModifiedAt>>

    @Query("SELECT COUNT(id) FROM worksites WHERE incident_id=:incidentId")
    fun getWorksitesCount(incidentId: Long): Flow<Int>

    @Insert
    fun insertWorksite(worksite: WorksiteEntity)

    @Query(
        """
        UPDATE worksites
        SET
        synced_at=:syncedAt,
        sync_attempt=0,
        is_local_modified=0,
        incident_id=:incidentId,
        address=:address,
        auto_contact_frequency_t=:autoContactFrequencyT,
        case_number=:caseNumber,
        city=:city,
        county=:county,
        created_at=COALESCE(:createdAt, created_at),
        email=:email,
        favorite_id=:favoriteId,
        key_work_type_type=:keyWorkTypeType,
        latitude=:latitude,
        longitude=:longitude,
        name=:name,
        phone1=:phone1,
        phone2=:phone2,
        plus_code=:plusCode,
        postal_code=:postalCode,
        reported_by=:reportedBy,
        state=:state,
        svi=:svi,
        what3Words=:what3Words,
        updated_at=:updatedAt
        WHERE network_id=:networkId AND local_global_uuid="" AND local_modified_at = :expectedLocalModifiedAt
        """
    )
    fun updateSyncWorksite(
        expectedLocalModifiedAt: Instant,
        syncedAt: Instant,
        networkId: Long,
        incidentId: Long,
        address: String,
        autoContactFrequencyT: String,
        caseNumber: String,
        city: String,
        county: String,
        createdAt: Instant?,
        email: String?,
        favoriteId: Long?,
        keyWorkTypeType: String,
        latitude: Float,
        longitude: Float,
        name: String,
        phone1: String,
        phone2: String?,
        plusCode: String?,
        postalCode: String,
        reportedBy: Long?,
        state: String,
        svi: Float?,
        what3Words: String?,
        updatedAt: Instant,
    )
}
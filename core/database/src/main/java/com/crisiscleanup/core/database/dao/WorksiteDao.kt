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
    ORDER BY updated_at DESC, id DESC
    LIMIT :limit
    OFFSET :offset
    """
    )
    fun getWorksites(incidentId: Long, limit: Int, offset: Int = 0): Flow<List<PopulatedWorksite>>

    // TODO Implement spatial indexes for coordinates search
    @Transaction
    @Query(
        """
    SELECT id, latitude, longitude, key_work_type_type
    FROM worksites
    WHERE incident_id=:incidentId AND
    (latitude BETWEEN :latitudeMin AND :latitudeMax) AND
    (longitude BETWEEN :longitudeLeft AND :longitudeRight)
    LIMIT :limit
    OFFSET :offset
    """
    )
    fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeMin: Double,
        latitudeMax: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<PopulatedWorksiteMapVisual>>

    @Transaction
    @Query(
        """
    SELECT id, latitude, longitude, key_work_type_type
    FROM worksites
    WHERE incident_id=:incidentId AND
    (latitude BETWEEN :latitudeMin AND :latitudeMax) AND
    (longitude>=:longitudeLeft OR longitude<=:longitudeRight)
    LIMIT :limit
    OFFSET :offset
    """
    )
    fun getWorksitesMapVisualLongitudeCrossover(
        incidentId: Long,
        latitudeMin: Double,
        latitudeMax: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<PopulatedWorksiteMapVisual>>

    @Transaction
    @Query(
        """
    SELECT network_id, local_modified_at
    FROM worksites_root
    WHERE incident_id=:incidentId AND network_id IN (:worksiteIds)
    """
    )
    fun getWorksitesLocalModifiedAt(
        incidentId: Long,
        worksiteIds: Set<Long>,
    ): Flow<List<WorksiteLocalModifiedAt>>

    @Transaction
    @Query("SELECT COUNT(id) FROM worksites_root WHERE incident_id=:incidentId")
    fun getWorksitesCount(incidentId: Long): Int

    @Transaction
    @Query(
        """
        INSERT OR ROLLBACK INTO worksites_root (
            synced_at, 
            network_id, 
            incident_id
        )
        VALUES (
            :syncedAt,
            :networkId,
            :incidentId
        )
    """
    )
    fun insertWorksiteRoot(
        syncedAt: Instant,
        networkId: Long,
        incidentId: Long,
    ): Long

    @Insert
    fun insertWorksite(worksite: WorksiteEntity)

    @Transaction
    @Query(
        """
        UPDATE OR ROLLBACK worksites_root
        SET
        synced_at=:syncedAt,
        sync_attempt=0,
        is_local_modified=0,
        incident_id=:incidentId
        WHERE network_id=:networkId AND local_global_uuid='' AND local_modified_at = :expectedLocalModifiedAt
        """
    )
    fun updateSyncWorksiteRoot(
        expectedLocalModifiedAt: Instant,
        syncedAt: Instant,
        networkId: Long,
        incidentId: Long,
    )

    @Transaction
    @Query(
        """
        UPDATE OR ROLLBACK worksites
        SET
        incident_id     =:incidentId,
        address         =:address,
        auto_contact_frequency_t=COALESCE(:autoContactFrequencyT,auto_contact_frequency_t),
        case_number     =:caseNumber,
        city            =:city,
        county          =:county,
        created_at      =COALESCE(:createdAt, created_at),
        email           =COALESCE(:email, email),
        favorite_id     =:favoriteId,
        key_work_type_type=:keyWorkTypeType,
        latitude        =:latitude,
        longitude       =:longitude,
        name            =:name,
        phone1          =COALESCE(:phone1, phone1),
        phone2          =COALESCE(:phone2, phone2),
        plus_code       =COALESCE(:plusCode, plus_code),
        postal_code     =:postalCode,
        reported_by     =COALESCE(:reportedBy, reported_by),
        state           =:state,
        svi             =:svi,
        what3Words      =COALESCE(:what3Words, what3Words),
        updated_at      =:updatedAt
        WHERE id=(SELECT id from worksites_root WHERE network_id=:networkId AND local_global_uuid='' AND local_modified_at = :expectedLocalModifiedAt)
        """
    )
    fun updateSyncWorksite(
        expectedLocalModifiedAt: Instant,
        networkId: Long,
        incidentId: Long,
        address: String,
        autoContactFrequencyT: String?,
        caseNumber: String,
        city: String,
        county: String,
        createdAt: Instant?,
        email: String?,
        favoriteId: Long?,
        keyWorkTypeType: String,
        latitude: Double,
        longitude: Double,
        name: String,
        phone1: String?,
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
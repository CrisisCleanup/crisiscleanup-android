package com.crisiscleanup.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.crisiscleanup.core.database.model.BoundedSyncedWorksiteIds
import com.crisiscleanup.core.database.model.PopulatedLocalWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksite
import com.crisiscleanup.core.database.model.PopulatedWorksiteMapVisual
import com.crisiscleanup.core.database.model.WorksiteEntity
import com.crisiscleanup.core.database.model.WorksiteLocalModifiedAt
import com.crisiscleanup.core.database.model.WorksiteRootEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

private const val selectFromMapVisualClause = """
        SELECT w.id, latitude, longitude, key_work_type_type, key_work_type_org, key_work_type_status, COUNT(wt.id) as work_type_count, favorite_id
        FROM worksites w LEFT JOIN work_types wt ON w.id=wt.worksite_id
        """

@Dao
interface WorksiteDao {
    @Transaction
    @Query("SELECT id FROM worksites_root WHERE network_id=:networkId AND local_global_uuid=''")
    fun getWorksiteId(networkId: Long): Long

    @Transaction
    @Query("SELECT network_id FROM worksites_root WHERE id=:id")
    fun getWorksiteNetworkId(id: Long): Long

    @Transaction
    @Query("SELECT incident_id FROM worksites_root WHERE id=:id")
    fun getIncidentId(id: Long): Long

    @Transaction
    @Query("SELECT * FROM worksites WHERE id=:id")
    fun getWorksite(id: Long): PopulatedWorksite

    @Transaction
    @Query("SELECT * FROM worksites WHERE id=:id")
    fun streamLocalWorksite(id: Long): Flow<PopulatedLocalWorksite?>

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
    fun streamWorksites(
        incidentId: Long,
        limit: Int,
        offset: Int = 0
    ): Flow<List<PopulatedWorksite>>

    @Transaction
    @Query(
        """
        $selectFromMapVisualClause
        WHERE incident_id=:incidentId AND
              (longitude BETWEEN :longitudeWest AND :longitudeEast) AND
              (latitude BETWEEN :latitudeSouth AND :latitudeNorth)
        GROUP BY w.id
        ORDER BY updated_at DESC, w.id DESC
        LIMIT :limit
        OFFSET :offset
        """
    )
    fun streamWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<PopulatedWorksiteMapVisual>>

    @Transaction
    @Query(
        """
        $selectFromMapVisualClause
        WHERE incident_id=:incidentId AND
              (longitude>=:longitudeLeft OR longitude<=:longitudeRight) AND
              (latitude BETWEEN :latitudeSouth AND :latitudeNorth)
        GROUP BY w.id
        ORDER BY updated_at DESC, w.id DESC
        LIMIT :limit
        OFFSET :offset
        """
    )
    fun streamWorksitesMapVisualLongitudeCrossover(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
        limit: Int,
        offset: Int,
    ): Flow<List<PopulatedWorksiteMapVisual>>

    @Transaction
    @Query(
        """
        SELECT id, network_id, local_modified_at, is_local_modified
        FROM worksites_root
        WHERE network_id IN (:worksiteIds)
        """
    )
    fun getWorksitesLocalModifiedAt(
        worksiteIds: Collection<Long>,
    ): List<WorksiteLocalModifiedAt>

    @Transaction
    @Query(
        """
        $selectFromMapVisualClause
        WHERE incident_id=:incidentId
        GROUP BY w.id
        ORDER BY updated_at DESC, w.id DESC
        LIMIT :limit
        OFFSET :offset
        """
    )
    fun getWorksitesMapVisual(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ): List<PopulatedWorksiteMapVisual>

    @Transaction
    @Query(
        """
        $selectFromMapVisualClause
        WHERE incident_id=:incidentId AND
              (longitude BETWEEN :longitudeWest AND :longitudeEast) AND
              (latitude BETWEEN :latitudeSouth AND :latitudeNorth)
        GROUP BY w.id
        ORDER BY updated_at DESC, w.id DESC
        LIMIT :limit
        OFFSET :offset
        """
    )
    fun getWorksitesMapVisual(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
        limit: Int,
        offset: Int,
    ): List<PopulatedWorksiteMapVisual>

    @Transaction
    @Query("SELECT COUNT(id) FROM worksites_root WHERE incident_id=:incidentId")
    fun getWorksitesCount(incidentId: Long): Int

    @Transaction
    @Query(
        """
        SELECT COUNT(id)
        FROM worksites
        WHERE incident_id=:incidentId AND
              (longitude BETWEEN :longitudeWest AND :longitudeEast) AND
              (latitude BETWEEN :latitudeSouth AND :latitudeNorth)
        """
    )
    fun getWorksitesCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
    ): Int

    @Transaction
    @Query(
        """
        SELECT COUNT(id)
        FROM worksites
        WHERE incident_id=:incidentId AND
              (longitude>=:longitudeLeft OR longitude<=:longitudeRight) AND
              (latitude BETWEEN :latitudeSouth AND :latitudeNorth)
        """
    )
    fun getWorksitesCountLongitudeCrossover(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeLeft: Double,
        longitudeRight: Double,
    ): Int

    @Transaction
    @Query("SELECT COUNT(id) FROM worksites_root WHERE incident_id=:incidentId")
    fun streamWorksitesCount(incidentId: Long): Flow<Int>

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
    fun insertOrRollbackWorksiteRoot(
        syncedAt: Instant,
        networkId: Long,
        incidentId: Long,
    ): Long

    @Insert
    fun insertRoot(worksiteRoot: WorksiteRootEntity): Long

    @Transaction
    @Query(
        """
        UPDATE worksites_root
        SET sync_uuid           =:syncUuid,
            local_modified_at   =:localModifiedAt,
            is_local_modified   =1
        WHERE id=:id
        """
    )
    fun updateRoot(
        id: Long,
        syncUuid: String,
        localModifiedAt: Instant,
    )

    @Insert
    fun insert(worksite: WorksiteEntity)

    @Update
    fun update(worksite: WorksiteEntity)

    @Transaction
    @Query(
        """
        UPDATE OR ROLLBACK worksites_root
        SET synced_at=:syncedAt,
            sync_attempt=0,
            is_local_modified=0,
            incident_id=:incidentId
        WHERE id=:id AND
              network_id=:networkId AND
              local_modified_at=:expectedLocalModifiedAt
        """
    )
    fun syncUpdateWorksiteRoot(
        id: Long,
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
        key_work_type_type  =CASE WHEN :keyWorkTypeType=='' THEN key_work_type_type ELSE :keyWorkTypeType END,
        key_work_type_org   =CASE WHEN :keyWorkTypeOrgClaim<0 THEN key_work_type_org ELSE :keyWorkTypeOrgClaim END,
        key_work_type_status=CASE WHEN :keyWorkTypeStatus=='' THEN key_work_type_status ELSE :keyWorkTypeStatus END,
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
        WHERE id=:id AND network_id=:networkId
        """
    )
    fun syncUpdateWorksite(
        id: Long,
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
        keyWorkTypeOrgClaim: Long?,
        keyWorkTypeStatus: String,
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

    @Transaction
    @Query(
        """
        UPDATE OR ROLLBACK worksites
        SET
        auto_contact_frequency_t=COALESCE(auto_contact_frequency_t, :autoContactFrequencyT),
        case_number =CASE WHEN LENGTH(case_number)==0 THEN :caseNumber ELSE case_number END,
        email       =COALESCE(email, :email),
        favorite_id =COALESCE(favorite_id, :favoriteId),
        phone1      =CASE WHEN LENGTH(COALESCE(phone1,''))<2 THEN :phone1 ELSE phone1 END,
        phone2      =COALESCE(phone2, :phone2),
        plus_code   =COALESCE(plus_code, :plusCode),
        svi         =COALESCE(svi, :svi),
        reported_by =COALESCE(reported_by, :reportedBy),
        what3Words  =COALESCE(what3Words, :what3Words)
        WHERE id=:id
        """
    )
    fun syncFillWorksite(
        id: Long,
        autoContactFrequencyT: String?,
        caseNumber: String,
        email: String?,
        favoriteId: Long?,
        phone1: String?,
        phone2: String?,
        plusCode: String?,
        svi: Float?,
        reportedBy: Long?,
        what3Words: String?,
    )

    @Transaction
    @Query(
        """
        SELECT id
        FROM worksites_root
        WHERE is_local_modified<>0
        ORDER BY local_modified_at DESC
        LIMIT :limit
        """
    )
    fun getLocallyModifiedWorksites(limit: Int): List<Long>

    @Transaction
    @Query(
        """
        UPDATE worksites_root
        SET network_id=:networkId,
            local_global_uuid=''
        WHERE id=:id;
        """
    )
    fun updateRootNetworkId(id: Long, networkId: Long)

    @Transaction
    @Query("UPDATE worksites SET network_id=:networkId WHERE id=:id;")
    fun updateWorksiteNetworkId(id: Long, networkId: Long)

    @Transaction
    @Query(
        """
        UPDATE worksites_root
        SET synced_at           =:syncedAt,
            is_local_modified   =0,
            sync_attempt        =0
        WHERE id=:worksiteId
        """
    )
    fun setRootUnmodified(worksiteId: Long, syncedAt: Instant)

    @Transaction
    @Query(
        """
        SELECT COUNT(id)
        FROM worksites
        WHERE incident_id=:incidentId AND
              (longitude BETWEEN :longitudeWest AND :longitudeEast) AND
              (latitude BETWEEN :latitudeSouth AND :latitudeNorth)
        """
    )
    fun getBoundedWorksiteCount(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
    ): Int

    @Transaction
    @Query(
        """
        SELECT w.id, w.network_id, r.synced_at, phone1
        FROM worksites w INNER JOIN worksites_root r ON w.id=r.id
        WHERE w.incident_id=:incidentId AND
              (longitude BETWEEN :longitudeWest AND :longitudeEast) AND
              (latitude BETWEEN :latitudeSouth AND :latitudeNorth)
        """
    )
    fun getBoundedSyncedWorksiteIds(
        incidentId: Long,
        latitudeSouth: Double,
        latitudeNorth: Double,
        longitudeWest: Double,
        longitudeEast: Double,
    ): List<BoundedSyncedWorksiteIds>
}

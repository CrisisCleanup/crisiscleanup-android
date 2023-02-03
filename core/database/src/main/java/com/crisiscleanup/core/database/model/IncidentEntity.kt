package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.IncidentLocation
import kotlinx.datetime.Instant

@Entity(
    "incidents",
    [
        Index(
            value = ["start_at"],
            orders = [Index.Order.DESC],
            name = "idx_newest_to_oldest_incidents",
        )
    ]
)
data class IncidentEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("start_at")
    val startAt: Instant,
    val name: String,
    @ColumnInfo("short_name", defaultValue = "")
    val shortName: String,
//    @ColumnInfo("active_phone_number", defaultValue = "")
//    val activePhoneNumber: String? = null,
    @ColumnInfo("is_archived", defaultValue = "0")
    val isArchived: Boolean = false,
)

@Entity("incident_locations")
data class IncidentLocationEntity(
    @PrimaryKey
    val id: Long,
    val location: Long,
)

fun IncidentLocationEntity.asExternalModel() = IncidentLocation(
    id = id,
    location = location,
)

@Entity(
    "incident_to_incident_location",
    primaryKeys = ["incident_id", "incident_location_id"],
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IncidentLocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["incident_location_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["incident_location_id", "incident_id"],
            name = "idx_incident_location_to_incident",
        ),
    ]
)
data class IncidentIncidentLocationCrossRef(
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("incident_location_id")
    val incidentLocationId: Long,
)

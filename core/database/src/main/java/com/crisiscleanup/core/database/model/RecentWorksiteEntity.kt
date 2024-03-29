package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Index.Order
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(
    "recent_worksites",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            value = ["incident_id", "viewed_at"],
            orders = [Order.ASC, Order.DESC],
        ),
        Index(
            value = ["viewed_at"],
            orders = [Order.DESC],
        ),
    ],
)
data class RecentWorksiteEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("incident_id")
    val incidentId: Long,
    @ColumnInfo("viewed_at")
    val viewedAt: Instant,
)

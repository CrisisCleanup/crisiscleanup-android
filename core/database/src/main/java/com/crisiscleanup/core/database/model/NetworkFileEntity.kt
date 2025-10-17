package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.NetworkImage
import kotlin.time.Instant

@Entity(
    "network_files",
)
data class NetworkFileEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("created_at")
    val createdAt: Instant,
    @ColumnInfo("file_id", defaultValue = "0")
    val fileId: Long,
    @ColumnInfo("file_type_t")
    val fileTypeT: String,
    @ColumnInfo("full_url")
    val fullUrl: String?,
    @ColumnInfo("large_thumbnail_url")
    val largeThumbnailUrl: String?,
    @ColumnInfo("mime_content_type")
    val mimeContentType: String,
    @ColumnInfo("small_thumbnail_url")
    val smallThumbnailUrl: String?,
    val tag: String?,
    val title: String?,
    val url: String,
)

@Entity(
    "worksite_to_network_file",
    primaryKeys = ["worksite_id", "network_file_id"],
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NetworkFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["network_file_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["network_file_id", "worksite_id"]),
    ],
)
data class WorksiteNetworkFileCrossRef(
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    // NetworkFile.id not NetworkFile.fileId
    @ColumnInfo("network_file_id")
    val networkFileId: Long,
)

fun NetworkFileEntity.asImageModel(rotateDegrees: Int) = NetworkImage(
    id = id,
    createdAt = createdAt,
    title = title ?: "",
    thumbnailUrl = smallThumbnailUrl ?: "",
    imageUrl = fullUrl ?: "",
    tag = tag ?: "",
    rotateDegrees = rotateDegrees,
)

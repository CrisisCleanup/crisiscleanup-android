package com.crisiscleanup.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.crisiscleanup.core.model.data.WorksiteLocalImage

@Entity(
    "worksite_local_images",
    foreignKeys = [
        ForeignKey(
            entity = WorksiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["worksite_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["worksite_id", "local_document_id"], unique = true),
    ],
)
data class WorksiteLocalImageEntity(
    @PrimaryKey(true)
    val id: Long,
    @ColumnInfo("worksite_id")
    val worksiteId: Long,
    @ColumnInfo("local_document_id")
    val documentId: String,
    val uri: String,
    val tag: String,
    @ColumnInfo("rotate_degrees")
    val rotateDegrees: Int = 0,
)

fun WorksiteLocalImageEntity.asExternalModel() = WorksiteLocalImage(
    id = id,
    worksiteId = worksiteId,
    documentId = documentId,
    uri = uri,
    tag = tag,
    rotateDegrees = rotateDegrees,
)

data class PopulatedWorksiteImageCount(
    @ColumnInfo("worksite_id")
    val id: Long,
    val count: Int,
)

package com.crisiscleanup.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CmsApiResult(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<CmsResultItem>,
)

@Serializable
data class CmsResultItem(
    val id: Int,
    val title: String,
    val tags: List<String>,
    val content: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("publish_at")
    val publishAt: String,
    @SerialName("list_order")
    val listOrder: Int,
    val thumbnail: Int?,
    @SerialName("thumbnail_file")
    val thumbnailFile: ThumbnailFile?,
)

@Serializable
data class ThumbnailFile(
    val id: Int,
    val filename: String,
    val title: String?,
    val notes: String?,
    val attr: String?,
    val url: String,
    @SerialName("full_url")
    val fullUrl: String,
    @SerialName("blog_url")
    val blogUrl: String,
    @SerialName("csv_url")
    val csvUrl: String,
    @SerialName("filename_original")
    val filenameOriginal: String,
    @SerialName("large_thumbnail_url")
    val largeThumbnailUrl: String,
    @SerialName("small_thumbnail_url")
    val smallThumbnailUrl: String,
    @SerialName("type_t")
    val typeT: String,
    @SerialName("presigned_post_url")
    val presignedPostUrl: String?,
)

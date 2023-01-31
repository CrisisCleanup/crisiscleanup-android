package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkFile(
    val id: Long,
    @SerialName("blog_url")
    val blogUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Instant,
    val file: Int? = null,
    @SerialName("file_type_t")
    val fileTypeT: String,
    @SerialName("filename")
    val fileName: String,
    @SerialName("filename_original")
    val filenameOriginal: String,
    @SerialName("full_url")
    val fullUrl: String? = null,
    @SerialName("large_thumbnail_url")
    val largeThumbnailUrl: String? = null,
    @SerialName("mime_content_type")
    val mimeContentType: String,
    val notes: String? = null,
    @SerialName("small_thumbnail_url")
    val smallThumbnailUrl: String? = null,
    val tag: String? = null,
    val title: String? = null,
    val url: String,
)
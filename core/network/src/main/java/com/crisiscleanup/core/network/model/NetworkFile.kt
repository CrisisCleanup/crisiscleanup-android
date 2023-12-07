package com.crisiscleanup.core.network.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class NetworkFile(
    val id: Long,
    @SerialName("blog_url")
    val blogUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Instant,
    val file: Long? = null,
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
) {
    val isProfilePicture = fileTypeT == "fileTypes.user_profile_picture"
}

val List<NetworkFile>.profilePictureUrl: String?
    get() = find { it.isProfilePicture }?.largeThumbnailUrl

@Serializable
data class NetworkFilePush(
    @SerialName("file")
    val file: Long,
    @SerialName("tag")
    val tag: String? = null,
)

@Serializable
data class NetworkFileUpload(
    val id: Long,
    @SerialName("presigned_post_url")
    val uploadProperties: FileUploadProperties,
)

@Serializable
data class FileUploadProperties(
    val url: String,
    val fields: FileUploadFields,
)

@Serializable
data class FileUploadFields(
    val key: String,
    @SerialName("x-amz-algorithm")
    val algorithm: String,
    @SerialName("x-amz-credential")
    val credential: String,
    @SerialName("x-amz-date")
    val date: String,
    val policy: String,
    @SerialName("x-amz-signature")
    val signature: String,
)

internal fun FileUploadFields.asPartMap() = mapOf(
    "key" to key,
    "x-amz-algorithm" to algorithm,
    "x-amz-credential" to credential,
    "x-amz-date" to date,
    "policy" to policy,
    "x-amz-signature" to signature,
)
    .mapValues { it.value.toRequestBody() }

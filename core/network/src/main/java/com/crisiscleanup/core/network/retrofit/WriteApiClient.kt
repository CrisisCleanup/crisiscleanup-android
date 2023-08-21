package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import javax.inject.Inject

private interface DataChangeApi {
    @TokenAuthenticationHeader
    @ThrowClientErrorHeader
    @POST("worksites")
    suspend fun newWorksite(
        @Header("ccu-created-at") createdAt: Instant,
        @Header("ccu-sync-uuid") syncUuid: String,
        @Body worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull

    @TokenAuthenticationHeader
    @ThrowClientErrorHeader
    @PUT("worksites/{worksiteId}")
    suspend fun updateWorksite(
        @Header("ccu-modified-at") modifiedAt: Instant,
        @Header("ccu-sync-uuid") syncUuid: String,
        @Path("worksiteId") worksiteId: Long,
        @Body worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/favorite")
    suspend fun favorite(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body favorite: NetworkType,
    ): NetworkType

    @TokenAuthenticationHeader
    @HTTP(method = "DELETE", path = "worksites/{worksiteId}/favorite", hasBody = true)
    suspend fun unfavorite(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body favoriteId: NetworkFavoriteId,
    ): Response<Unit>

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/flags")
    suspend fun addFlag(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body flag: NetworkFlag,
    ): NetworkFlag

    @TokenAuthenticationHeader
    @HTTP(method = "DELETE", path = "worksites/{worksiteId}/flags", hasBody = true)
    suspend fun deleteFlag(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body flagId: NetworkFlagId,
    ): Response<Unit>

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/notes")
    suspend fun addNote(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body note: NetworkNoteNote,
    ): NetworkNote

    @TokenAuthenticationHeader
    @PATCH("worksite_work_types/{workTypeId}")
    suspend fun updateWorkTypeStatus(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("workTypeId") workTypeId: Long,
        @Body status: NetworkWorkTypeStatus,
    ): NetworkWorkType

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/claim")
    suspend fun claimWorkTypes(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body workTypes: NetworkWorkTypeTypes,
    )

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/unclaim")
    suspend fun unclaimWorkTypes(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body workTypes: NetworkWorkTypeTypes,
    )

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/request_take")
    suspend fun requestWorkTypes(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body request: NetworkWorkTypeChangeRequest,
    )

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/release")
    suspend fun releaseWorkTypes(
        @Header("ccu-created-at") createdAt: Instant,
        @Path("worksiteId") worksiteId: Long,
        @Body release: NetworkWorkTypeChangeRelease,
    )

    @TokenAuthenticationHeader
    @HTTP(method = "DELETE", path = "worksites/{worksiteId}/files", hasBody = true)
    suspend fun deleteFile(
        @Path("worksiteId") worksiteId: Long,
        @Body file: NetworkFilePush,
    ): Response<Unit>

    @FormUrlEncoded
    @TokenAuthenticationHeader
    @POST("files")
    suspend fun startFileUpload(
        @Field("filename") fileName: String,
        @Field("content_type") contentType: String,
    ): NetworkFileUpload

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/files")
    suspend fun addUploadedFile(
        @Path("worksiteId") worksiteId: Long,
        @Body file: NetworkFilePush,
    ): NetworkFile

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/share")
    suspend fun shareWorksite(
        @Path("worksiteId") worksiteId: Long,
        @Body shareDetails: NetworkShareDetails,
    ): Response<Unit>
}

interface FileUploadApi {
    @Multipart
    @POST
    suspend fun uploadFile(
        @Url url: String,
        @PartMap parts: Map<String, @JvmSuppressWildcards RequestBody>,
        @Part file: MultipartBody.Part,
    ): Response<Unit>
}

class WriteApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup) retrofit: Retrofit,
    @RetrofitConfiguration(RetrofitConfigurations.Basic) basicRetrofit: Retrofit,
) : CrisisCleanupWriteApi {
    private val changeWorksiteApi = retrofit.create(DataChangeApi::class.java)
    private val fileUploadApi = basicRetrofit.create(FileUploadApi::class.java)

    override suspend fun saveWorksite(
        modifiedAt: Instant,
        syncUuid: String,
        worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull {
        return if (worksite.id == null) {
            changeWorksiteApi.newWorksite(modifiedAt, syncUuid, worksite)
        } else {
            changeWorksiteApi.updateWorksite(modifiedAt, syncUuid, worksite.id, worksite)
        }
    }

    override suspend fun favoriteWorksite(createdAt: Instant, worksiteId: Long) =
        changeWorksiteApi.favorite(createdAt, worksiteId, networkTypeFavorite)

    override suspend fun unfavoriteWorksite(
        createdAt: Instant,
        worksiteId: Long,
        favoriteId: Long,
    ) {
        changeWorksiteApi.unfavorite(createdAt, worksiteId, NetworkFavoriteId(favoriteId))
    }

    override suspend fun addFlag(createdAt: Instant, worksiteId: Long, flag: NetworkFlag) =
        changeWorksiteApi.addFlag(createdAt, worksiteId, flag)

    override suspend fun deleteFlag(createdAt: Instant, worksiteId: Long, flagId: Long) {
        changeWorksiteApi.deleteFlag(createdAt, worksiteId, NetworkFlagId(flagId))
    }

    override suspend fun addNote(createdAt: Instant, worksiteId: Long, note: String) =
        changeWorksiteApi.addNote(createdAt, worksiteId, NetworkNoteNote(note, createdAt))

    override suspend fun updateWorkTypeStatus(
        createdAt: Instant,
        workTypeId: Long,
        status: String,
    ) =
        changeWorksiteApi.updateWorkTypeStatus(createdAt, workTypeId, NetworkWorkTypeStatus(status))

    override suspend fun claimWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: Collection<String>,
    ) =
        changeWorksiteApi.claimWorkTypes(createdAt, worksiteId, NetworkWorkTypeTypes(workTypes))

    override suspend fun unclaimWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: Collection<String>,
    ) = changeWorksiteApi.unclaimWorkTypes(createdAt, worksiteId, NetworkWorkTypeTypes(workTypes))

    override suspend fun requestWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: List<String>,
        reason: String,
    ) {
        changeWorksiteApi.requestWorkTypes(
            createdAt,
            worksiteId,
            NetworkWorkTypeChangeRequest(workTypes, reason),
        )
    }

    override suspend fun releaseWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: List<String>,
        reason: String,
    ) {
        changeWorksiteApi.releaseWorkTypes(
            createdAt,
            worksiteId,
            NetworkWorkTypeChangeRelease(workTypes, reason),
        )
    }

    override suspend fun deleteFile(worksiteId: Long, file: Long) {
        changeWorksiteApi.deleteFile(worksiteId, NetworkFilePush(file))
    }

    override suspend fun startFileUpload(fileName: String, contentType: String) =
        changeWorksiteApi.startFileUpload(fileName, contentType)

    override suspend fun uploadFile(
        url: String,
        fields: FileUploadFields,
        file: File,
        mimeType: String,
    ) {
        val mediaType = mimeType.toMediaTypeOrNull()
        val requestFile = file.asRequestBody(mediaType)
        val partFile = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val parts = fields.asPartMap()
        fileUploadApi.uploadFile(url, parts, partFile)
    }

    override suspend fun addFileToWorksite(worksiteId: Long, file: Long, tag: String) =
        changeWorksiteApi.addUploadedFile(worksiteId, NetworkFilePush(file, tag))

    override suspend fun shareWorksite(
        worksiteId: Long,
        emails: List<String>,
        phoneNumbers: List<String>,
        shareMessage: String,
        noClaimReason: String?,
    ) {
        changeWorksiteApi.shareWorksite(
            worksiteId,
            NetworkShareDetails(
                emails,
                phoneNumbers,
                shareMessage,
                if (noClaimReason?.isNotBlank() == true) noClaimReason else null,
            ),
        )
    }
}

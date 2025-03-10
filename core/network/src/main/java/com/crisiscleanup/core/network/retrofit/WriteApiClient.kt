package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.FileUploadFields
import com.crisiscleanup.core.network.model.NetworkFavoriteId
import com.crisiscleanup.core.network.model.NetworkFile
import com.crisiscleanup.core.network.model.NetworkFilePush
import com.crisiscleanup.core.network.model.NetworkFileUpload
import com.crisiscleanup.core.network.model.NetworkFlag
import com.crisiscleanup.core.network.model.NetworkFlagId
import com.crisiscleanup.core.network.model.NetworkIncidentRedeployRequest
import com.crisiscleanup.core.network.model.NetworkLocationCoordinates
import com.crisiscleanup.core.network.model.NetworkLocationUpdate
import com.crisiscleanup.core.network.model.NetworkNote
import com.crisiscleanup.core.network.model.NetworkNoteNote
import com.crisiscleanup.core.network.model.NetworkPointLocation
import com.crisiscleanup.core.network.model.NetworkRequestRedeploy
import com.crisiscleanup.core.network.model.NetworkShareDetails
import com.crisiscleanup.core.network.model.NetworkType
import com.crisiscleanup.core.network.model.NetworkWorkType
import com.crisiscleanup.core.network.model.NetworkWorkTypeChangeRelease
import com.crisiscleanup.core.network.model.NetworkWorkTypeChangeRequest
import com.crisiscleanup.core.network.model.NetworkWorkTypeStatus
import com.crisiscleanup.core.network.model.NetworkWorkTypeTypes
import com.crisiscleanup.core.network.model.NetworkWorksiteFull
import com.crisiscleanup.core.network.model.NetworkWorksitePush
import com.crisiscleanup.core.network.model.asPartMap
import com.crisiscleanup.core.network.model.networkTypeFavorite
import kotlinx.datetime.Instant
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.PartMap
import retrofit2.http.Path
import retrofit2.http.Url
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

    @TokenAuthenticationHeader
    @POST("incident_requests")
    suspend fun requestRedeploy(
        @Body redeployPayload: NetworkRequestRedeploy,
    ): NetworkIncidentRedeployRequest?

    @TokenAuthenticationHeader
    @POST("user_geo_locations")
    suspend fun shareLocation(
        @Body pointLocation: NetworkPointLocation,
    ): NetworkLocationUpdate
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
    private val writeApi = retrofit.create(DataChangeApi::class.java)
    private val fileUploadApi = basicRetrofit.create(FileUploadApi::class.java)

    override suspend fun saveWorksite(
        modifiedAt: Instant,
        syncUuid: String,
        worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull {
        return if (worksite.id == null) {
            writeApi.newWorksite(modifiedAt, syncUuid, worksite)
        } else {
            writeApi.updateWorksite(modifiedAt, syncUuid, worksite.id, worksite)
        }
    }

    override suspend fun favoriteWorksite(createdAt: Instant, worksiteId: Long) =
        writeApi.favorite(createdAt, worksiteId, networkTypeFavorite)

    override suspend fun unfavoriteWorksite(
        createdAt: Instant,
        worksiteId: Long,
        favoriteId: Long,
    ) {
        writeApi.unfavorite(createdAt, worksiteId, NetworkFavoriteId(favoriteId))
    }

    override suspend fun addFlag(createdAt: Instant, worksiteId: Long, flag: NetworkFlag) =
        writeApi.addFlag(createdAt, worksiteId, flag)

    override suspend fun deleteFlag(createdAt: Instant, worksiteId: Long, flagId: Long) {
        writeApi.deleteFlag(createdAt, worksiteId, NetworkFlagId(flagId))
    }

    override suspend fun addNote(createdAt: Instant, worksiteId: Long, note: String) =
        writeApi.addNote(createdAt, worksiteId, NetworkNoteNote(note, createdAt))

    override suspend fun updateWorkTypeStatus(
        createdAt: Instant,
        workTypeId: Long,
        status: String,
    ) =
        writeApi.updateWorkTypeStatus(createdAt, workTypeId, NetworkWorkTypeStatus(status))

    override suspend fun claimWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: Collection<String>,
    ) =
        writeApi.claimWorkTypes(createdAt, worksiteId, NetworkWorkTypeTypes(workTypes))

    override suspend fun unclaimWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: Collection<String>,
    ) = writeApi.unclaimWorkTypes(createdAt, worksiteId, NetworkWorkTypeTypes(workTypes))

    override suspend fun requestWorkTypes(
        createdAt: Instant,
        worksiteId: Long,
        workTypes: List<String>,
        reason: String,
    ) {
        writeApi.requestWorkTypes(
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
        writeApi.releaseWorkTypes(
            createdAt,
            worksiteId,
            NetworkWorkTypeChangeRelease(workTypes, reason),
        )
    }

    override suspend fun deleteFile(worksiteId: Long, file: Long) {
        writeApi.deleteFile(worksiteId, NetworkFilePush(file))
    }

    override suspend fun startFileUpload(fileName: String, contentType: String) =
        writeApi.startFileUpload(fileName, contentType)

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
        writeApi.addUploadedFile(worksiteId, NetworkFilePush(file, tag))

    override suspend fun shareWorksite(
        worksiteId: Long,
        emails: List<String>,
        phoneNumbers: List<String>,
        shareMessage: String,
        noClaimReason: String?,
    ) {
        writeApi.shareWorksite(
            worksiteId,
            NetworkShareDetails(
                emails,
                phoneNumbers,
                shareMessage,
                if (noClaimReason?.isNotBlank() == true) noClaimReason else null,
            ),
        )
    }

    override suspend fun requestRedeploy(organizationId: Long, incidentId: Long) =
        writeApi.requestRedeploy(
            NetworkRequestRedeploy(
                organizationId,
                incidentId,
            ),
        )?.let { it.organization == organizationId && it.incident == incidentId } == true

    override suspend fun shareLocation(latitude: Double, longitude: Double) {
        writeApi.shareLocation(
            NetworkPointLocation(
                NetworkLocationCoordinates(listOf(longitude, latitude), "Point"),
            ),
        )
    }
}

package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import javax.inject.Inject

private interface DataChangeApi {
    @TokenAuthenticationHeader
    @POST("worksites")
    suspend fun newWorksite(
        @Header("cc-created-at") createdAt: Instant,
        @Header("cc-sync-uuid") syncUuid: String,
        @Body worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull

    @TokenAuthenticationHeader
    @PUT("worksites/{worksiteId}")
    suspend fun updateWorksite(
        @Header("cc-modified-at") modifiedAt: Instant,
        @Header("cc-sync-uuid") syncUuid: String,
        @Path("worksiteId")
        worksiteId: Long,
        @Body worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/favorite")
    suspend fun favorite(
        @Header("cc-created-at") createdAt: Instant,
        @Path("worksiteId")
        worksiteId: Long,
        @Body favorite: NetworkType,
    ): NetworkType

    @TokenAuthenticationHeader
    @HTTP(method = "DELETE", path = "worksites/{worksiteId}/favorite", hasBody = true)
    suspend fun unfavorite(
        @Header("cc-created-at") createdAt: Instant,
        @Path("worksiteId")
        worksiteId: Long,
        @Body favoriteId: NetworkFavoriteId,
    ): Response<Unit>

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/flags")
    suspend fun addFlag(
        @Header("cc-created-at") createdAt: Instant,
        @Path("worksiteId")
        worksiteId: Long,
        @Body flag: NetworkFlag,
    ): NetworkFlag

    @TokenAuthenticationHeader
    @HTTP(method = "DELETE", path = "worksites/{worksiteId}/flags", hasBody = true)
    suspend fun deleteFlag(
        @Header("cc-created-at") createdAt: Instant,
        @Path("worksiteId")
        worksiteId: Long,
        @Body flagId: NetworkFlagId,
    ): Response<Unit>

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/notes")
    suspend fun addNote(
        @Path("worksiteId")
        worksiteId: Long,
        @Body note: NetworkNoteNote,
    ): NetworkNote

    @TokenAuthenticationHeader
    @PATCH("worksite_work_types/{workTypeId}")
    suspend fun updateWorkTypeStatus(
        @Header("cc-created-at") createdAt: Instant,
        @Path("workTypeId")
        workTypeId: Long,
        @Body status: NetworkWorkTypeStatus,
    ): NetworkWorkType

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/claim")
    suspend fun claimWorkTypes(
        @Header("cc-created-at") createdAt: Instant,
        @Path("worksiteId")
        worksiteId: Long,
        @Body workTypes: NetworkWorkTypeTypes,
    )

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/unclaim")
    suspend fun unclaimWorkTypes(
        @Header("cc-created-at") createdAt: Instant,
        @Path("worksiteId")
        worksiteId: Long,
        @Body workTypes: NetworkWorkTypeTypes,
    )

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/request_take")
    suspend fun requestWorkTypes(
        @Path("worksiteId")
        worksiteId: Long,
        @Body request: NetworkWorkTypeChangeRequest,
    )

    @TokenAuthenticationHeader
    @POST("worksites/{worksiteId}/release")
    suspend fun releaseWorkTypes(
        @Path("worksiteId")
        worksiteId: Long,
        @Body release: NetworkWorkTypeChangeRelease,
    )
}

class WriteApiClient @Inject constructor(
    @CrisisCleanupRetrofit retrofit: Retrofit
) : CrisisCleanupWriteApi {
    private val changeWorksiteApi = retrofit.create(DataChangeApi::class.java)

    override suspend fun saveWorksite(
        modifiedAt: Instant,
        syncUuid: String,
        worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull {
        return if (worksite.id == null)
            changeWorksiteApi.newWorksite(modifiedAt, syncUuid, worksite)
        else changeWorksiteApi.updateWorksite(modifiedAt, syncUuid, worksite.id, worksite)
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

    override suspend fun addNote(worksiteId: Long, note: String) =
        changeWorksiteApi.addNote(worksiteId, NetworkNoteNote(note))

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
        worksiteId: Long,
        workTypes: List<String>,
        reason: String
    ) = changeWorksiteApi.requestWorkTypes(
        worksiteId,
        NetworkWorkTypeChangeRequest(workTypes, reason),
    )

    override suspend fun releaseWorkTypes(
        worksiteId: Long,
        workTypes: List<String>,
        reason: String
    ) = changeWorksiteApi.releaseWorkTypes(
        worksiteId,
        NetworkWorkTypeChangeRelease(workTypes, reason),
    )
}
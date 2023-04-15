package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
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
    @PUT("worksites/:worksiteId")
    suspend fun updateWorksite(
        @Header("cc-modified-at") createdAt: Instant,
        @Header("cc-sync-uuid") syncUuid: String,
        @Path("worksiteId")
        worksiteId: Long,
        @Body worksite: NetworkWorksitePush,
    ): NetworkWorksiteFull

    @TokenAuthenticationHeader
    @POST("worksites/:worksiteId/favorite")
    suspend fun favorite(
        @Path("worksiteId")
        worksiteId: Long,
        @Body favorite: NetworkType,
    ): NetworkType

    @TokenAuthenticationHeader
    @DELETE("worksites/:worksiteId/favorite")
    suspend fun unfavorite(
        @Path("worksiteId")
        worksiteId: Long,
        @Body favoriteId: NetworkFavoriteId,
    )

    @TokenAuthenticationHeader
    @POST("worksites/:worksiteId/flags")
    suspend fun addFlag(
        @Path("worksiteId")
        worksiteId: Long,
        @Body flag: NetworkFlag,
    ): NetworkFlag

    @TokenAuthenticationHeader
    @DELETE("worksites/:worksiteId/flags")
    suspend fun deleteFlag(
        @Path("worksiteId")
        worksiteId: Long,
        @Body flagId: NetworkFlagId,
    )

    @TokenAuthenticationHeader
    @POST("worksites/:worksiteId/notes")
    suspend fun addNote(
        @Path("worksiteId")
        worksiteId: Long,
        @Body note: NetworkNoteNote,
    ): NetworkNote

    @TokenAuthenticationHeader
    @PATCH("worksite_work_types/:workTypeId")
    suspend fun updateWorkTypeStatus(
        @Path("workTypeId")
        workTypeId: Long,
        @Body status: NetworkWorkTypeStatus,
    ): NetworkWorkType

    @TokenAuthenticationHeader
    @POST("worksites/:worksiteId/claim")
    suspend fun claimWorkTypes(
        @Path("worksiteId")
        worksiteId: Long,
        @Body workTypes: NetworkWorkTypeTypes,
    )

    @TokenAuthenticationHeader
    @POST("worksites/:worksiteId/unclaim")
    suspend fun unclaimWorkTypes(
        @Path("worksiteId")
        worksiteId: Long,
        @Body workTypes: NetworkWorkTypeTypes,
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

    override suspend fun favoriteWorksite(worksiteId: Long) =
        changeWorksiteApi.favorite(worksiteId, networkTypeFavorite)

    override suspend fun unfavoriteWorksite(worksiteId: Long, favoriteId: Long) =
        changeWorksiteApi.unfavorite(worksiteId, NetworkFavoriteId(favoriteId))

    override suspend fun addFlag(worksiteId: Long, flag: NetworkFlag) =
        changeWorksiteApi.addFlag(worksiteId, flag)

    override suspend fun deleteFlag(worksiteId: Long, flagId: Long) =
        changeWorksiteApi.deleteFlag(worksiteId, NetworkFlagId(flagId))

    override suspend fun addNote(worksiteId: Long, note: String) =
        changeWorksiteApi.addNote(worksiteId, NetworkNoteNote(note))

    override suspend fun updateWorkTypeStatus(workTypeId: Long, status: String) =
        changeWorksiteApi.updateWorkTypeStatus(workTypeId, NetworkWorkTypeStatus(status))

    override suspend fun claimWorkTypes(worksiteId: Long, workTypes: Collection<String>) =
        changeWorksiteApi.claimWorkTypes(worksiteId, NetworkWorkTypeTypes(workTypes))

    override suspend fun unclaimWorkTypes(worksiteId: Long, workTypes: Collection<String>) =
        changeWorksiteApi.unclaimWorkTypes(worksiteId, NetworkWorkTypeTypes(workTypes))
}
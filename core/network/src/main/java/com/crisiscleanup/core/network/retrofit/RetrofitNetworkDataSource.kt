package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkIncidentsResult
import com.crisiscleanup.core.network.model.NetworkLocationsResult
import com.crisiscleanup.core.network.model.NetworkWorksitesCountResult
import com.crisiscleanup.core.network.model.NetworkWorksitesFullResult
import com.crisiscleanup.core.network.model.NetworkWorksitesShortResult
import kotlinx.datetime.Instant
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

private interface CrisisCleanupNetworkApi {
    @GET("incidents")
    suspend fun getIncidents(
        @Query("fields")
        fields: String,
        @Query("limit")
        limit: Int,
        @Query("ordering")
        ordering: String
    ): NetworkIncidentsResult

    @GET("locations")
    suspend fun getLocations(
        @Query("id__in")
        ids: String,
        @Query("limit")
        limit: Int,
    ): NetworkLocationsResult

    @TokenAuthenticationHeader
    @GET("worksites")
    suspend fun getWorksites(
        @Query("incident")
        incidentId: Long,
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
    ): NetworkWorksitesFullResult

    @TokenAuthenticationHeader
    @GET("worksites/count")
    suspend fun getWorksitesCount(
        @Query("incident")
        incidentId: Long,
    ): NetworkWorksitesCountResult

    @TokenAuthenticationHeader
    @GET("worksites_all")
    suspend fun getWorksitesAll(
        @Query("incident")
        incidentId: Long,
        @Query("updated_at__gt")
        updatedAtAfter: Instant?,
        @Query("updated_at__lt")
        updatedAtBefore: Instant? = null,
    ): NetworkWorksitesShortResult
}

@Singleton
class RetrofitNetworkDataSource @Inject constructor(
    @CrisisCleanupRetrofit retrofit: Retrofit
) : CrisisCleanupNetworkDataSource {
    private val networkApi = retrofit.create(CrisisCleanupNetworkApi::class.java)
    override suspend fun getIncidents(fields: List<String>, limit: Int, ordering: String) =
        networkApi.getIncidents(fields.joinToString(","), limit, ordering)

    override suspend fun getIncidentLocations(locationIds: List<Long>): NetworkLocationsResult =
        networkApi.getLocations(locationIds.joinToString(","), locationIds.size)

    override suspend fun getWorksites(
        incidentId: Long,
        limit: Int,
        offset: Int
    ): NetworkWorksitesFullResult = networkApi.getWorksites(incidentId, limit, offset)

    override suspend fun getWorksitesCount(incidentId: Long): NetworkWorksitesCountResult =
        networkApi.getWorksitesCount(incidentId)

    override suspend fun getWorksitesAll(
        incidentId: Long,
        updatedAtAfter: Instant?,
        updatedAtBefore: Instant?
    ): NetworkWorksitesShortResult =
        networkApi.getWorksitesAll(incidentId, updatedAtAfter, updatedAtBefore)
}
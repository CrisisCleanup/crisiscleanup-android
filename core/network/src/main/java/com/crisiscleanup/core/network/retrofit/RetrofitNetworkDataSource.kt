package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

private interface CrisisCleanupNetworkApi {
    @TokenAuthenticationHeader
    @GET("incidents")
    suspend fun getIncidents(
        @Query("fields")
        fields: String,
        @Query("limit")
        limit: Int,
        @Query("ordering")
        ordering: String
    ): NetworkIncidentsResult

    @TokenAuthenticationHeader
    @GET("locations")
    suspend fun getLocations(
        @Query("id__in")
        ids: String,
        @Query("limit")
        limit: Int,
    ): NetworkLocationsResult

    @TokenAuthenticationHeader
    @WrapIncidentResponseHeader
    @GET("incidents/{id}")
    suspend fun getIncident(
        @Path("id")
        id: Long,
        @Query("fields")
        fields: String,
    ): NetworkIncidentResult

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
        @Query("updated_at__gt")
        updatedAtAfter: Instant? = null,
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

    @TokenAuthenticationHeader
    @GET("worksites_page")
    suspend fun getWorksitesPage(
        @Query("incident")
        incidentId: Long,
        @Query("limit")
        pageCount: Int,
        @Query("page")
        pageOffset: Int?,
        @Query("center_coordinates")
        centerCoordinates: List<Double>?,
        @Query("updated_at__gt")
        updatedAtAfter: Instant?,
    ): NetworkWorksitesShortResult
}

@Singleton
class RetrofitNetworkDataSource @Inject constructor(
    @CrisisCleanupRetrofit retrofit: Retrofit
) : CrisisCleanupNetworkDataSource {
    private val networkApi = retrofit.create(CrisisCleanupNetworkApi::class.java)
    override suspend fun getIncidents(fields: List<String>, limit: Int, ordering: String) =
        networkApi.getIncidents(fields.joinToString(","), limit, ordering)

    override suspend fun getIncidentLocations(locationIds: List<Long>) =
        networkApi.getLocations(locationIds.joinToString(","), locationIds.size)

    override suspend fun getIncident(id: Long, fields: List<String>) =
        networkApi.getIncident(id, fields.joinToString(","))

    override suspend fun getWorksites(incidentId: Long, limit: Int, offset: Int) =
        networkApi.getWorksites(incidentId, limit, offset)

    override suspend fun getWorksitesCount(incidentId: Long, updatedAtAfter: Instant?) =
        networkApi.getWorksitesCount(incidentId, updatedAtAfter)

    override suspend fun getWorksitesAll(
        incidentId: Long,
        updatedAtAfter: Instant?,
        updatedAtBefore: Instant?
    ) = networkApi.getWorksitesAll(incidentId, updatedAtAfter, updatedAtBefore)

    override suspend fun getWorksitesPage(
        incidentId: Long,
        updatedAtAfter: Instant?,
        pageCount: Int,
        pageOffset: Int?,
        latitude: Double?,
        longitude: Double?
    ): NetworkWorksitesShortResult {
        val centerCoordinates: List<Double>? = if (latitude == null && longitude == null) null else
            listOf(latitude!!, longitude!!)
        return networkApi.getWorksitesPage(
            incidentId,
            pageCount,
            if ((pageOffset ?: 0) <= 1) null else pageOffset,
            centerCoordinates,
            updatedAtAfter,
        )
    }
}
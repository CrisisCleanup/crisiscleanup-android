package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject

private interface DataSourceApi {
    @TokenAuthenticationHeader
    @GET("incidents")
    suspend fun getIncidents(
        @Query("fields")
        fields: String,
        @Query("limit")
        limit: Int,
        @Query("ordering")
        ordering: String,
        @Query("start_at__gt")
        after: Instant?,
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
    @WrapResponseHeader("incident")
    @GET("incidents/{id}")
    suspend fun getIncident(
        @Path("id")
        id: Long,
        @Query("fields")
        fields: String,
    ): NetworkIncidentResult

    @TokenAuthenticationHeader
    @GET("incidents/{incidentId}/organizations")
    suspend fun getIncidentOrganizations(
        @Path("incidentId")
        incidentId: Long,
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
    ): NetworkIncidentOrganizationsResult

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
    @GET("worksites")
    suspend fun getWorksitesLocationSearch(
        @Query("incident")
        incidentId: Long,
        @Query("fields")
        fields: String,
        @Query("search")
        q: String,
        @Query("limit")
        limit: Int,
    ): NetworkWorksiteLocationSearchResult

    @TokenAuthenticationHeader
    @GET("worksites")
    suspend fun getWorksites(
        @Query("id__in")
        worksiteIds: String,
    ): NetworkWorksitesFullResult

    @TokenAuthenticationHeader
    @GET("worksites/count")
    suspend fun getWorksitesCount(
        @Query("incident")
        incidentId: Long,
        @Query("updated_at__gt")
        updatedAtAfter: Instant? = null,
    ): NetworkCountResult

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

    @GET("languages")
    suspend fun getLanguages(): NetworkLanguagesResult

    @WrapResponseHeader("translation")
    @GET("languages/{key}")
    suspend fun getLanguageTranslations(
        @Path("key")
        languageKey: String,
    ): NetworkLanguageTranslationResult

    @GET("localizations/count")
    suspend fun getLocalizationCount(
        @Query("updated_at__gt")
        after: Instant,
    ): NetworkCountResult
}

class DataApiClient @Inject constructor(
    @CrisisCleanupRetrofit retrofit: Retrofit
) : CrisisCleanupNetworkDataSource {
    private val networkApi = retrofit.create(DataSourceApi::class.java)
    override suspend fun getIncidents(
        fields: List<String>,
        limit: Int,
        ordering: String,
        after: Instant?
    ) =
        networkApi.getIncidents(fields.joinToString(","), limit, ordering, after)

    override suspend fun getIncidentLocations(locationIds: List<Long>) =
        networkApi.getLocations(locationIds.joinToString(","), locationIds.size)

    override suspend fun getIncident(id: Long, fields: List<String>) =
        networkApi.getIncident(id, fields.joinToString(","))

    override suspend fun getIncidentOrganizations(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ) = networkApi.getIncidentOrganizations(incidentId, limit, offset)

    override suspend fun getWorksites(incidentId: Long, limit: Int, offset: Int) =
        networkApi.getWorksites(incidentId, limit, offset)

    override suspend fun getWorksites(worksiteIds: Collection<Long>) =
        networkApi.getWorksites(worksiteIds.joinToString(","))

    override suspend fun getWorksite(id: Long): NetworkWorksiteFull? {
        val result = getWorksites(listOf(id))
        return result.results?.firstOrNull()
    }

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

    private val locationSearchFields = listOf(
        "id",
        "name",
        "case_number",
        "address",
        "postal_code",
        "city",
        "state",
        "incident",
        "location",
        "key_work_type",
    ).joinToString(",")

    override suspend fun getLocationSearchWorksites(
        incidentId: Long,
        q: String,
        limit: Int
    ) = networkApi.getWorksitesLocationSearch(
        incidentId,
        locationSearchFields,
        q,
        limit
    )

    override suspend fun getLanguages() = networkApi.getLanguages()

    override suspend fun getLanguageTranslations(key: String) =
        networkApi.getLanguageTranslations(key)

    override suspend fun getLocalizationCount(after: Instant) =
        networkApi.getLocalizationCount(after)
}
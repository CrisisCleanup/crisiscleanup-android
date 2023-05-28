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
    @GET("statuses")
    suspend fun getStatuses(): NetworkWorkTypeStatusResult

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
    @GET("worksites_all")
    suspend fun getWorksitesSearch(
        @Query("incident")
        incidentId: Long,
        @Query("search")
        q: String,
    ): NetworkWorksitesShortResult

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
    @GET("worksites_all")
    suspend fun getWorksitesShort(
        @Query("id__in")
        id: Long,
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

    @TokenAuthenticationHeader
    @GET("worksite_requests")
    suspend fun getWorkTypeRequests(
        @Query("worksite_work_type__worksite")
        id: Long,
    ): NetworkWorkTypeRequestResult
}

class DataApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup) retrofit: Retrofit,
) : CrisisCleanupNetworkDataSource {
    private val networkApi = retrofit.create(DataSourceApi::class.java)

    override suspend fun getStatuses() = networkApi.getStatuses()

    override suspend fun getIncidents(
        fields: List<String>,
        limit: Int,
        ordering: String,
        after: Instant?
    ) = networkApi.getIncidents(fields.joinToString(","), limit, ordering, after)
        .let {
            it.errors?.tryThrowException()
            it.results ?: emptyList()
        }


    override suspend fun getIncidentLocations(locationIds: List<Long>) =
        networkApi.getLocations(locationIds.joinToString(","), locationIds.size)
            .let {
                it.errors?.tryThrowException()
                it.results ?: emptyList()
            }

    override suspend fun getIncident(id: Long, fields: List<String>) =
        networkApi.getIncident(id, fields.joinToString(","))
            .let {
                it.errors?.tryThrowException()
                it.incident
            }

    override suspend fun getIncidentOrganizations(
        incidentId: Long,
        limit: Int,
        offset: Int,
    ) = networkApi.getIncidentOrganizations(incidentId, limit, offset)
        .apply { errors?.tryThrowException() }

    override suspend fun getWorksites(incidentId: Long, limit: Int, offset: Int) =
        networkApi.getWorksites(incidentId, limit, offset)
            .apply { errors?.tryThrowException() }

    override suspend fun getWorksites(worksiteIds: Collection<Long>) =
        networkApi.getWorksites(worksiteIds.joinToString(","))
            .apply { errors?.tryThrowException() }

    override suspend fun getWorksite(id: Long) = getWorksites(listOf(id))
        .let {
            it.errors?.tryThrowException()
            it.results?.firstOrNull()
        }

    override suspend fun getWorksiteShort(id: Long) =
        networkApi.getWorksitesShort(id).results?.get(0)

    override suspend fun getWorksitesCount(incidentId: Long, updatedAtAfter: Instant?) =
        networkApi.getWorksitesCount(incidentId, updatedAtAfter)
            .let {
                it.errors?.tryThrowException()
                it.count ?: 0
            }

    override suspend fun getWorksitesAll(
        incidentId: Long,
        updatedAtAfter: Instant?,
        updatedAtBefore: Instant?
    ) = networkApi.getWorksitesAll(incidentId, updatedAtAfter, updatedAtBefore)
        .apply { errors?.tryThrowException() }

    override suspend fun getWorksitesPage(
        incidentId: Long,
        updatedAtAfter: Instant?,
        pageCount: Int,
        pageOffset: Int?,
        latitude: Double?,
        longitude: Double?
    ): List<NetworkWorksiteShort> {
        val centerCoordinates: List<Double>? = if (latitude == null && longitude == null) null else
            listOf(latitude!!, longitude!!)
        val result = networkApi.getWorksitesPage(
            incidentId,
            pageCount,
            if ((pageOffset ?: 0) <= 1) null else pageOffset,
            centerCoordinates,
            updatedAtAfter,
        )
        result.errors?.tryThrowException()
        return result.results ?: emptyList()
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
        .let {
            it.errors?.tryThrowException()
            it.results ?: emptyList()
        }

    override suspend fun getSearchWorksites(
        incidentId: Long,
        q: String,
    ) = networkApi.getWorksitesSearch(incidentId, q)
        .let {
            it.errors?.tryThrowException()
            it.results ?: emptyList()
        }

    override suspend fun getLanguages() = networkApi.getLanguages().results

    override suspend fun getLanguageTranslations(key: String) =
        networkApi.getLanguageTranslations(key).translation

    override suspend fun getLocalizationCount(after: Instant) =
        networkApi.getLocalizationCount(after)

    override suspend fun getWorkTypeRequests(id: Long) =
        networkApi.getWorkTypeRequests(id)
            .let {
                it.errors?.tryThrowException()
                it.results ?: emptyList()
            }
}
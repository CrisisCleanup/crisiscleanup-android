package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.*
import kotlinx.datetime.Instant
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import javax.inject.Inject

private interface DataSourceApi {
    @TokenAuthenticationHeader
    @GET("users/me")
    suspend fun getProfile(): NetworkAccountProfileResult

    @TokenAuthenticationHeader
    @GET("organizations")
    suspend fun getOrganizations(
        @Query("id__in")
        ids: String,
    ): NetworkOrganizationsResult

    @GET("statuses")
    suspend fun getStatuses(): NetworkWorkTypeStatusResult

    @TokenAuthenticationHeader
    @GET("incidents")
    suspend fun getIncidents(
        @Query("fields")
        fields: String,
        @Query("limit")
        limit: Int,
        @Query("sort")
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
    ): NetworkOrganizationsResult

    @TokenAuthenticationHeader
    @GET("worksites")
    suspend fun getWorksitesCoreData(
        @Query("incident")
        incidentId: Long,
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
        @Query("fields")
        fields: String?,
    ): NetworkWorksitesCoreDataResult

    @TokenAuthenticationHeader
    @GET("worksites_all")
    suspend fun getWorksitesSearch(
        @Query("incident")
        incidentId: Long,
        @Query("search")
        q: String,
        @QueryMap
        filters: Map<String, @JvmSuppressWildcards Any> = emptyMap(),
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
    ): NetworkWorksitesPageResult

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

    @TokenAuthenticationHeader
    @GET("/organizations")
    suspend fun getNearbyClaimingOrganizations(
        @Query("nearby_claimed")
        nearbyClaimed: String,
    ): NetworkOrganizationsResult

    @TokenAuthenticationHeader
    @GET("/users")
    suspend fun searchUsers(
        @Query("search")
        q: String,
        @Query("organization")
        organization: Long,
        @Query("limit")
        limit: Int,
    ): NetworkUsersResult

    @TokenAuthenticationHeader
    @WrapResponseHeader("events")
    @GET("worksites/{id}/history")
    suspend fun getCaseHistory(
        @Path("id")
        worksiteId: Long,
    ): NetworkCaseHistoryResult

    @TokenAuthenticationHeader
    @GET("/users")
    suspend fun getUsers(
        @Query("id__in")
        ids: String,
    ): NetworkUsersResult

    @TokenAuthenticationHeader
    @GET("/organizations")
    suspend fun searchOrganizations(
        @Query("search") q: String,
    ): NetworkOrganizationsSearchResult

    @Headers("Cookie: ")
    @GET("/users/me")
    suspend fun getProfile(
        @Header("Authorization")
        accessToken: String,
    ): NetworkUserProfile
}

private val worksiteCoreDataFields = listOf(
    "id",
    "incident",
    "name",
    "case_number",
    "location",
    "address",
    "postal_code",
    "city",
    "county",
    "state",
    "phone1",
    "phone2",
    "email",
    "form_data",
    "flags",
    "notes",
    "work_types",
    "favorite",
    "what3words",
    "pluscode",
    "svi",
    "auto_contact_frequency_t",
    "reported_by",
    "updated_at",
)
private val worksiteCoreDataFieldsQ = worksiteCoreDataFields.joinToString(",")

class DataApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.CrisisCleanup) retrofit: Retrofit,
) : CrisisCleanupNetworkDataSource {
    private val networkApi = retrofit.create(DataSourceApi::class.java)

    override suspend fun getProfilePic() = networkApi.getProfile().files?.profilePictureUrl

    override suspend fun getOrganizations(organizations: List<Long>) =
        networkApi.getOrganizations(organizations.joinToString(",")).let {
            it.errors?.tryThrowException()
            it.results ?: emptyList()
        }

    override suspend fun getStatuses() = networkApi.getStatuses()

    override suspend fun getIncidents(
        fields: List<String>,
        limit: Int,
        ordering: String,
        after: Instant?,
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

    override suspend fun getWorksitesCoreData(incidentId: Long, limit: Int, offset: Int) =
        networkApi.getWorksitesCoreData(incidentId, limit, offset, worksiteCoreDataFieldsQ)
            .apply { errors?.tryThrowException() }
            .results

    override suspend fun getWorksites(worksiteIds: Collection<Long>) =
        networkApi.getWorksites(worksiteIds.joinToString(","))
            .apply { errors?.tryThrowException() }
            .results

    override suspend fun getWorksite(id: Long) = getWorksites(listOf(id))?.firstOrNull()

    override suspend fun getWorksitesCount(incidentId: Long, updatedAtAfter: Instant?) =
        networkApi.getWorksitesCount(incidentId, updatedAtAfter)
            .let {
                it.errors?.tryThrowException()
                it.count ?: 0
            }

    override suspend fun getWorksitesPage(
        incidentId: Long,
        pageCount: Int,
        pageOffset: Int?,
        latitude: Double?,
        longitude: Double?,
        updatedAtAfter: Instant?,
    ): List<NetworkWorksitePage> {
        val centerCoordinates: List<Double>? = if (latitude == null && longitude == null) {
            null
        } else {
            listOf(latitude!!, longitude!!)
        }
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
        limit: Int,
    ) = networkApi.getWorksitesLocationSearch(
        incidentId,
        locationSearchFields,
        q,
        limit,
    )
        .let {
            it.errors?.tryThrowException()
            it.results ?: emptyList()
        }

    override suspend fun getSearchWorksites(
        incidentId: Long,
        q: String,
        filters: Map<String, Any>,
    ) = networkApi.getWorksitesSearch(incidentId, q, filters)
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

    override suspend fun getNearbyOrganizations(
        latitude: Double,
        longitude: Double,
    ) = networkApi.getNearbyClaimingOrganizations("$longitude,$latitude")
        .let {
            it.errors?.tryThrowException()
            it.results ?: emptyList()
        }

    override suspend fun searchUsers(q: String, organization: Long, limit: Int) =
        networkApi.searchUsers(q, organization, limit)
            .let {
                it.errors?.tryThrowException()
                it.results ?: emptyList()
            }

    override suspend fun getCaseHistory(worksiteId: Long) = networkApi.getCaseHistory(worksiteId)
        .let {
            it.errors?.tryThrowException()
            it.events ?: emptyList()
        }

    override suspend fun getUsers(ids: Collection<Long>) =
        networkApi.getUsers(ids.joinToString(","))
            .let {
                it.errors?.tryThrowException()
                it.results ?: emptyList()
            }

    override suspend fun searchOrganizations(q: String) =
        networkApi.searchOrganizations(q).results ?: emptyList()

    override suspend fun getProfile(accessToken: String) =
        networkApi.getProfile("Bearer $accessToken")
}

package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkAccountProfileResult
import com.crisiscleanup.core.network.model.NetworkCaseHistoryResult
import com.crisiscleanup.core.network.model.NetworkCountResult
import com.crisiscleanup.core.network.model.NetworkFlagsFormDataResult
import com.crisiscleanup.core.network.model.NetworkIncidentResult
import com.crisiscleanup.core.network.model.NetworkIncidentsListResult
import com.crisiscleanup.core.network.model.NetworkIncidentsResult
import com.crisiscleanup.core.network.model.NetworkLanguageTranslationResult
import com.crisiscleanup.core.network.model.NetworkLanguagesResult
import com.crisiscleanup.core.network.model.NetworkList
import com.crisiscleanup.core.network.model.NetworkListResult
import com.crisiscleanup.core.network.model.NetworkListsResult
import com.crisiscleanup.core.network.model.NetworkLocationsResult
import com.crisiscleanup.core.network.model.NetworkOrganizationsResult
import com.crisiscleanup.core.network.model.NetworkOrganizationsSearchResult
import com.crisiscleanup.core.network.model.NetworkPortalConfig
import com.crisiscleanup.core.network.model.NetworkRedeployRequestsResult
import com.crisiscleanup.core.network.model.NetworkTeamResult
import com.crisiscleanup.core.network.model.NetworkUserProfile
import com.crisiscleanup.core.network.model.NetworkUsersResult
import com.crisiscleanup.core.network.model.NetworkWorkTypeRequestResult
import com.crisiscleanup.core.network.model.NetworkWorkTypeStatusResult
import com.crisiscleanup.core.network.model.NetworkWorksiteChange
import com.crisiscleanup.core.network.model.NetworkWorksiteChangesResult
import com.crisiscleanup.core.network.model.NetworkWorksiteLocationSearchResult
import com.crisiscleanup.core.network.model.NetworkWorksitesCoreDataResult
import com.crisiscleanup.core.network.model.NetworkWorksitesFullResult
import com.crisiscleanup.core.network.model.NetworkWorksitesPageResult
import com.crisiscleanup.core.network.model.NetworkWorksitesShortResult
import com.crisiscleanup.core.network.model.tryThrowException
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Tag
import javax.inject.Inject
import kotlin.time.Instant

private interface DataSourceApi {
    @TokenAuthenticationHeader
    @GET("users/{user}")
    suspend fun getProfile(
        @Path("user")
        userId: Long,
        @Tag endpointId: EndpointRequestId = EndpointRequestId.UserProfile,
    ): NetworkAccountProfileResult

    @GET("users/me")
    suspend fun getProfile(
        @Header("Authorization")
        accessToken: String,
        @Tag endpointId: EndpointRequestId = EndpointRequestId.MyProfileNoAuth,
    ): NetworkUserProfile

    @TokenAuthenticationHeader
    @GET("organizations")
    suspend fun getOrganizations(
        @Query("id__in", encoded = true)
        ids: String,
    ): NetworkOrganizationsResult

    @GET("statuses")
    suspend fun getStatuses(): NetworkWorkTypeStatusResult

    @TokenAuthenticationHeader
    @GET("incidents")
    suspend fun getIncidents(
        @Query("fields", encoded = true)
        fields: String,
        @Query("limit")
        limit: Int,
        @Query("sort")
        ordering: String,
        @Query("start_at__gt")
        after: Instant?,
        @Tag endpointId: EndpointRequestId = EndpointRequestId.Incidents,
    ): NetworkIncidentsResult

    @GET("incidents")
    suspend fun getIncidentsNoAuth(
        @Query("fields", encoded = true)
        fields: String,
        @Query("limit")
        limit: Int,
        @Query("sort")
        ordering: String,
        @Query("start_at__gt")
        after: Instant?,
        @Tag endpointId: EndpointRequestId = EndpointRequestId.IncidentsNoAuth,
    ): NetworkIncidentsResult

    @GET("incidents_list")
    suspend fun getIncidentsList(
        @Query("fields", encoded = true)
        fields: String,
        @Query("limit")
        limit: Int,
        @Query("sort")
        ordering: String,
    ): NetworkIncidentsListResult

    @TokenAuthenticationHeader
    @GET("locations")
    suspend fun getLocations(
        @Query("id__in", encoded = true)
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
        @Query("fields", encoded = true)
        fields: String,
    ): NetworkIncidentResult

    @TokenAuthenticationHeader
    @ConnectTimeoutHeader("5")
    @ReadTimeoutHeader("10")
    @GET("organizations")
    suspend fun getIncidentOrganizations(
        @Query("incident")
        incidentId: Long,
        @Query("fields", encoded = true)
        fields: String,
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
        @Query("fields", encoded = true)
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
        @Query("fields", encoded = true)
        fields: String,
        @Query("search")
        q: String,
        @Query("limit")
        limit: Int,
    ): NetworkWorksiteLocationSearchResult

    @TokenAuthenticationHeader
    @GET("worksites")
    suspend fun getWorksites(
        @Query("id__in", encoded = true)
        worksiteIds: String,
        @Tag endpointId: EndpointRequestId = EndpointRequestId.Worksites,
    ): NetworkWorksitesFullResult

    @TokenAuthenticationHeader
    @ConnectTimeoutHeader("10")
    @ReadTimeoutHeader("15")
    @GET("worksites")
    suspend fun getWorksite(
        @Query("id__in", encoded = true)
        worksiteId: String,
        @Tag endpointId: EndpointRequestId = EndpointRequestId.Worksite,
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
        @Query("center_coordinates", encoded = true)
        centerCoordinates: String?,
        @Query("updated_at__gt")
        updatedAtAfter: Instant?,
    ): NetworkWorksitesPageResult

    @TokenAuthenticationHeader
    @GET("worksites_page")
    suspend fun getWorksitesPageUpdatedBefore(
        @Query("incident")
        incidentId: Long,
        @Query("limit")
        pageCount: Int,
        @Query("offset")
        offset: Int,
        @Query("updated_at__lt")
        updatedBefore: Instant,
        @Query("sort")
        sort: String,
    ): NetworkWorksitesPageResult

    @TokenAuthenticationHeader
    @GET("worksites_page")
    suspend fun getWorksitesPageUpdatedAfter(
        @Query("incident")
        incidentId: Long,
        @Query("limit")
        pageCount: Int,
        @Query("offset")
        offset: Int,
        @Query("updated_at__gt")
        updatedAfter: Instant,
        @Query("sort")
        sort: String,
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
    @ConnectTimeoutHeader("10")
    @ReadTimeoutHeader("15")
    @GET("worksite_requests")
    suspend fun getWorkTypeRequests(
        @Query("worksite_work_type__worksite")
        id: Long,
    ): NetworkWorkTypeRequestResult

    @TokenAuthenticationHeader
    @GET("organizations")
    suspend fun getNearbyClaimingOrganizations(
        @Query("nearby_claimed", encoded = true)
        nearbyClaimed: String,
    ): NetworkOrganizationsResult

    @TokenAuthenticationHeader
    @GET("users")
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
    @GET("users")
    suspend fun getUsers(
        @Query("id__in", encoded = true)
        ids: String,
    ): NetworkUsersResult

    @TokenAuthenticationHeader
    @GET("organizations")
    suspend fun searchOrganizations(
        @Query("search") q: String,
    ): NetworkOrganizationsSearchResult

    @TokenAuthenticationHeader
    @GET("incident_requests")
    suspend fun getRedeployRequests(): NetworkRedeployRequestsResult

    @TokenAuthenticationHeader
    @GET("worksites_data_flags")
    suspend fun getWorksitesFlagsFormDataBefore(
        @Query("incident")
        incidentId: Long,
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
        @Query("updated_at__lt")
        updatedAtBefore: Instant,
        @Query("sort")
        sort: String,
    ): NetworkFlagsFormDataResult

    @TokenAuthenticationHeader
    @GET("worksites_data_flags")
    suspend fun getWorksitesFlagsFormDataAfter(
        @Query("incident")
        incidentId: Long,
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
        @Query("updated_at__gt")
        updatedAfter: Instant,
        @Query("sort")
        sort: String,
    ): NetworkFlagsFormDataResult

    @TokenAuthenticationHeader
    @GET("worksites_data_flags")
    suspend fun getWorksitesFlagsFormData(
        @Query("id__in", encoded = true)
        ids: String,
    ): NetworkFlagsFormDataResult

    @TokenAuthenticationHeader
    @GET("lists")
    suspend fun getLists(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int?,
    ): NetworkListsResult

    @TokenAuthenticationHeader
    @WrapResponseHeader("list")
    @ThrowClientErrorHeader
    @GET("lists/{listId}")
    suspend fun getList(
        @Path("listId") id: Long,
    ): NetworkListResult

    @TokenAuthenticationHeader
    @GET("teams")
    suspend fun getTeams(
        @Query("incident")
        incidentId: Long?,
        @Query("limit")
        limit: Int,
        @Query("offset")
        offset: Int,
    ): NetworkTeamResult

    @TokenAuthenticationHeader
    @WrapResponseHeader("changes")
    @GET("worksites_changes")
    suspend fun getWorksiteChanges(
        @Query("since")
        after: Instant,
    ): NetworkWorksiteChangesResult

    @GET("portals/current")
    suspend fun getCurrentPortalConfig(): NetworkPortalConfig
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

    override suspend fun getProfileData(accountId: Long) = networkApi.getProfile(accountId)

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

    override suspend fun getIncidentsNoAuth(
        fields: List<String>,
        limit: Int,
        ordering: String,
        after: Instant?,
    ) = networkApi.getIncidentsNoAuth(fields.joinToString(","), limit, ordering, after)
        .let {
            it.errors?.tryThrowException()
            it.results ?: emptyList()
        }

    override suspend fun getIncidentsList(
        fields: List<String>,
        limit: Int,
        ordering: String,
    ) = networkApi.getIncidentsList(fields.joinToString(","), limit, ordering)
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
        fields: List<String>,
        limit: Int,
        offset: Int,
    ) = networkApi.getIncidentOrganizations(
        incidentId,
        fields.joinToString(","),
        limit = limit,
        offset = offset,
    )
        .apply { errors?.tryThrowException() }

    override suspend fun getWorksitesCoreData(incidentId: Long, limit: Int, offset: Int) =
        networkApi.getWorksitesCoreData(incidentId, limit, offset, worksiteCoreDataFieldsQ)
            .apply { errors?.tryThrowException() }
            .results

    override suspend fun getWorksites(worksiteIds: Collection<Long>) =
        networkApi.getWorksites(worksiteIds.joinToString(","))
            .apply { errors?.tryThrowException() }
            .results

    override suspend fun getWorksite(id: Long) = networkApi.getWorksite(id.toString())
        .apply { errors?.tryThrowException() }
        .results?.firstOrNull()

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
    ): NetworkWorksitesPageResult {
        val centerCoordinates = if (latitude == null && longitude == null) {
            null
        } else {
            "$longitude,$latitude"
        }
        return networkApi.getWorksitesPage(
            incidentId,
            pageCount,
            if ((pageOffset ?: 0) <= 1) null else pageOffset,
            centerCoordinates,
            updatedAtAfter,
        ).apply {
            errors?.tryThrowException()
        }
    }

    override suspend fun getWorksitesPageUpdatedAt(
        incidentId: Long,
        pageCount: Int,
        updatedAt: Instant,
        isPagingBackwards: Boolean,
        offset: Int,
    ): NetworkWorksitesPageResult {
        val result = if (isPagingBackwards) {
            networkApi.getWorksitesPageUpdatedBefore(
                incidentId,
                pageCount,
                offset = offset,
                updatedAt,
                "-updated_at",
            )
        } else {
            networkApi.getWorksitesPageUpdatedAfter(
                incidentId,
                pageCount,
                offset = offset,
                updatedAt,
                "updated_at",
            )
        }
        return result.also {
            it.errors?.tryThrowException()
        }
    }

    override suspend fun getWorksitesFlagsFormDataPage(
        incidentId: Long,
        pageCount: Int,
        updatedAt: Instant,
        isPagingBackwards: Boolean,
        offset: Int,
    ): NetworkFlagsFormDataResult {
        val result = if (isPagingBackwards) {
            networkApi.getWorksitesFlagsFormDataBefore(
                incidentId,
                pageCount,
                offset = offset,
                updatedAt,
                "-updated_at",
            )
        } else {
            networkApi.getWorksitesFlagsFormDataAfter(
                incidentId,
                pageCount,
                offset = offset,
                updatedAt,
                "updated_at",
            )
        }
        return result.also {
            it.errors?.tryThrowException()
        }
    }

    override suspend fun getWorksitesFlagsFormData(ids: Collection<Long>) =
        networkApi.getWorksitesFlagsFormData(ids.joinToString(","))
            .let {
                it.errors?.tryThrowException()
                it.results ?: emptyList()
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

    override suspend fun getRequestRedeployIncidentIds() =
        networkApi.getRedeployRequests().results?.map { it.incident }?.toSet() ?: emptySet()

    override suspend fun getLists(limit: Int, offset: Int?) = networkApi.getLists(limit, offset)

    override suspend fun getList(id: Long): NetworkList? {
        val result = networkApi.getList(id)
        result.errors?.tryThrowException()
        return result.list
    }

    override suspend fun getLists(ids: List<Long>): List<NetworkList?> {
        val networkLists = mutableListOf<NetworkList?>()
        for (id in ids) {
            var list: NetworkList? = null
            try {
                list = getList(id)
            } catch (_: Exception) {
            }
            networkLists.add(list)
        }
        return networkLists
    }

    override suspend fun getTeams(incidentId: Long?, limit: Int, offset: Int) =
        networkApi.getTeams(incidentId, limit, offset)

    override suspend fun getWorksiteChanges(after: Instant): List<NetworkWorksiteChange> {
        val result = networkApi.getWorksiteChanges(after)
        result.errors?.tryThrowException()
        result.error?.let { errorMessage ->
            throw Exception(errorMessage)
        }
        return result.changes ?: emptyList()
    }

    override suspend fun getClaimThresholds() = networkApi.getCurrentPortalConfig().attr
}

package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupIncidentApi
import com.crisiscleanup.core.network.model.NetworkIncidentsResult
import retrofit2.Retrofit
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton

private interface IncidentApi {
    @TokenAuthenticationHeader
    @GET("incidents")
    suspend fun getIncidents(
        fields: List<String>,
        limit: Int,
        ordering: String
    ): NetworkIncidentsResult
}

@Singleton
class IncidentApiClient @Inject constructor(
    @CrisisCleanupRetrofit retrofit: Retrofit
) : CrisisCleanupIncidentApi {
    private val networkApi = retrofit.create(IncidentApi::class.java)
    override suspend fun getIncidents(fields: List<String>, limit: Int, ordering: String) =
        networkApi.getIncidents(fields, limit, ordering)
}
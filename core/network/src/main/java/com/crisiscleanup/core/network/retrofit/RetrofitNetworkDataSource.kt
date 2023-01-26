package com.crisiscleanup.core.network.retrofit

import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.model.NetworkIncidentsResult
import retrofit2.Retrofit
import retrofit2.http.GET
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

}

@Singleton
class RetrofitNetworkDataSource @Inject constructor(
    @CrisisCleanupRetrofit retrofit: Retrofit
) : CrisisCleanupNetworkDataSource {
    private val networkApi = retrofit.create(CrisisCleanupNetworkApi::class.java)
    override suspend fun getIncidents(fields: List<String>, limit: Int, ordering: String) =
        networkApi.getIncidents(fields.joinToString(","), limit, ordering)
}
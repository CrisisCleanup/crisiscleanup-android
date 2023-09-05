package com.crisiscleanup.core.network.endoflife

import com.crisiscleanup.core.network.retrofit.RetrofitConfiguration
import com.crisiscleanup.core.network.retrofit.RetrofitConfigurations
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import javax.inject.Inject

interface EndOfLifeClient {
    suspend fun getEarlybirdEndOfLife(): NetworkBuildEndOfLife?
}

private interface EndOfLifeApi {
    @GET
    suspend fun getEarlybirdEndOfLife(
        @Url url: String = "https://android-earlybird-end-5vzbp64o2a-uc.a.run.app",
    ): NetworkBuildEndOfLife
}

class EndOfLifeApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.BasicJson) basicRetrofit: Retrofit,
) : EndOfLifeClient {
    private val endOfLifeApi = basicRetrofit.create(EndOfLifeApi::class.java)

    override suspend fun getEarlybirdEndOfLife() = endOfLifeApi.getEarlybirdEndOfLife()
}

class NoEndOfLifeClient @Inject constructor() : EndOfLifeClient {
    override suspend fun getEarlybirdEndOfLife() = null
}

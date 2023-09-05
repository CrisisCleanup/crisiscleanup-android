package com.crisiscleanup.core.network.appsupport

import com.crisiscleanup.core.network.retrofit.RetrofitConfiguration
import com.crisiscleanup.core.network.retrofit.RetrofitConfigurations
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import javax.inject.Inject

interface AppSupportClient {
    suspend fun getAppSupportInfo(isTest: Boolean): NetworkAppSupportInfo?
}

private interface AppSupportApi {
    @GET
    suspend fun getAppSupportInfo(
        @Url url: String = "https://android-earlybird-end-5vzbp64o2a-uc.a.run.app",
    ): NetworkAppSupportInfo
}

class AppSupportApiClient @Inject constructor(
    @RetrofitConfiguration(RetrofitConfigurations.BasicJson) basicRetrofit: Retrofit,
) : AppSupportClient {
    private val api = basicRetrofit.create(AppSupportApi::class.java)

    override suspend fun getAppSupportInfo(isTest: Boolean) = api.getAppSupportInfo(
        if (isTest) {
            "https://crisis-cleanup-app-support-5vzbp64o2a-uc.a.run.app/min-supported-version/test/android"
        } else {
            "https://crisis-cleanup-app-support-5vzbp64o2a-uc.a.run.app/min-supported-version/android"
        },
    )
}

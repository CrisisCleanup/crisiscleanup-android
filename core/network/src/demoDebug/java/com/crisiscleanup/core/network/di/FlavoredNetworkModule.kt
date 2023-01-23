package com.crisiscleanup.core.network.di

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupIncidentApi
import com.crisiscleanup.core.network.retrofit.AuthApiClient
import com.crisiscleanup.core.network.retrofit.IncidentApiClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FlavoredNetworkModule {

    // Toggle the commented line to use a fake or an actual API
    @Binds
//    fun FakeAuthApi.binds(): CrisisCleanupAuthApi
    fun bindsAuthApiClient(apiClient: AuthApiClient): CrisisCleanupAuthApi

    @Binds
    fun bindsIncidentApiClient(apiClient: IncidentApiClient): CrisisCleanupIncidentApi
}

package com.crisiscleanup.core.network.di

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.CrisisCleanupWriteApi
import com.crisiscleanup.core.network.endoflife.EndOfLifeClient
import com.crisiscleanup.core.network.endoflife.NoEndOfLifeClient
import com.crisiscleanup.core.network.retrofit.AuthApiClient
import com.crisiscleanup.core.network.retrofit.DataApiClient
import com.crisiscleanup.core.network.retrofit.WriteApiClient
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
    fun bindsDataApiClient(apiClient: DataApiClient): CrisisCleanupNetworkDataSource

    @Binds
    fun bindsWriteApiClient(apiClient: WriteApiClient): CrisisCleanupWriteApi

    @Binds
    fun bindsEndOfLifeClient(apiClient: NoEndOfLifeClient): EndOfLifeClient
}

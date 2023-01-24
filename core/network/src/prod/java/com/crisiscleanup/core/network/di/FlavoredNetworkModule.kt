package com.crisiscleanup.core.network.di

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.CrisisCleanupNetworkDataSource
import com.crisiscleanup.core.network.retrofit.AuthApiClient
import com.crisiscleanup.core.network.retrofit.RetrofitNetworkDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FlavoredNetworkModule {

    @Binds
    fun bindsAuthApiClient(apiClient: AuthApiClient): CrisisCleanupAuthApi

    @Binds
    fun bindsNetworkDataSource(apiClient: RetrofitNetworkDataSource): CrisisCleanupNetworkDataSource
}
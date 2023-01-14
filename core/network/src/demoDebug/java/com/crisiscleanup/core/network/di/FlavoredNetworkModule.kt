package com.crisiscleanup.core.network.di

import com.crisiscleanup.core.network.CrisisCleanupAuthApi
import com.crisiscleanup.core.network.retrofit.AuthApiClient
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
    fun AuthApiClient.binds(): CrisisCleanupAuthApi
}

package com.crisiscleanup.core.network.di

import com.crisiscleanup.core.network.endoflife.EndOfLifeApiClient
import com.crisiscleanup.core.network.endoflife.EndOfLifeClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface FlavoredNetworkModule {
    @Binds
    fun bindsEndOfLifeClient(apiClient: EndOfLifeApiClient): EndOfLifeClient
}

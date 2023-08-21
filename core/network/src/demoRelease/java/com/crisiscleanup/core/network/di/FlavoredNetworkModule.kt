package com.crisiscleanup.core.network.di

import com.crisiscleanup.core.network.endoflife.EndOfLifeClient
import com.crisiscleanup.core.network.endoflife.NoEndOfLifeClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DemoReleaseFlavoredNetworkModule {
    @Binds
    fun bindsEndOfLifeClient(apiClient: NoEndOfLifeClient): EndOfLifeClient
}

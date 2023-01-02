package com.crisiscleanup.core.data.di

import com.crisiscleanup.core.data.repository.OfflineFirstUserDataRepository
import com.crisiscleanup.core.data.repository.UserDataRepository
import com.crisiscleanup.core.data.util.ConnectivityManagerNetworkMonitor
import com.crisiscleanup.core.data.util.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsUserDataRepository(
        userDataRepository: OfflineFirstUserDataRepository
    ): UserDataRepository

    @Binds
    fun bindsNetworkMonitor(
        networkMonitor: ConnectivityManagerNetworkMonitor
    ): NetworkMonitor
}

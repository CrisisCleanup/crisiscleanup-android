package com.crisiscleanup.core.data.test

import com.crisiscleanup.core.common.NetworkMonitor
import com.crisiscleanup.core.data.di.DataModule
import com.crisiscleanup.core.data.repository.AccountDataRepository
import com.crisiscleanup.core.data.repository.LocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.OfflineFirstLocalAppPreferencesRepository
import com.crisiscleanup.core.data.repository.fake.FakeAccountRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class],
)
interface TestDataModule {
    @Binds
    fun bindsLocalAppPreferencesRepository(
        localAppPreferencesRepository: OfflineFirstLocalAppPreferencesRepository,
    ): LocalAppPreferencesRepository

    @Binds
    fun bindsAccountDataRepository(
        repository: FakeAccountRepository,
    ): AccountDataRepository

    @Binds
    fun bindsNetworkMonitor(
        networkMonitor: AlwaysOnlineNetworkMonitor,
    ): NetworkMonitor
}

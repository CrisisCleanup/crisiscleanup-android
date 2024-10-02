package com.crisiscleanup.core.data.di

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.common.PermissionManager
import com.crisiscleanup.core.common.di.ApplicationScope
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.data.repository.CasesFilterRepository
import com.crisiscleanup.core.data.repository.CrisisCleanupCasesFilterRepository
import com.crisiscleanup.core.datastore.CasesFiltersDataSource
import com.crisiscleanup.core.datastore.LocalPersistedCasesFilters
import com.crisiscleanup.core.datastore.di.LocalCasesFilterType
import com.crisiscleanup.core.datastore.di.LocalCasesFilterTypes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CasesFilterModule {
    @Singleton
    @Provides
    @CasesFilterType(CasesFilterTypes.Cases)
    fun providesCasesFiltersRepository(
        @LocalCasesFilterType(LocalCasesFilterTypes.Cases)
        dataStore: DataStore<LocalPersistedCasesFilters>,
        permissionManager: PermissionManager,
        @ApplicationScope
        externalScope: CoroutineScope,
        @Dispatcher(CrisisCleanupDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ): CasesFilterRepository = CrisisCleanupCasesFilterRepository(
        CasesFiltersDataSource(dataStore),
        permissionManager,
        externalScope,
        ioDispatcher,
    )

    @Singleton
    @Provides
    @CasesFilterType(CasesFilterTypes.TeamCases)
    fun providesTeamCasesFiltersRepository(
        @LocalCasesFilterType(LocalCasesFilterTypes.TeamCases)
        dataStore: DataStore<LocalPersistedCasesFilters>,
        permissionManager: PermissionManager,
        @ApplicationScope
        externalScope: CoroutineScope,
        @Dispatcher(CrisisCleanupDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    ): CasesFilterRepository = CrisisCleanupCasesFilterRepository(
        CasesFiltersDataSource(dataStore),
        permissionManager,
        externalScope,
        ioDispatcher,
    )
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class CasesFilterType(val type: CasesFilterTypes)

enum class CasesFilterTypes {
    Cases,
    TeamCases,
}

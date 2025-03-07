package com.crisiscleanup.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.crisiscleanup.core.common.network.CrisisCleanupDispatchers.IO
import com.crisiscleanup.core.common.network.Dispatcher
import com.crisiscleanup.core.datastore.AccountInfoProtoSerializer
import com.crisiscleanup.core.datastore.AppMetricsSerializer
import com.crisiscleanup.core.datastore.CasesFiltersProtoSerializer
import com.crisiscleanup.core.datastore.IncidentCachePreferencesSerializer
import com.crisiscleanup.core.datastore.UserPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        userPreferencesSerializer: UserPreferencesSerializer,
    ) = DataStoreFactory.create(
        serializer = userPreferencesSerializer,
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
    ) {
        context.dataStoreFile("user_preferences.pb")
    }

    @Provides
    @Singleton
    fun providesAppMetricsDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        appMetricsSerializer: AppMetricsSerializer,
    ) = DataStoreFactory.create(
        serializer = appMetricsSerializer,
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
    ) {
        context.dataStoreFile("app_metrics.pb")
    }

    @Provides
    @Singleton
    fun providesAccountInfoProtoDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        serializer: AccountInfoProtoSerializer,
    ) = DataStoreFactory.create(
        serializer = serializer,
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
    ) {
        context.dataStoreFile("account_info.pb")
    }

    @Provides
    @Singleton
    fun providesCasesFiltersProtoDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        serializer: CasesFiltersProtoSerializer,
    ) = DataStoreFactory.create(
        serializer = serializer,
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
    ) {
        context.dataStoreFile("local_persisted_cases_filters.pb")
    }

    @Provides
    @Singleton
    fun providesIncidentCachePreferencesDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        serializer: IncidentCachePreferencesSerializer,
    ) = DataStoreFactory.create(
        serializer = serializer,
        scope = CoroutineScope(ioDispatcher + SupervisorJob()),
    ) {
        context.dataStoreFile("incident_cache_preferences.pb")
    }
}

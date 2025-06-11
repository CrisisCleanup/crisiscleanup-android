package com.crisiscleanup.core.datastore.test

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.crisiscleanup.core.datastore.AccountInfo
import com.crisiscleanup.core.datastore.AccountInfoProtoSerializer
import com.crisiscleanup.core.datastore.AppMetrics
import com.crisiscleanup.core.datastore.AppMetricsSerializer
import com.crisiscleanup.core.datastore.UserPreferences
import com.crisiscleanup.core.datastore.UserPreferencesSerializer
import com.crisiscleanup.core.datastore.di.DataStoreModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.junit.rules.TemporaryFolder
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataStoreModule::class],
)
object TestDataStoreModule {
    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(
        serializer: UserPreferencesSerializer,
        tmpFolder: TemporaryFolder,
    ): DataStore<UserPreferences> =
        tmpFolder.testUserPreferencesDataStore(serializer)

    @Provides
    @Singleton
    fun providesAccountInfoDataStore(
        serializer: AccountInfoProtoSerializer,
        tmpFolder: TemporaryFolder,
    ): DataStore<AccountInfo> = tmpFolder.testAccountInfoDataStore(serializer)

    @Provides
    @Singleton
    fun providesAppMetricsDataStore(
        serializer: AppMetricsSerializer,
        tmpFolder: TemporaryFolder,
    ): DataStore<AppMetrics> = tmpFolder.testAppMetricsDataStore(serializer)
}

fun TemporaryFolder.testUserPreferencesDataStore(
    serializer: UserPreferencesSerializer = UserPreferencesSerializer(),
) = DataStoreFactory.create(
    serializer = serializer,
) {
    newFile("user_preferences_test.pb")
}

fun TemporaryFolder.testAccountInfoDataStore(
    serializer: AccountInfoProtoSerializer = AccountInfoProtoSerializer(),
) = DataStoreFactory.create(
    serializer = serializer,
) {
    newFile("account_info_test.pb")
}

fun TemporaryFolder.testAppMetricsDataStore(
    serializer: AppMetricsSerializer = AppMetricsSerializer(),
) = DataStoreFactory.create(
    serializer = serializer,
) {
    newFile("app_metrics_test.pb")
}

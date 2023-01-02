package com.crisiscleanup.core.datastore.test

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
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
    replaces = [DataStoreModule::class]
)
object TestDataStoreModule {

    @Provides
    @Singleton
    fun providesUserPreferencesDataStore(
        userPreferencesSerializer: UserPreferencesSerializer,
        tmpFolder: TemporaryFolder
    ): DataStore<UserPreferences> =
        tmpFolder.testUserPreferencesDataStore(userPreferencesSerializer)
}

fun TemporaryFolder.testUserPreferencesDataStore(
    userPreferencesSerializer: UserPreferencesSerializer = UserPreferencesSerializer()
) = DataStoreFactory.create(
    serializer = userPreferencesSerializer,
) {
    newFile("user_preferences_test.pb")
}

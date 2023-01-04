package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.datastore.CrisisCleanupPreferencesDataSource
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class OfflineFirstUserDataRepository @Inject constructor(
    private val crisisCleanupPreferencesDataSource: CrisisCleanupPreferencesDataSource
) : UserDataRepository {

    override val userData: Flow<UserData> =
        crisisCleanupPreferencesDataSource.userData

    override suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) =
        crisisCleanupPreferencesDataSource.setShouldHideOnboarding(shouldHideOnboarding)
}

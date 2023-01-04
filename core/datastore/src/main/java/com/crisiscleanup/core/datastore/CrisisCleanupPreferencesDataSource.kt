package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CrisisCleanupPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>
) {

    val userData = userPreferences.data
        .map {
            UserData(
                shouldHideOnboarding = it.shouldHideOnboarding
            )
        }

    suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {
        userPreferences.updateData {
            it.copy {
                this.shouldHideOnboarding = shouldHideOnboarding
            }
        }
    }
}
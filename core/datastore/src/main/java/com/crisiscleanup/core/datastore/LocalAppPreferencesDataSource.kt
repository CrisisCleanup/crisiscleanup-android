package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * Stores data and preferences related to the local app (on each device)
 */
class LocalAppPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>
) {
    val userData = userPreferences.data
        .map {
            UserData(
                darkThemeConfig = when (it.darkThemeConfig) {
                    null,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_UNSPECIFIED,
                    DarkThemeConfigProto.UNRECOGNIZED,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM ->
                        DarkThemeConfig.FOLLOW_SYSTEM

                    DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT ->
                        DarkThemeConfig.LIGHT

                    DarkThemeConfigProto.DARK_THEME_CONFIG_DARK -> DarkThemeConfig.DARK
                },
                shouldHideOnboarding = it.shouldHideOnboarding,

                syncAttempt = SyncAttempt(
                    it.syncAttempt.successfulSeconds,
                    it.syncAttempt.attemptedSeconds,
                    it.syncAttempt.attemptedCounter,
                ),

                selectedIncidentId = it.selectedIncidentId,
            )
        }

    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        userPreferences.updateData {
            it.copy {
                this.darkThemeConfig = when (darkThemeConfig) {
                    DarkThemeConfig.FOLLOW_SYSTEM ->
                        DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM

                    DarkThemeConfig.LIGHT -> DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT
                    DarkThemeConfig.DARK -> DarkThemeConfigProto.DARK_THEME_CONFIG_DARK
                }
            }
        }
    }

    suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean) {
        userPreferences.updateData {
            it.copy {
                this.shouldHideOnboarding = shouldHideOnboarding
            }
        }
    }

    suspend fun setSyncAttempt(
        isSuccessful: Boolean,
        attemptedSeconds: Long = Clock.System.now().epochSeconds,
    ) {
        userPreferences.updateData {
            val builder = SyncAttemptProto.newBuilder(it.syncAttempt)
            if (isSuccessful) {
                builder.successfulSeconds = attemptedSeconds
                builder.attemptedCounter = 0
            } else {
                builder.attemptedCounter += 1
            }
            builder.attemptedSeconds = attemptedSeconds
            val attempt = builder.build()

            it.copy {
                this.syncAttempt = attempt
            }
        }
    }

    suspend fun setSelectedIncident(id: Long) {
        userPreferences.updateData {
            it.copy { selectedIncidentId = id }
        }
    }
}
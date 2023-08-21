package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.SyncAttempt
import com.crisiscleanup.core.model.data.UserData
import com.crisiscleanup.core.model.data.WorksiteSortBy
import com.crisiscleanup.core.model.data.worksiteSortByFromLiteral
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject

/**
 * Stores data and preferences related to the local app (on each device)
 */
class LocalAppPreferencesDataSource @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
) {
    val userData = userPreferences.data
        .map {
            UserData(
                darkThemeConfig = when (it.darkThemeConfig) {
                    null,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_UNSPECIFIED,
                    DarkThemeConfigProto.UNRECOGNIZED,
                    DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM,
                    ->
                        DarkThemeConfig.FOLLOW_SYSTEM

                    DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT ->
                        DarkThemeConfig.LIGHT

                    DarkThemeConfigProto.DARK_THEME_CONFIG_DARK -> DarkThemeConfig.DARK
                },
                shouldHideOnboarding = it.shouldHideOnboarding,

                saveCredentialsPromptCount = it.saveCredentialsPromptCount,
                disableSaveCredentialsPrompt = it.disableSaveCredentialsPrompt,

                syncAttempt = SyncAttempt(
                    it.syncAttempt.successfulSeconds,
                    it.syncAttempt.attemptedSeconds,
                    it.syncAttempt.attemptedCounter,
                ),

                selectedIncidentId = it.selectedIncidentId,

                languageKey = it.languageKey,

                tableViewSortBy = worksiteSortByFromLiteral(it.tableViewSortBy),

                allowAllAnalytics = it.allowAllAnalytics,
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
                builder.attemptedCounter++
            }
            builder.attemptedSeconds = attemptedSeconds
            val attempt = builder.build()

            it.copy {
                syncAttempt = attempt
            }
        }
    }

    suspend fun incrementSaveCredentialsPrompt() {
        userPreferences.updateData {
            it.copy { saveCredentialsPromptCount++ }
        }
    }

    suspend fun setDisableSaveCredentialsPrompt(disable: Boolean) {
        userPreferences.updateData {
            it.copy { disableSaveCredentialsPrompt = disable }
        }
    }

    suspend fun clearSyncData() {
        userPreferences.updateData {
            it.copy {
                syncAttempt = SyncAttemptProto.newBuilder().build()
            }
        }
    }

    suspend fun setSelectedIncident(id: Long) {
        userPreferences.updateData {
            it.copy { selectedIncidentId = id }
        }
    }

    suspend fun setLanguageKey(key: String) {
        userPreferences.updateData {
            it.copy { languageKey = key }
        }
    }

    suspend fun setTableViewSortBy(sortBy: WorksiteSortBy) {
        userPreferences.updateData {
            it.copy { tableViewSortBy = sortBy.literal }
        }
    }

    suspend fun setAnalytics(allowAll: Boolean) {
        userPreferences.updateData {
            it.copy { allowAllAnalytics = allowAll }
        }
    }
}

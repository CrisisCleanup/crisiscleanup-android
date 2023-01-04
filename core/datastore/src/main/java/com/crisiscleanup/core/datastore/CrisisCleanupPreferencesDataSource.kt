package com.crisiscleanup.core.datastore

import androidx.datastore.core.DataStore
import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CrisisCleanupPreferencesDataSource @Inject constructor(
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
                shouldHideOnboarding = it.shouldHideOnboarding
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
}
package com.crisiscleanup.core.data.repository

import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.UserData
import com.crisiscleanup.core.model.data.WorksiteSortBy
import kotlinx.coroutines.flow.Flow

interface LocalAppPreferencesRepository {
    val userPreferences: Flow<UserData>

    /**
     * Sets the desired dark theme config.
     */
    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig)

    /**
     * Sets whether the user has completed the onboarding process.
     */
    suspend fun setShouldHideOnboarding(shouldHideOnboarding: Boolean)

    suspend fun setHideGettingStartedVideo(hide: Boolean)

    suspend fun setMenuTutorialDone(isDone: Boolean)

    /**
     * Caches ID of selected incident.
     */
    suspend fun setSelectedIncident(id: Long)

    suspend fun setLanguageKey(key: String)

    suspend fun setTableViewSortBy(sortBy: WorksiteSortBy)

    suspend fun setAnalytics(allowAll: Boolean)

    suspend fun setShareLocationWithOrg(share: Boolean)
}

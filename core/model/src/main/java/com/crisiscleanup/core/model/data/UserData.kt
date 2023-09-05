package com.crisiscleanup.core.model.data

/**
 * Class summarizing local activity and preferences data
 */
// Named UserData originally so sticking with it rather than renaming to (Local)AppPreferences.
data class UserData(
    val darkThemeConfig: DarkThemeConfig,
    val shouldHideOnboarding: Boolean,

    val syncAttempt: SyncAttempt,

    val selectedIncidentId: Long,

    val languageKey: String,

    val tableViewSortBy: WorksiteSortBy,

    val allowAllAnalytics: Boolean,
)

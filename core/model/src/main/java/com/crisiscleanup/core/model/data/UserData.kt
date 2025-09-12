package com.crisiscleanup.core.model.data

/**
 * Class summarizing local activity and preferences data
 */
// Named UserData originally so sticking with it rather than renaming to (Local)AppPreferences.
data class UserData(
    val darkThemeConfig: DarkThemeConfig,
    val shouldHideOnboarding: Boolean,

    val selectedIncidentId: Long,

    val languageKey: String,

    val tableViewSortBy: WorksiteSortBy,

    val allowAllAnalytics: Boolean,

    val hideGettingStartedVideo: Boolean,
    val isMenuTutorialDone: Boolean,

    val shareLocationWithOrg: Boolean,

    val casesMapBounds: IncidentCoordinateBounds,
    val teamMapBounds: IncidentCoordinateBounds,

    val isWorkScreenTableView: Boolean,

    val isSyncMediaImmediate: Boolean,
)

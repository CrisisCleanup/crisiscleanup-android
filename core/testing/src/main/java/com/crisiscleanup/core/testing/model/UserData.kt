package com.crisiscleanup.core.testing.model

import com.crisiscleanup.core.model.data.DarkThemeConfig
import com.crisiscleanup.core.model.data.EmptyIncident
import com.crisiscleanup.core.model.data.IncidentCoordinateBoundsNone
import com.crisiscleanup.core.model.data.UserData
import com.crisiscleanup.core.model.data.WorksiteSortBy

val UserDataNone = UserData(
    darkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    shouldHideOnboarding = false,
    selectedIncidentId = EmptyIncident.id,
    languageKey = "",
    tableViewSortBy = WorksiteSortBy.None,
    allowAllAnalytics = false,
    hideGettingStartedVideo = false,
    isMenuTutorialDone = false,
    shareLocationWithOrg = false,
    casesMapBounds = IncidentCoordinateBoundsNone,
    teamMapBounds = IncidentCoordinateBoundsNone,
    isWorkScreenTableView = false,
    isSyncMediaImmediate = false,
)

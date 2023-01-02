package com.crisiscleanup.core.model.data

/**
 * Class summarizing user interest data
 */
data class UserData(
    val themeBrand: ThemeBrand,
    val darkThemeConfig: DarkThemeConfig,
    val shouldHideOnboarding: Boolean
)

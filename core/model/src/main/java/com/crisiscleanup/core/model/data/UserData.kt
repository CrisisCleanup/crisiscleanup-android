package com.crisiscleanup.core.model.data

/**
 * Class summarizing user activity and local preferences data
 */
data class UserData(
    val darkThemeConfig: DarkThemeConfig,
    val shouldHideOnboarding: Boolean
)

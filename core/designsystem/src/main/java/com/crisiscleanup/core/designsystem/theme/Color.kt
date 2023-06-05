package com.crisiscleanup.core.designsystem.theme

import androidx.compose.ui.graphics.Color

internal val md_theme_primary = Color(0xFF2D2D2D) // Button icon, control activated, control action
internal val md_theme_onPrimary = Color(0xFFFFFFFF) // Check in checkbox
internal val md_theme_primaryContainer = Color(0xFFFECE09)
internal val md_theme_onPrimaryContainer = Color(0xFF2D2D2D)
internal val md_theme_secondary = Color(0xFF52DBCB)
internal val md_theme_onSecondary = Color(0xFF003732)
internal val md_theme_secondaryContainer = Color(0xFF005049)
internal val md_theme_onSecondaryContainer = Color(0xFF73F8E7)
internal val md_theme_tertiary = Color(0xFFFFB86F)
internal val md_theme_onTertiary = Color(0xFF4A2800)
internal val md_theme_tertiaryContainer = Color(0xFF693C00)
internal val md_theme_onTertiaryContainer = Color(0xFFFFDCBE)
internal val md_theme_error = Color(0xFFED4747)
internal val md_theme_errorContainer = Color(0xFF93000A)
internal val md_theme_onError = Color(0xFF690005)
internal val md_theme_onErrorContainer = Color(0xFFFFDAD6)
internal val md_theme_background = Color(0xFFF6F8F9)
internal val md_theme_onBackground = Color(0xFF2D2D2D)
internal val md_theme_surface = Color(0xFFFFFFFF)
internal val md_theme_onSurface = Color(0xFF2D2D2D)
internal val md_theme_surfaceVariant = Color(0xFF4C4639)
internal val md_theme_onSurfaceVariant = Color(0xFF818181) // Hints
internal val md_theme_outline = Color(0xFFDADADA) // Outline
internal val md_theme_inverseOnSurface = Color(0xFFFFFFFF)
internal val md_theme_inverseSurface = Color(0xFF2D2D2D) // Snackbar background
internal val md_theme_inversePrimary = Color(0xFFFECE09)
internal val md_theme_shadow = Color(0xFF000000)
internal val md_theme_surfaceTint = Color(0xFFEFC100)
internal val md_theme_surfaceTintColor = Color(0xFFEFC100)

val primaryBlueColor = Color(0xFF009BFF)
val primaryBlueOneTenthColor = primaryBlueColor.copy(alpha = 0.1f)
val primaryRedColor = Color(0xFFED4747)
val primaryOrangeColor = Color(0xFFF79820)
val incidentDisasterContainerColor = primaryBlueColor
val incidentDisasterContentColor = Color(0xFFFFFFFF)
val attentionBackgroundColor = md_theme_primaryContainer
val cancelButtonContainerColor = Color(0xFFEAEAEA)
val cancelButtonContentColor = md_theme_primary
val actionLinkColor = primaryBlueColor
val separatorColor = Color(0xFFF6F8F9)
val selectedOptionContainerColor = Color(0xFFF6F8F9)
val neutralIconColor = Color(0xFF848F99)
val navigationContainerColor = Color(0xFF2D2D2D)

// Mirror FilledButtonTokens.class
private const val disabledAlpha = 0.38f
fun Color.disabledAlpha() = copy(alpha = disabledAlpha)
val disabledButtonContentColor = md_theme_onSurface.disabledAlpha()

const val statusUnknownColorCode = 0xFF000000
const val statusUnclaimedColorCode = 0xFFD0021B
const val statusNotStartedColorCode = 0xFFFAB92E
const val statusInProgressColorCode = 0xFFF0F032
const val statusPartiallyCompletedColorCode = 0xFF0054BB
const val statusNeedsFollowUpColorCode = 0xFFEA51EB
const val statusCompletedColorCode = 0xFF0fa355
const val statusDoneByOthersNhwColorCode = 0xFF82D78C
const val statusOutOfScopeRejectedColorCode = 0xFF1D1D1D
const val statusUnresponsiveColorCode = 0xFF787878
const val statusDuplicateUnclaimedColorCode = 0xFF7F7F7F
const val statusDuplicateClaimedColorCode = 0xFF82D78C
val statusUnknownColor = Color(statusUnknownColorCode)
val statusUnclaimedColor = Color(statusUnclaimedColorCode)
val statusNotStartedColor = Color(statusNotStartedColorCode)
val statusInProgressColor = Color(statusInProgressColorCode)
val statusPartiallyCompletedColor = Color(statusPartiallyCompletedColorCode)
val statusNeedsFollowUpColor = Color(statusNeedsFollowUpColorCode)
val statusCompletedColor = Color(statusCompletedColorCode)
val statusDoneByOthersNhwDiColor = Color(statusDoneByOthersNhwColorCode)
val statusOutOfScopeRejectedColor = Color(statusOutOfScopeRejectedColorCode)
val statusUnresponsiveColor = Color(statusUnresponsiveColorCode)

internal val md_theme_light_primary = Color(0xFF735C00)
internal val md_theme_light_onPrimary = Color(0xFFFFFFFF)
internal val md_theme_light_primaryContainer = Color(0xFFFFE085)
internal val md_theme_light_onPrimaryContainer = Color(0xFF231B00)
internal val md_theme_light_secondary = Color(0xFF006A61)
internal val md_theme_light_onSecondary = Color(0xFFFFFFFF)
internal val md_theme_light_secondaryContainer = Color(0xFF73F8E7)
internal val md_theme_light_onSecondaryContainer = Color(0xFF00201D)
internal val md_theme_light_tertiary = Color(0xFF8A5100)
internal val md_theme_light_onTertiary = Color(0xFFFFFFFF)
internal val md_theme_light_tertiaryContainer = Color(0xFFFFDCBE)
internal val md_theme_light_onTertiaryContainer = Color(0xFF2C1600)
internal val md_theme_light_error = Color(0xFFBA1A1A)
internal val md_theme_light_errorContainer = Color(0xFFFFDAD6)
internal val md_theme_light_onError = Color(0xFFFFFFFF)
internal val md_theme_light_onErrorContainer = Color(0xFF410002)
internal val md_theme_light_background = Color(0xFFFFFBFF)
internal val md_theme_light_onBackground = Color(0xFF1E1B16)
internal val md_theme_light_surface = Color(0xFFFFFBFF)
internal val md_theme_light_onSurface = Color(0xFF1E1B16)
internal val md_theme_light_surfaceVariant = Color(0xFFEBE2CF)
internal val md_theme_light_onSurfaceVariant = Color(0xFF4C4639)
internal val md_theme_light_outline = Color(0xFF7D7667)
internal val md_theme_light_inverseOnSurface = Color(0xFFF7F0E7)
internal val md_theme_light_inverseSurface = Color(0xFF33302A)
internal val md_theme_light_inversePrimary = Color(0xFFEFC100)
internal val md_theme_light_shadow = Color(0xFF000000)
internal val md_theme_light_surfaceTint = Color(0xFF735C00)
internal val md_theme_light_surfaceTintColor = Color(0xFF735C00)

internal val md_theme_dark_primary = Color(0xFFEFC100)
internal val md_theme_dark_onPrimary = Color(0xFF3C2F00)
internal val md_theme_dark_primaryContainer = Color(0xFF574500)
internal val md_theme_dark_onPrimaryContainer = Color(0xFFFFE085)
internal val md_theme_dark_secondary = Color(0xFF52DBCB)
internal val md_theme_dark_onSecondary = Color(0xFF003732)
internal val md_theme_dark_secondaryContainer = Color(0xFF005049)
internal val md_theme_dark_onSecondaryContainer = Color(0xFF73F8E7)
internal val md_theme_dark_tertiary = Color(0xFFFFB86F)
internal val md_theme_dark_onTertiary = Color(0xFF4A2800)
internal val md_theme_dark_tertiaryContainer = Color(0xFF693C00)
internal val md_theme_dark_onTertiaryContainer = Color(0xFFFFDCBE)
internal val md_theme_dark_error = Color(0xFFFFB4AB)
internal val md_theme_dark_errorContainer = Color(0xFF93000A)
internal val md_theme_dark_onError = Color(0xFF690005)
internal val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
internal val md_theme_dark_background = Color(0xFF1E1B16)
internal val md_theme_dark_onBackground = Color(0xFFE8E2D9)
internal val md_theme_dark_surface = Color(0xFF1E1B16)
internal val md_theme_dark_onSurface = Color(0xFFE8E2D9)
internal val md_theme_dark_surfaceVariant = Color(0xFF4C4639)
internal val md_theme_dark_onSurfaceVariant = Color(0xFFCEC6B4)
internal val md_theme_dark_outline = Color(0xFF979080)
internal val md_theme_dark_inverseOnSurface = Color(0xFF1E1B16)
internal val md_theme_dark_inverseSurface = Color(0xFFE8E2D9)
internal val md_theme_dark_inversePrimary = Color(0xFF735C00)
internal val md_theme_dark_shadow = Color(0xFF000000)
internal val md_theme_dark_surfaceTint = Color(0xFFEFC100)
internal val md_theme_dark_surfaceTintColor = Color(0xFFEFC100)

internal val seed = Color(0xFFFECE09)

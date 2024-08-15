package com.crisiscleanup.core.appcomponent.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.appcomponent.AppTopBarDataProvider
import com.crisiscleanup.core.commoncase.ui.IncidentDropdownSelect
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TopAppBarDefault
import com.crisiscleanup.core.designsystem.component.TruncatedAppBarText
import com.crisiscleanup.core.designsystem.icon.CrisisCleanupIcons

@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    incidentDropdownModifier: Modifier = Modifier,
    accountToggleModifier: Modifier = Modifier,
    dataProvider: AppTopBarDataProvider,
    openAuthentication: () -> Unit = {},
    onOpenIncidents: (() -> Unit)? = null,
) {
    val screenTitle by dataProvider.screenTitle.collectAsStateWithLifecycle()
    val isHeaderLoading by dataProvider.showHeaderLoading.collectAsState(false)

    val disasterIconResId by dataProvider.disasterIconResId.collectAsStateWithLifecycle()

    val isAccountExpired by dataProvider.isAccountExpired.collectAsStateWithLifecycle()
    val profilePictureUri by dataProvider.profilePictureUri.collectAsStateWithLifecycle()

    AppTopBar(
        modifier = modifier,
        incidentDropdownModifier = incidentDropdownModifier,
        accountToggleModifier = accountToggleModifier,
        title = screenTitle,
        isAppHeaderLoading = isHeaderLoading,
        profilePictureUri = profilePictureUri,
        isAccountExpired = isAccountExpired,
        openAuthentication = openAuthentication,
        disasterIconResId = disasterIconResId,
        onOpenIncidents = onOpenIncidents,
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
)
@Composable
internal fun AppTopBar(
    modifier: Modifier = Modifier,
    incidentDropdownModifier: Modifier = Modifier,
    accountToggleModifier: Modifier = Modifier,
    title: String = "",
    isAppHeaderLoading: Boolean = false,
    profilePictureUri: String = "",
    isAccountExpired: Boolean = false,
    openAuthentication: () -> Unit = {},
    @DrawableRes disasterIconResId: Int = 0,
    onOpenIncidents: (() -> Unit)? = null,
) {
    val t = LocalAppTranslator.current
    val actionText = t("actions.account")
    TopAppBarDefault(
        modifier = modifier,
        accountToggleModifier = accountToggleModifier,
        title = title,
        profilePictureUri = profilePictureUri,
        actionIcon = CrisisCleanupIcons.Account,
        actionText = actionText,
        isActionAttention = isAccountExpired,
        onActionClick = openAuthentication,
        onNavigationClick = null,
        titleContent = @Composable {
            // TODO Match height of visible part of app bar (not the entire app bar)
            if (onOpenIncidents == null) {
                TruncatedAppBarText(title = title)
            } else {
                IncidentDropdownSelect(
                    modifier = incidentDropdownModifier.testTag("appIncidentSelector"),
                    onOpenIncidents,
                    disasterIconResId,
                    title = title,
                    contentDescription = t("nav.change_incident"),
                    isLoading = isAppHeaderLoading,
                )
            }
        },
    )
}

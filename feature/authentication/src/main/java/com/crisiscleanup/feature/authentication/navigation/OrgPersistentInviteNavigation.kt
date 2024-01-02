package com.crisiscleanup.feature.authentication.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.crisiscleanup.core.appnav.RouteConstant.orgPersistentInviteRoute
import com.crisiscleanup.core.common.event.UserPersistentInvite
import com.crisiscleanup.feature.authentication.ui.OrgPersistentInviteRoute

internal const val inviteTokenArg = "inviteToken"
internal const val inviteUserIdArg = "inviteUserId"

internal class OrgPersistentInviteArgs(
    val inviteToken: String,
    val inviteUserId: Long,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<String>(inviteTokenArg)),
        checkNotNull(savedStateHandle.get<Long>(showEmailInputArg)),
    )
}

fun NavController.navigateToOrgPersistentInvite(invite: UserPersistentInvite) {
    val query = "$inviteTokenArg=${invite.inviteToken}&$inviteUserIdArg=${invite.inviterUserId}"
    val route = "$orgPersistentInviteRoute?$query"
    navigate(route)
}

fun NavGraphBuilder.orgPersistentInviteScreen(
    onBack: () -> Unit,
) {
    composable(
        route = "$orgPersistentInviteRoute?$inviteTokenArg={$inviteTokenArg}&$inviteUserIdArg={$inviteUserIdArg}",
        arguments = listOf(
            navArgument(inviteTokenArg) {},
            navArgument(inviteUserIdArg) {},
        ),
    ) {
        OrgPersistentInviteRoute(
            onBack = onBack,
        )
    }
}

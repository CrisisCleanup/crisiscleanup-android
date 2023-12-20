package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.feature.authentication.RequestOrgAccessViewModel

@Composable
fun RequestOrgAccessRoute(
    onBack: () -> Unit,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    val clearStateOnBack = remember(onBack, viewModel) {
        {
            viewModel.clearInviteCode()
            onBack()
        }
    }
    // TODO Backing out does nothing when directed from paste invite link
    BackHandler {
        clearStateOnBack()
    }

    Text("Org invite")
}

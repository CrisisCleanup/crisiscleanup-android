package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.feature.authentication.RequestOrgAccessViewModel

@Composable
fun RequestOrgAccessRoute(
    onBack: () -> Unit,
    viewModel: RequestOrgAccessViewModel = hiltViewModel(),
) {
    // TODO Backing out does nothing when directed from paste invite link
    BackHandler {
        onBack()
    }

    Text("Org invite")
}
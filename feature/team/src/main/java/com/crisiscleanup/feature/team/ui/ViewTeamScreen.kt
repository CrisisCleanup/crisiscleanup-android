package com.crisiscleanup.feature.team.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.crisiscleanup.feature.team.ViewTeamViewModel

@Composable
fun ViewTeamRoute(
    onBack: () -> Unit,
) {
    ViewTeamScreen(
        onBack,
    )
}

@Composable
private fun ViewTeamScreen(
    onBack: () -> Unit,
    viewModel: ViewTeamViewModel = hiltViewModel(),
) {
    Text("Team ${viewModel.teamId}")
}

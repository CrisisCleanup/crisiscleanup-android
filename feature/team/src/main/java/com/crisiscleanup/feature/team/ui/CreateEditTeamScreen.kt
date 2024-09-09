package com.crisiscleanup.feature.team.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.feature.team.CreateEditTeamViewModel

@Composable
fun CreateEditTeamRoute(
    onBack: () -> Unit,
    viewModel: CreateEditTeamViewModel = hiltViewModel(),
) {
    Text("Create/edit team ${viewModel.startingEditorStep}")
}

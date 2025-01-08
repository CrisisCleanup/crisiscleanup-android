package com.crisiscleanup.feature.team.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crisiscleanup.core.model.data.Worksite

@Composable
fun AssignedCasesView(
    assignedCases: List<Worksite>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text("Assigned ${assignedCases.size}")
    }
}

package com.crisiscleanup.feature.incidentcache.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.feature.incidentcache.IncidentWorksitesCacheViewModel

@Composable
fun IncidentWorksitesCacheRoute(
    onBack: () -> Unit,
) {
    IncidentWorksitesCacheScreen(
        onBack = onBack,
    )
}

@Composable
private fun IncidentWorksitesCacheScreen(
    onBack: () -> Unit,
    viewModel: IncidentWorksitesCacheViewModel = hiltViewModel(),
) {
    Text("Incident cache")
}

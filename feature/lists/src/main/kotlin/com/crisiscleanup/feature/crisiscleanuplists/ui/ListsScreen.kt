package com.crisiscleanup.feature.crisiscleanuplists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.CrisisCleanupButton
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.feature.crisiscleanuplists.ListsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsRoute(
    onBack: () -> Unit = {},
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val incidentLists by viewModel.incidentLists.collectAsStateWithLifecycle()
    val allLists = viewModel.allLists.collectAsLazyPagingItems()

    val t = LocalAppTranslator.current
    // TODO Tabs for current incident lists and all lists
    // TODO Pull to refresh
    Column {
        TopAppBarBackAction(
            title = t("~~Lists"),
            onAction = onBack,
        )

        Text(t("~~Lists are currently read-only. Manage lists using Crisis Cleanup on the browser"))

        Text("Incident lists ${incidentLists.size}. All ${allLists.itemCount} ")

        CrisisCleanupButton(
            text = t("Refresh lists"),
            onClick = { viewModel.refreshLists(true) },
        )
    }
}
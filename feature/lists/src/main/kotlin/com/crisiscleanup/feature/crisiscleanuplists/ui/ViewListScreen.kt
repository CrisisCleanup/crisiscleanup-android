package com.crisiscleanup.feature.crisiscleanuplists.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.common.relativeTime
import com.crisiscleanup.core.commonassets.getDisasterIcon
import com.crisiscleanup.core.commoncase.ui.IncidentHeaderView
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedByHalf
import com.crisiscleanup.core.model.data.CrisisCleanupList
import com.crisiscleanup.feature.crisiscleanuplists.ViewListViewModel
import com.crisiscleanup.feature.crisiscleanuplists.ViewListViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewListRoute(
    onBack: () -> Unit = {},
    viewModel: ViewListViewModel = hiltViewModel(),
) {
    val screenTitle by viewModel.screenTitle.collectAsStateWithLifecycle()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column {
            TopAppBarBackAction(
                title = screenTitle,
                onAction = onBack,
            )

            when (viewState) {
                is ViewListViewState.Success -> {
                    val list = (viewState as ViewListViewState.Success).list
                    ListDetailsView(list)
                }

                is ViewListViewState.Error -> {
                    Text((viewState as ViewListViewState.Error).message)
                }

                else -> {}
            }
        }

        BusyIndicatorFloatingTopCenter(viewState is ViewListViewState.Loading)
    }
}

@Composable
private fun ListDetailsView(
    list: CrisisCleanupList,
) {
    list.incident?.let { incident ->
        IncidentHeaderView(
            Modifier,
            incident.shortName,
            getDisasterIcon(incident.disaster),
            isSyncing = false,
        )
    }

    Row(
        listItemModifier,
        horizontalArrangement = listItemSpacedByHalf,
    ) {
        ListIcon(list)
        Text(list.updatedAt.relativeTime)
    }

    val description = list.description.trim()
    if (description.isNotBlank()) {
        Text(
            description,
            listItemModifier,
        )
    }

    LazyColumn {
        // TODO Load individual items
        //      Change incident
        //      Open to Case
    }
}
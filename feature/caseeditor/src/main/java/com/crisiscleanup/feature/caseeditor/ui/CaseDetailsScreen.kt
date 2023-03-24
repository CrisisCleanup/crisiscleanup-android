package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseDetailsViewModel
import com.crisiscleanup.feature.caseeditor.R

@Composable
internal fun DetailsSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
) {
    EditCaseSummaryHeader(
        // TODO Translator?
        R.string.details,
        isEditable,
        onEdit,
        modifier,
    ) {
        worksite.run {
            // TODO
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseDetailsRoute(
    viewModel: EditCaseDetailsViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
) {
    BackHandler {
        if (viewModel.onSystemBack()) {
            onBackClick()
        }
    }

    val onNavigateBack = remember(viewModel) {
        {
            if (viewModel.onNavigateBack()) {
                onBackClick()
            }
        }
    }
    val onNavigateCancel = remember(viewModel) {
        {
            if (viewModel.onNavigateCancel()) {
                onBackClick()
            }
        }
    }
    Column {
        // TODO This seems to be recomposing when map is moved.
        //      Is it possible to not recompose due to surrounding views?
        TopAppBarBackCancel(
            titleResId = R.string.details,
            onBack = onNavigateBack,
            onCancel = onNavigateCancel,
        )
        // TODO Remove the gap between the top bar above and location view below
        DetailsView()
    }
}

@Composable
private fun DetailsView() {
    Text("Details")
}

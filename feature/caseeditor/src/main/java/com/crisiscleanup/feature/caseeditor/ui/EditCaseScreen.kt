package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.feature.caseeditor.EditCaseBaseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseBackCancelView(
    viewModel: EditCaseBaseViewModel,
    onBackClick: () -> Unit,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
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
        // TODO Investigate and separate recomposition from content if necessary
        TopAppBarBackCancel(
            title = title,
            onBack = onNavigateBack,
            onCancel = onNavigateCancel,
        )
        // TODO Remove the gap between the top bar above and location view below
        content()
    }
}

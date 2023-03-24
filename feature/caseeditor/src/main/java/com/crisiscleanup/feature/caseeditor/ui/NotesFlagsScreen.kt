package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.core.model.data.Worksite
import com.crisiscleanup.feature.caseeditor.EditCaseNotesFlagsViewModel
import com.crisiscleanup.feature.caseeditor.R


@Composable
internal fun NotesFlagsSummaryView(
    worksite: Worksite,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit = {},
) {
    EditCaseSummaryHeader(
        R.string.notes_flags,
        isEditable,
        onEdit,
        modifier,
    ) {
        // TODO
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditCaseNotesFlagsRoute(
    viewModel: EditCaseNotesFlagsViewModel = hiltViewModel(),
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
        TopAppBarBackCancel(
            titleResId = R.string.notes_flags,
            onBack = onNavigateBack,
            onCancel = onNavigateCancel,
        )

//        val closeKeyboard = rememberCloseKeyboard(viewModel)
//        val scrollState = rememberScrollState()
//        Column(
//            Modifier
//                .scrollFlingListener(closeKeyboard)
//                .verticalScroll(scrollState)
//                .weight(1f)
//        ) {
//        }
        // TODO Lazy column
    }
}

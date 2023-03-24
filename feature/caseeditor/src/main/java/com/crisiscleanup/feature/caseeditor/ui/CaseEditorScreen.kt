package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.component.TopAppBarBackCancel
import com.crisiscleanup.feature.caseeditor.CaseEditorUiState
import com.crisiscleanup.feature.caseeditor.CaseEditorViewModel
import com.crisiscleanup.core.common.R as commonR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CaseEditorRoute(
    onEditPropertyData: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditNotesFlags: () -> Unit = {},
    onEditDetails: () -> Unit = {},
    onBackClick: () -> Unit = {},
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    BackHandler {
        if (!viewModel.saveChanges()) {
            // TODO Prompt if there are unsaved changes on back click
            onBackClick()
        }
    }

    val navigateBack by remember { viewModel.navigateBack }
    if (navigateBack) {
        onBackClick()
    } else {
        val headerTitle by viewModel.headerTitle.collectAsState()
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
                title = headerTitle,
                onBack = onNavigateBack,
                onCancel = onNavigateCancel,
            )
            CaseEditorScreen(
                onEditProperty = onEditPropertyData,
                onEditLocation = onEditLocation,
                onEditNotesFlags = onEditNotesFlags,
                onEditDetails = onEditDetails,
            )
        }
    }
}

@Composable
internal fun CaseEditorScreen(
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),
    onEditProperty: () -> Unit = {},
    onEditLocation: () -> Unit = {},
    onEditNotesFlags: () -> Unit = {},
    onEditDetails: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState) {
        is CaseEditorUiState.Loading -> {
            Box(modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        is CaseEditorUiState.WorksiteData -> {
            Box(Modifier.fillMaxSize()) {
                CaseSummary(
                    uiState as CaseEditorUiState.WorksiteData,
                    editPropertyData = onEditProperty,
                    editLocation = onEditLocation,
                    editNotesFlags = onEditNotesFlags,
                    editDetails = onEditDetails,
                )
            }
        }
        else -> {
            val errorData = uiState as CaseEditorUiState.Error
            val errorMessage = if (errorData.errorResId != 0) stringResource(errorData.errorResId)
            else errorData.errorMessage.ifEmpty { stringResource(commonR.string.unexpected_error) }
            Box(modifier) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.CaseSummary(
    worksiteData: CaseEditorUiState.WorksiteData,
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),

    editPropertyData: () -> Unit = {},
    editLocation: () -> Unit = {},
    editNotesFlags: () -> Unit = {},
    editDetails: () -> Unit = {},
) {
    val isLoadingWorksite by viewModel.isLoadingWorksite.collectAsStateWithLifecycle()
    val isEditable = worksiteData.isEditable

    Column(modifier.matchParentSize()) {
        Text(
            worksiteData.incident.name,
            // TODO Consistent spacing between all (forms) elements
            modifier = modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineMedium,
        )

        val worksite by viewModel.editingWorksite.collectAsStateWithLifecycle()
        if (viewModel.isCreateWorksite) {
            LocationSummaryView(
                worksite,
                isEditable,
                onEdit = editLocation,
            )
        }
        PropertySummaryView(
            worksite,
            isEditable,
            onEdit = editPropertyData,
        )

        if (!viewModel.isCreateWorksite) {
            LocationSummaryView(
                worksite,
                isEditable,
                onEdit = editLocation,
            )
        }

        NotesFlagsSummaryView(
            worksite,
            isEditable,
            onEdit = editNotesFlags,
        )

        if (worksite.isNew) {
            DetailsSummaryView(
                worksite,
                isEditable,
                onEdit = editDetails,
            )
        }
    }

    AnimatedVisibility(
        modifier = Modifier.align(Alignment.TopCenter),
        visible = isLoadingWorksite,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        CircularProgressIndicator(
            Modifier
                .wrapContentSize()
                .padding(48.dp)
                .size(24.dp)
        )
    }
}

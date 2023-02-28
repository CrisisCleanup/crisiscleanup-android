package com.crisiscleanup.feature.caseeditor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.feature.caseeditor.CaseEditorUiState
import com.crisiscleanup.feature.caseeditor.CaseEditorViewModel

@Composable
internal fun CaseEditorRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CaseEditorViewModel = hiltViewModel(),
) {
    BackHandler {
        // TODO Prompt if there are unsaved changes on back click
        onBackClick()
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    when (uiState) {
        is CaseEditorUiState.Loading -> {
            Box(modifier.fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        is CaseEditorUiState.WorksiteData -> {
            Box(Modifier.fillMaxSize()) {
                val isRefreshingWorksite by viewModel.isRefreshingWorksite.collectAsStateWithLifecycle()

                val worksiteData = uiState as CaseEditorUiState.WorksiteData
                Text("Case ${worksiteData.worksite.caseNumber}")

                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.TopCenter),
                    visible = isRefreshingWorksite,
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
        }
        else -> {
            // TODO Better UI
            Box(modifier) {
                Text(
                    text = "We have a problem",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
internal fun CaseEditorScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onForceReload: () -> Unit = {},
) {

}
package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.caseeditor.CaseHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseEditCaseHistoryRoute(
    onBack: () -> Unit = {},
    viewModel: CaseHistoryViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    CompositionLocalProvider(
        LocalAppTranslator provides translator,
    ) {
        val isLoadingCaseHistory by viewModel.isLoadingCaseHistory.collectAsStateWithLifecycle(false)

        val historyEvents by viewModel.historyEvents.collectAsStateWithLifecycle()
        val isDataLoaded by viewModel.isHistoryLoaded.collectAsStateWithLifecycle()

        Column(Modifier.fillMaxSize()) {
            TopAppBarBackAction(
                title = viewModel.screenTitle,
                onAction = onBack,
            )
            Text("History under construction")
        }
    }
}
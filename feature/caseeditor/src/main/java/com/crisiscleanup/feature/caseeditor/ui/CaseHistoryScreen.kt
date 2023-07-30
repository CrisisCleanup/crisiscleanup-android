package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
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
        Column(Modifier.fillMaxSize()) {
            TopAppBarBackAction(
                title = translator("actions.history"),
                onAction = onBack,
            )
            Text("History under construction")
        }
    }
}
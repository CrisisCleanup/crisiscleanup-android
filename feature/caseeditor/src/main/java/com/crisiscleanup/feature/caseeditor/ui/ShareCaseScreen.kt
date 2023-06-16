package com.crisiscleanup.feature.caseeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.AppTranslator
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TopAppBarCancelAction
import com.crisiscleanup.feature.caseeditor.CaseShareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseEditShareCaseRoute(
    onBack: () -> Unit = {},
    viewModel: CaseShareViewModel = hiltViewModel(),
) {
    val translator = viewModel.translator
    val appTranslator = remember(viewModel) {
        AppTranslator(translator = translator)
    }

    CompositionLocalProvider(
        LocalAppTranslator provides appTranslator,
    ) {
        Column {
            TopAppBarCancelAction(
                title = translator("actions.share"),
                onAction = onBack,
            )
        }
    }
}
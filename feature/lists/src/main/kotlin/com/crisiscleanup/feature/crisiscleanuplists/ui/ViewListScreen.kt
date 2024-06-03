package com.crisiscleanup.feature.crisiscleanuplists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.feature.crisiscleanuplists.ViewListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewListRoute(
    onBack: () -> Unit = {},
    viewModel: ViewListViewModel = hiltViewModel(),
) {
    val t = LocalAppTranslator.current


    Column {
        // TODO Use title (name of list) from view model
        TopAppBarBackAction(
            title = t("~~List"),
            onAction = onBack,
        )
        Text("Single list ${viewModel.listId}")
    }
}
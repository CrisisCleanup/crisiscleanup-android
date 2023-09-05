package com.crisiscleanup.feature.authentication.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.feature.authentication.AuthenticationViewModel


@Composable
fun SurvivorInfoRoute(
    enableBackHandler: Boolean,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    closeAuthentication: () -> Unit = {},
) {
    SurvivorInfoScreen(
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurvivorInfoScreen(
    onBack: () -> Unit = {},
) {
    val translator = LocalAppTranslator.current

    Column {
        TopAppBarBackAction(
            title = translator("~~I need help"),
            onAction = onBack,
        )
    }
}
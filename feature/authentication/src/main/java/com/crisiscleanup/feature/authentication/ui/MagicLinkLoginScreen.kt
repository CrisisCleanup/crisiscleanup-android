package com.crisiscleanup.feature.authentication.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.AnimatedBusyIndicator
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.fillWidthPadded
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.authentication.MagicLinkLoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagicLinkLoginRoute(
    onBack: () -> Unit,
    closeAuthentication: () -> Unit = {},
    viewModel: MagicLinkLoginViewModel = hiltViewModel(),
) {
    val isAuthenticateSuccessful by viewModel.isAuthenticateSuccessful.collectAsStateWithLifecycle()

    val clearStateOnBack = remember(onBack, viewModel, isAuthenticateSuccessful) {
        {
            viewModel.clearMagicLinkLogin()

            if (isAuthenticateSuccessful) {
                closeAuthentication()
            } else {
                onBack()
            }
        }
    }

    if (isAuthenticateSuccessful) {
        clearStateOnBack()
    }

    BackHandler {
        clearStateOnBack()
    }

    val translator = LocalAppTranslator.current

    Column {
        TopAppBarBackAction(
            title = translator("actions.login"),
            onAction = clearStateOnBack,
        )

        val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
        val errorMessage = viewModel.errorMessage
        if (isAuthenticating) {
            AnimatedBusyIndicator(
                true,
                modifier = listItemModifier,
            )
        } else if (errorMessage.isNotBlank()) {
            Text(
                modifier = fillWidthPadded,
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

package com.crisiscleanup.feature.organizationmanage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.core.designsystem.LocalAppTranslator
import com.crisiscleanup.core.designsystem.component.BusyIndicatorFloatingTopCenter
import com.crisiscleanup.core.designsystem.component.TopAppBarBackAction
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.feature.organizationmanage.RequestRedeployViewModel
import com.crisiscleanup.feature.organizationmanage.RequestRedeployViewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestRedeployRoute(
    onBack: () -> Unit = {},
    viewModel: RequestRedeployViewModel = hiltViewModel(),
) {
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()
    val isLoading = viewState == RequestRedeployViewState.Loading
    val isRedeployRequested = viewModel.isRedeployRequested

    val t = LocalAppTranslator.current
    Column {
        TopAppBarBackAction(
            title = t("requestRedeploy.request_redeploy"),
            onAction = onBack,
        )

        if (isLoading) {
            Box {
                BusyIndicatorFloatingTopCenter(true)
            }
        } else if (isRedeployRequested) {
            Text(
                t("requestRedeploy.request_redeploy_success"),
                listItemModifier,
            )
        } else {
            (viewState as? RequestRedeployViewState.Ready)?.let { readyState ->
                val incidentOptions = readyState.incidents
                val isTransient by viewModel.isTransient.collectAsStateWithLifecycle(true)
                val isEditable = !isTransient
                val errorMessage = viewModel.redeployErrorMessage

                Text(
                    t("requestRedeploy.choose_an_incident"),
                    listItemModifier,
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        errorMessage,
                        listItemModifier,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

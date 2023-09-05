package com.crisiscleanup.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crisiscleanup.MainActivityViewModel
import com.crisiscleanup.core.common.R
import com.crisiscleanup.core.designsystem.component.SmallBusyIndicator
import com.crisiscleanup.core.designsystem.theme.listItemModifier
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy

@Composable
internal fun SwitchToProductionView(
    viewModel: MainActivityViewModel = hiltViewModel(),
) {
    Column(
        listItemModifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.fillMaxWidth(0.67f)) {
            Image(
                painter = painterResource(id = R.drawable.crisis_cleanup_logo),
                contentDescription = null,
            )
        }

        Spacer(Modifier.height(24.dp))

        val switchingMessage by viewModel.productionSwitchMessage.collectAsStateWithLifecycle()
        val isSwitching by viewModel.isSwitchingToProduction.collectAsStateWithLifecycle()
        Column(
            listItemModifier,
            listItemSpacedBy,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Please wait while we upgrade...")
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSwitching,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SmallBusyIndicator(
                        padding = 8.dp,
                    )
                }
            }
            Text(switchingMessage)
        }
    }
}

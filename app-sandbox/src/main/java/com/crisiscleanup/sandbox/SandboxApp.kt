package com.crisiscleanup.sandbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.crisiscleanup.core.designsystem.component.CrisisCleanupBackground
import com.crisiscleanup.core.designsystem.component.CrisisCleanupTextButton
import com.crisiscleanup.core.designsystem.theme.listItemSpacedBy
import com.crisiscleanup.sandbox.navigation.MULTI_IMAGE_ROUTE
import com.crisiscleanup.sandbox.navigation.SandboxNavHost
import com.crisiscleanup.sandbox.navigation.navigateToBottomNav
import com.crisiscleanup.sandbox.navigation.navigateToCheckboxes
import com.crisiscleanup.sandbox.navigation.navigateToChips
import com.crisiscleanup.sandbox.navigation.navigateToMultiImage
import com.crisiscleanup.sandbox.navigation.navigateToSingleImage

@Composable
fun SandboxApp(
    windowSizeClass: WindowSizeClass,
    appState: SandboxAppState = rememberAppState(
        windowSizeClass = windowSizeClass,
    ),
) {
    CrisisCleanupBackground {
        Box(Modifier.fillMaxSize()) {
            val snackbarHostState = remember { SnackbarHostState() }

            Scaffold(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .consumeWindowInsets(padding)
                        .windowInsetsPadding(
                            if (appState.isFullscreenRoute) {
                                WindowInsets(0, 0, 0, 0)
                            } else {
                                WindowInsets.safeDrawing
                            },
                        ),
                ) {
                    SandboxNavHost(
                        appState.navController,
                        appState::onBack,
                        MULTI_IMAGE_ROUTE,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RootRoute(navController: NavController) {
    Column {
        Spacer(Modifier.weight(1f))
        FlowRow(
            horizontalArrangement = listItemSpacedBy,
            verticalArrangement = listItemSpacedBy,
            maxItemsInEachRow = 6,
        ) {
            CrisisCleanupTextButton(text = "Bottom nav") {
                navController.navigateToBottomNav()
            }
            CrisisCleanupTextButton(text = "Checkboxes") {
                navController.navigateToCheckboxes()
            }
            CrisisCleanupTextButton(text = "Chips") {
                navController.navigateToChips()
            }
            CrisisCleanupTextButton(text = "Image") {
                navController.navigateToSingleImage()
            }
            CrisisCleanupTextButton(text = "Images") {
                navController.navigateToMultiImage()
            }
        }
    }
}
